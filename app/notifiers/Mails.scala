/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Association du Paris Java User Group.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package notifiers


import javax.swing.text.html.HTML
import java.io.File

import models._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.libs.mailer.{Email, MailerPlugin}
import controllers.{CallForPaper, LeaderBoardParams}
import controllers.LeaderBoardParams
import org.joda.time.{DateTime, DateTimeZone, Period}
import org.joda.time.format.DateTimeFormat
import play.twirl.api.Html

import scala.util.matching.Regex
import java.io.BufferedWriter
import java.io.FileWriter
import javax.security.auth.Subject

import play.api.libs.mailer.{Email, MailerPlugin}
import controllers.LeaderBoardParams
import notifiers.Mails.{bcc, extractOtherEmails, from, _}

/**
  * Sends all emails
  *
  * Author: nmartignole
  * Created: 04/10/2013 15:56
  */

object Mails {

  lazy val from = ConferenceDescriptor.current().fromEmail
  lazy val committeeEmail = ConferenceDescriptor.current().committeeEmail
  lazy val bugReportRecipient = ConferenceDescriptor.current().bugReportRecipient
  lazy val bcc = ConferenceDescriptor.current().bccEmail

  def replaceAllDynamicParametres(content:String,speaker: Webuser,proposal: Proposal): String ={
    var cont= content.replace("Speaker.email",speaker.email )
    cont=cont.replace("Speaker.firstName",speaker.firstName )
    cont=cont.replace("proposal.title",proposal.title )
    proposal.deadline match {
      case Some(a)=>cont= cont.replace("proposal.deadline",a.toString("dd/MM/yyyy") )
      case None =>}
    cont
  }

  /**
    * Send a message to a set of Speakers.
    * This function used to send 2 emails in the previous version.
    *
    * @return the rfc 822 Message-ID
    */
  def sendMessageToSpeakers(fromWebuser: Webuser, toWebuser: Webuser, proposal: Proposal, msg: String, inReplyTo: Option[String]): String = {
    val listOfEmails = extractOtherEmails(proposal)

    val inReplyHeaders: Seq[(String, String)] = inReplyTo.map {
      replyId: String =>
        Seq("In-Reply-To" -> replyId)
    }.getOrElse(Seq.empty[(String, String)])

    val email = Email(
      subject = s"[${proposal.id}] ${proposal.title}",
      from = from,
      to = Seq(toWebuser.email),
      cc = listOfEmails, // Send the email to the speaker and co-speakers
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendMessageToSpeaker(fromWebuser.cleanName, proposal, msg).toString()),
      bodyHtml = Some(views.html.Mails.sendMessageToSpeaker(fromWebuser.cleanName, proposal, msg).toString()),
      charset = Some("utf-8"),
      headers = inReplyHeaders
    )
    val newMessageId = MailerPlugin.send(email) // returns the message-ID

    // For Program committee
    // The URL to the talk is different, thus we need another template.
    val emailForCommittee = Email(
      subject = s"[${proposal.id}] Message to a speaker - ${proposal.title}",
      from = from,
      to = Seq(committeeEmail),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendMessageToSpeakerCommittee(fromWebuser.cleanName, toWebuser.cleanName, proposal, msg).toString()),
      bodyHtml = Some(views.html.Mails.sendMessageToSpeakerCommittee(fromWebuser.cleanName, toWebuser.cleanName, proposal, msg).toString()),
      charset = Some("utf-8"),
      headers = inReplyHeaders
    )
    MailerPlugin.send(emailForCommittee)

    newMessageId
  }

  def sendMessageToCommittee(fromWebuser: Webuser, proposal: Proposal, msg: String, inReplyTo: Option[String]) = {
    val listOfOtherSpeakersEmail = extractOtherEmails(proposal)

    val inReplyHeaders: Seq[(String, String)] = inReplyTo.map {
      replyId: String =>
        Seq("In-Reply-To" -> replyId)
    }.getOrElse(Seq.empty[(String, String)])

    val email = Email(
      subject = s"[${proposal.id}] ${proposal.title}", // please keep a generic subject => perfect for Mail Thread
      from = from,
      to = Seq(committeeEmail),
      cc = listOfOtherSpeakersEmail,
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendMessageToCommittee(fromWebuser.cleanName, proposal, msg).toString()),
      bodyHtml = Some(views.html.Mails.sendMessageToCommittee(fromWebuser.cleanName, proposal, msg).toString()),
      charset = Some("utf-8"),
      headers = inReplyHeaders
    )
    MailerPlugin.send(email) // returns the message-ID
  }

  def sendNotifyProposalSubmitted(fromWebuser: Webuser, proposal: Proposal) = {
    val listOfOtherSpeakersEmail = extractOtherEmails(proposal)
    val subjectEmail: String = Messages("mail.notify_proposal.subject", fromWebuser.cleanName, proposal.title)

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(committeeEmail),
      cc = listOfOtherSpeakersEmail,
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendNotifyProposalSubmitted(fromWebuser.cleanName, proposal.id, proposal.title, Messages(proposal.track.label), Messages(proposal.talkType.id)).toString()),
      bodyHtml = Some(views.html.Mails.sendNotifyProposalSubmitted(fromWebuser.cleanName, proposal.id, proposal.title, Messages(proposal.track.label), Messages(proposal.talkType.id)).toString()),
      charset = Some("utf-8"),
      headers = Seq()
    )
    MailerPlugin.send(email)
  }

  /**
    * Post a new message to SMTP with an optional In-Reply-To, so that Mail clients can order by / group by all messages together.
    * Message-ID cannot be set here. MimeMessages updateMessageID() method would need to be overloaded but it's too complex.
    *
    * @return the RFC 822 Message-ID generated by MimeMessages
    */
  def postInternalMessage(fromWebuser: Webuser, proposal: Proposal, msg: String, inReplyTo: Option[String]): String = {
    val subjectEmail: String = s"[PRIVATE][${proposal.id}] ${proposal.title}"

    val inReplyHeaders: Seq[(String, String)] = inReplyTo.map {
      replyId: String =>
        Seq("In-Reply-To" -> replyId)
    }.getOrElse(Seq.empty[(String, String)])

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(committeeEmail),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.postInternalMessage(fromWebuser.cleanName, proposal, msg).toString()),
      bodyHtml = Some(views.html.Mails.postInternalMessage(fromWebuser.cleanName, proposal, msg).toString()),
      charset = Some("utf-8"),
      headers = inReplyHeaders
    )

    // Mailjet does not keep the Message-ID, you must use Mailgun if you want this code to work
    val messageId = MailerPlugin.send(email)
    messageId
  }

  def sendReminderForDraft(speaker: Webuser, proposals: List[Proposal]) = {
    val subjectEmail = proposals.size match {
      case x if x > 1 => Messages("mail.draft_multiple_reminder.subject", proposals.size, Messages("longYearlyName"))
      case other => Messages("mail.draft_single_reminder.subject", Messages("longYearlyName"))
    }
    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(speaker.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendReminderForDraft(speaker.firstName, proposals).toString()),
      bodyHtml = Some(views.html.Mails.sendReminderForDraft(speaker.firstName, proposals).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }

  def sendProposalApproved(speaker: Webuser, proposal: Proposal) = {
    MailsManager.getEmailMode() match {
      case Some(email)=>
        if (email=="disable"){
          //pour calculer la date limite pour accepter de présenter le sujet
          var subjectEmail: String = Messages("mail.proposal_approved.subject", proposal.title)
          var bodyh=views.html.Mails.acceptrefuse.sendProposalApproved(proposal,None).toString()
          proposal.deadline match {
            case None =>
            case Some(days)=>subjectEmail= Messages("mail.proposal_approved.subject2", proposal.title,DateTimeFormat.forPattern("dd/MM/yyyy").print(days))
              bodyh= views.html.Mails.acceptrefuse.sendProposalApproved(proposal,Some(DateTimeFormat.forPattern("dd/MM/yyyy").print(days))).toString()
          }

          val otherSpeakers = extractOtherEmails(proposal)
          val email = Email(
            subject = subjectEmail,
            from = from,
            to = Seq(speaker.email),
            cc = otherSpeakers,
            bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
            bodyText = Some(views.txt.Mails.acceptrefuse.sendProposalApproved(proposal).toString()),
            bodyHtml = Some(bodyh),
            charset = Some("utf-8")
          )

          MailerPlugin.send(email)
        }else{
          MailsManager.getMailByTypeAndLang("approved",proposal.lang) match {
            case Some(mail)=>
              val otherSpeakers = extractOtherEmails(proposal)
              val email = Email(
                subject = mail.Subject,
                from = from,
                to = Seq(speaker.email),
                cc = otherSpeakers,
                bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
                bodyText = Some(replaceAllDynamicParametres(mail.content,speaker,proposal).toString()),
                bodyHtml = Some(replaceAllDynamicParametres(mail.content,speaker,proposal)),
                charset = Some("utf-8")
              )
              MailerPlugin.send(email)
            case None=>
          }
        }
      case None=>
    }
  }

  def sendProposalApprovedAfeterRefese(speaker: Webuser, proposal: Proposal,content:String,subject:Option[String]) = {
    //pour calculer la date limite pour accepter de présenter le sujet

    var subjectEmail: String = subject.map(x=>x).getOrElse(Messages("mail.proposal_approved.subject", proposal.title))
    val otherSpeakers = extractOtherEmails(proposal)
    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(speaker.email),
      cc = otherSpeakers,
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(content),
      bodyHtml = Some(content),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }
  def sendProposalcustomRefese(speaker: Webuser, proposal: Proposal,content:String,subject:Option[String]) = {
    //pour calculer la date limite pour accepter de présenter le sujet

    var subjectEmail: String = subject.map(x=>x).getOrElse(Messages("mail.proposal_refused.subject", proposal.title))
    val otherSpeakers = extractOtherEmails(proposal)
    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(speaker.email),
      cc = otherSpeakers,
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(content),
      bodyHtml = Some(content),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }
  def sendProposalRefused(speaker: Webuser, proposal: Proposal) = {
    val subjectEmail: String = Messages("mail.proposal_refused.subject", proposal.title)
    val otherSpeakers = extractOtherEmails(proposal)

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(speaker.email),
      cc = otherSpeakers,
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.acceptrefuse.sendProposalRefused(proposal).toString()),
      bodyHtml = Some(views.html.Mails.acceptrefuse.sendProposalRefused(proposal).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }

  def sendAcceptedtoAcceptteTermeAndCondition(speaker:Speaker)={
    val subjectEmail:String=Messages("acceptTermsConditions")
    val email =Email(
      subject=subjectEmail ,
      from=from,
      to =Seq(speaker.email) ,
      bcc= bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(""),
      bodyHtml = Some(views.html.Mails.sendtoAcceptetermAndCondition(speaker).toString()),
      charset = Some("utf-8")
    )
    MailerPlugin.send(email)
  }

  def sendResultToSpeaker(speaker: Speaker, listOfApprovedProposals: Set[Proposal], listOfRefusedProposals: Set[Proposal]) = {
    val subjectEmail: String = Messages("mail.speaker_cfp_results.subject", Messages("longYearlyName"))

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(speaker.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.acceptrefuse.sendResultToSpeaker(speaker, listOfApprovedProposals, listOfRefusedProposals).toString()),
      bodyHtml = Some(views.html.Mails.acceptrefuse.sendResultToSpeaker(speaker, listOfApprovedProposals, listOfRefusedProposals).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }

  def sendInvitationForSpeaker(speakerEmail: String, message: String, requestId: String) = {
    val subjectEmail: String = Messages("shortYearlyName") + " special request"

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(speakerEmail),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendInvitationForSpeaker(message, requestId).toString()),
      bodyHtml = Some(views.html.Mails.sendInvitationForSpeaker(message, requestId).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }

  def sendGoldenTicketEmail(webuser: Webuser, gt: GoldenTicket) = {
    val subjectEmail: String = Messages("mail.goldenticket.subject", Messages("shortYearlyName"))

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(webuser.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.goldenticket.sendGoldenTicketEmail(webuser, gt).toString()),
      bodyHtml = Some(views.html.Mails.goldenticket.sendGoldenTicketEmail(webuser, gt).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }

  def sendCreateTalkCfpClose(webuser: Webuser) = {
    val subjectEmail: String = Messages("Invitation to create talk")
    val linkToCFPHome: String = ConferenceDescriptor.getFullRoutePath(controllers.routes.Application.home.url)
    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(webuser.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendCreateTalkCfpClose(webuser, linkToCFPHome).toString()),
      bodyHtml = Some(views.html.Mails.sendCreateTalkCfpClose(webuser, linkToCFPHome).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }


  def sendEmailTalk(webuser: Webuser, gt: GoldenTicket) = {
    val subjectEmail: String = Messages("mail.goldenticket.subject", Messages("shortYearlyName"))

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq(webuser.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.goldenticket.sendGoldenTicketEmail(webuser, gt).toString()),
      bodyHtml = Some(views.html.Mails.goldenticket.sendGoldenTicketEmail(webuser, gt).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }
  def sendScheduledFavorite( slots:List[Slot] , webuser:Webuser ) = {
    val subjectEmail: String = Messages("Scheduled Favorite")
    val email =Email(
      subject = subjectEmail,
      from = from,
      to = Seq(webuser.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendScheduledFavorite(slots , webuser).toString()),
      bodyHtml = Some(views.html.Mails.sendScheduledFavorite(slots , webuser).toString()),
      charset = Some("utf-8")


    )
    MailerPlugin.send(email)
  }
  def sendScheduledSpeaksProps( slots:List[Slot] , webuser:Webuser ) = {
    val subjectEmail: String = Messages("Scheduled Talks")
    val email =Email(
      subject = subjectEmail,
      from = from,
      to = Seq(webuser.email),
      bcc = bcc.map(s => List(s)).getOrElse(Seq.empty[String]),
      bodyText = Some(views.txt.Mails.sendScheduledTalksInfo(slots , webuser).toString()),
      bodyHtml = Some(views.html.Mails.sendScheduledTalksInfo(slots , webuser).toString()),
      charset = Some("utf-8")


    )
    MailerPlugin.send(email)
  }

  /**
    * Mail digest.
    *
    * @param userIDs the list of CFP uuids for given digest
    * @param digest  List of speakers and their new proposals
    * @return
    */
  def sendDigest(digest: Digest,
                 userIDs: List[String],
                 proposals: List[Proposal],
                 isDigestFilterOn: Boolean,
                 leaderBoardParams: LeaderBoardParams): String = {

    val subjectEmail: String = Messages("mail.digest.subject", digest.value, Messages("longYearlyName"))

    val emails = userIDs.map(uuid => Webuser.findByUUID(uuid).get.email)

    val email = Email(
      subject = subjectEmail,
      from = from,
      to = Seq("no-reply-digest@devoxx.com"), // Use fake email because we use bcc instead
      bcc = emails,
      bodyText = Some(views.txt.Mails.digest.sendDigest(digest, proposals, isDigestFilterOn, leaderBoardParams).toString()),
      bodyHtml = Some(views.html.Mails.digest.sendDigest(digest, proposals, isDigestFilterOn, leaderBoardParams).toString()),
      charset = Some("utf-8")
    )

    MailerPlugin.send(email)
  }

  private def extractOtherEmails(proposal: Proposal): List[String] = {
    val maybeSecondSpeaker = proposal.secondarySpeaker.flatMap(uuid => Webuser.getEmailFromUUID(uuid))
    val maybeOtherEmails = proposal.otherSpeakers.flatMap(uuid => Webuser.getEmailFromUUID(uuid))
    maybeOtherEmails ++ maybeSecondSpeaker.toList
  }

  private def extractAllEmails(proposal: Proposal): Iterable[String] = {
    val mainSpeakerEmail = Webuser.getEmailFromUUID(proposal.mainSpeaker)
    mainSpeakerEmail ++ extractOtherEmails(proposal)
  }

}
