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

package controllers

import controllers.Authentication.Redirect
import notifiers.TransactionalEmails
import library.search.ElasticSearch
import library.{SendMessageToCommittee, ZapActor}
import library.sms.{SendWelcomeAndHelp, SmsActor, TwilioSender}
import library.{NotifyProposalSubmitted, SendMessageToCommittee, ZapActor}
import models._
import org.apache.commons.lang3.StringUtils
import play.api.cache.Cache
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import play.api.libs.Crypto
import play.api.libs.json.{JsArray, JsResult, JsValue, Json}
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.Cookie
import play.api.Play.current

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.parsing.json.{JSONArray, JSONObject}
import scala.concurrent.Future
import views.html

/**
  * Main controller for the speakers.
  *
  * Author: nicolas martignole
  * Created: 29/09/2013 12:24
  */
object CallForPaper extends SecureCFPController {

  def homeForSpeaker = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid

      Speaker.findByUUID(uuid).map {
        speaker: Speaker =>
          if (!Webuser.isSpeaker(uuid)) {
            Webuser.addToSpeaker(uuid)
          }
          val hasApproved = Proposal.countByProposalState(uuid, ProposalState.APPROVED) > 0
          val hasAccepted = Proposal.countByProposalState(uuid, ProposalState.ACCEPTED) > 0
          val needsToAcceptTermAndCondition = Speaker.needsToAccept(uuid) && (hasAccepted || hasApproved)

          (hasApproved, hasAccepted) match {
            case (true, _) => Redirect(routes.ApproveOrRefuse.doAcceptOrRefuseTalk()).flashing("success" -> Messages("please.check.approved"))
            case other =>
              val allProposals = Proposal.allMyProposals(uuid)
              val totalArchived = Proposal.countByProposalState(uuid, ProposalState.ARCHIVED)
              val ratings = if (hasAccepted || hasApproved) {
                Rating.allRatingsForTalks(allProposals)
              } else {
                Map.empty[Proposal, List[Rating]]
              }
              Ok(views.html.CallForPaper.homeForSpeaker(speaker, request.webuser, allProposals, totalArchived, ratings, needsToAcceptTermAndCondition))
          }
      }.getOrElse {
        val flashMessage = if (Webuser.hasAccessToGoldenTicket(request.webuser.uuid)) {
          Messages("callforpaper.gt.create.profile")
        } else {
          Messages("callforpaper.import.profile")
        }
        //We have a Webuser but no associated Speaker profile
        Redirect(routes.CallForPaper.newSpeakerForExistingWebuser()).flashing("success" -> flashMessage)
      }
  }

  def newSpeakerForExistingWebuserByUuid(webUuid: String) = SecuredAction {
    implicit request =>
      val webuser = Webuser.findByUUID(webUuid).get
      val defaultValues = (webuser.email, webuser.firstName, webuser.lastName, StringUtils.abbreviate("...", 750),
        None, None, None, None, None, "No experience", None, QuestionAndAnswers.empty)
      Ok(views.html.Authentication.confirmImport(Authentication.importSpeakerForm.fill(defaultValues)))
  }

  // Specific secured action. We need a redirect from homeForSpeaker, to be able to display flash message
  case class Notification(typ:String ,subject:String , receiver:String , receiverType:String , date:String , checked:Boolean){}




  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  def getnotifs(id:String) = SecuredAction.async{
    implicit request =>
     WS.url(s"http://localhost:${NotificationService.port}/notifications/${id.toString}").get().map( res => Ok(views.html.CFPAdmin.notifications(res.json.as[JsArray].value.toList , id)))
       .recover{ case e:java.net.ConnectException => Ok( "Failed to connect" )}
      /*val futureResponse: Future[JsResult[Notification]]= WS.url(s"http://localhost:7080/notifs/receiver/${id.toString}").get()
        .map {
          response => (response.json \ "").validate[Notification]
        }*/


     /* val result = Await.result(futureResponse, Duration.Inf)


      var j = result.json.as[JsArray]*/

      //Ok(result.toString)

    //Ok(views.html.CFPAdmin.notifications(j.value.toList))
  }
  def newSpeakerForExistingWebuser = SecuredAction {
    implicit request =>
      val w = request.webuser
      val defaultValues = (w.email, w.firstName, w.lastName, StringUtils.abbreviate("...", 750),
        None, None, None, None, None, "No experience", None, QuestionAndAnswers.empty)

      Ok(views.html.Authentication.confirmImport(Authentication.importSpeakerForm.fill(defaultValues)))
  }

  val speakerForm = play.api.data.Form(mapping(
    "uuid" -> ignored("xxx"),
    "email" -> (email verifying nonEmpty),
    "lastName" -> nonEmptyText(maxLength = 25),
    "bio" -> nonEmptyText(maxLength = 750),
    "lang" -> optional(text),
    "twitter" -> optional(text),
    "avatarUrl" -> optional(text),
    "picture" -> optional(text),
    "company" -> optional(text),
    "blog" -> optional(text),
    "firstName" -> nonEmptyText(maxLength = 25),
    "qualifications" -> nonEmptyText(maxLength = 750),
    "phoneNumber" -> optional(text),
    "questionAndAnswers" -> optional(seq(
      mapping(
        "question" -> optional(text),
        "answer" -> optional(text)
      )(QuestionAndAnswer.apply)(QuestionAndAnswer.unapply))
    )
  )(Speaker.createSpeaker)(Speaker.unapplyForm))

  def editProfile = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      Speaker.findByUUID(uuid).map {
        speaker =>
          Ok(html.CallForPaper.editProfile(speakerForm.fill(speaker), uuid))
      }.getOrElse(Unauthorized("User not found"))
  }

  def saveProfile = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      speakerForm.bindFromRequest.fold(
        invalidForm => BadRequest(html.CallForPaper.editProfile(invalidForm, uuid)).flashing("error" -> "Invalid form, please check and correct errors. "),
        updatedSpeaker => {
          Speaker.update(uuid, updatedSpeaker)
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> "Profile saved")
        }
      )
  }

  // Load a new proposal form
  private def createCookie(webuser: Webuser) = {
    Cookie("cfp_rm"
      , value = Crypto.encryptAES(webuser.uuid)
      , maxAge = Some(588000)
      , secure = ConferenceDescriptor.isHTTPSEnabled
      , httpOnly = true)
  }


  def vipProposal(uuid: String) = SecuredAction(IsMemberOf("cfp")) {

    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Application.index())
    // Redirect(routes.CallForPaper.newProposal()).flashing("warning" -> Messages("cfp.reminder.proposals")).withSession("uuid" -> uuid)
  }

  def newProposal() = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid

      Ok(views.html.CallForPaper.newProposal(Proposal.proposalForm, request.webuser)).withSession(request.session + ("token" -> Crypto.sign(uuid)))
  }

  // Load a proposal
  def editProposal(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findProposal(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          if (proposal.mainSpeaker == uuid) {
            val proposalForm = Proposal.proposalForm.fill(proposal)
            Ok(views.html.CallForPaper.newProposal(proposalForm, request.webuser)).withSession(request.session + ("token" -> Crypto.sign(proposalId)))
          } else if (proposal.secondarySpeaker.isDefined && proposal.secondarySpeaker.get == uuid) {
            // Switch the mainSpeaker and the other Speakers
            val proposalForm = Proposal.proposalForm.fill(Proposal.setMainSpeaker(proposal, uuid))
            Ok(views.html.CallForPaper.newProposal(proposalForm, request.webuser)).withSession(request.session + ("token" -> Crypto.sign(proposalId)))
          } else if (proposal.otherSpeakers.contains(uuid)) {
            // Switch the secondary speaker and this speaker
            val proposalForm = Proposal.proposalForm.fill(Proposal.setMainSpeaker(proposal, uuid))
            Ok(views.html.CallForPaper.newProposal(proposalForm, request.webuser)).withSession(request.session + ("token" -> Crypto.sign(proposalId)))
          } else {
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> "Invalid state")
          }
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  // Prerender the proposal, but do not persist
  def previewProposal() = SecuredAction {
    implicit request =>
      Proposal.proposalForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.CallForPaper.newProposal(hasErrors, request.webuser)).flashing("error" -> "invalid.form"),
        validProposal => {
          val summary = validProposal.summaryAsHtml
          // markdown to HTML
          val privateMessage = validProposal.privateMessageAsHtml // markdown to HTML
          play.Logger.info(s"Proposal being previewed after being changed by '${request.webuser.cleanName}' (uuid: ${request.webuser.uuid}, proposal id: '${validProposal.id}')")
          Ok(html.CallForPaper.previewProposal(summary, privateMessage, Proposal.proposalForm.fill(validProposal), request.webuser.uuid))
        }
      )
  }

  // Revalidate to avoid CrossSite forgery and save the proposal
  def saveProposal() = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      /*val wb = Webuser.findByUUID(uuid)
      wb match {
       case Some(w) => Webuser.activeVip( w , false)

      }*/

      Proposal.proposalForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.CallForPaper.newProposal(hasErrors, request.webuser)),
        proposal => {
          CFPAdmin.postNotification(s"Proposal created by ${Webuser.getName(uuid)}" , "proposal" , "speaker" , "" ,"admins")
         /* val data = Json.obj(
            "type" -> ,
            "subject" -> "speaker",
            "receiver" -> "b8114a07156aec70e3cb1de7f1c62d8c2cfd6b2f" ,
            "receiver-type" -> "admins"
          )
          val futureResponse: Future[WSResponse] = WS.url("http://localhost:7070/notif").post(data.toString())
          */// If the editor is not the owner then findProposal returns None
          Proposal.findProposal(uuid, proposal.id) match {
            case Some(existingProposal) =>
              // This is an edit operation
              // First we try to reset the speaker's, we do not take the values from the FORM for security reason
              val updatedProposal = proposal.copy(mainSpeaker = existingProposal.mainSpeaker, secondarySpeaker = existingProposal.secondarySpeaker, otherSpeakers = existingProposal.otherSpeakers)

              // Then because the editor becomes mainSpeaker, we have to update the secondary and otherSpeaker
              if (existingProposal.state == ProposalState.DRAFT || existingProposal.state == ProposalState.SUBMITTED) {
                Proposal.save(uuid, Proposal.setMainSpeaker(updatedProposal, uuid), ProposalState.DRAFT)
                if (ConferenceDescriptor.isResetVotesForSubmitted) {
                  Review.archiveAllVotesOnProposal(proposal.id)
                  Event.storeEvent(Event(proposal.id, uuid, s"Reset all votes on ${proposal.id}"))
                }
                Event.storeEvent(Event(proposal.id, uuid, "Updated proposal " + proposal.id + " with title " + StringUtils.abbreviate(proposal.title, 80)))
                play.Logger.info(s"Existing proposal has been changed and saved by '${request.webuser.cleanName}' (uuid: ${request.webuser.uuid}, proposal id: '${existingProposal.id}')")
                Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("saved1"))
              } else {
                Proposal.save(uuid, Proposal.setMainSpeaker(updatedProposal, uuid), existingProposal.state)
                Event.storeEvent(Event(proposal.id, uuid, "Edited proposal " + proposal.id + " with current state [" + existingProposal.state.code + "]"))
                play.Logger.info(s"Existing proposal has been changed and saved by '${request.webuser.cleanName}' (uuid: ${request.webuser.uuid}, proposal id: '${existingProposal.id}')")
                Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("saved2"))
              }
            case other =>
              // Check that this is really a new id and that it does not exist
              if (Proposal.isNew(proposal.id)) {
                // This is a "create new" operation
                Proposal.save(uuid, proposal, ProposalState.DRAFT)
                Event.storeEvent(Event(proposal.id, uuid, "Created a new proposal " + proposal.id + " with title " + StringUtils.abbreviate(proposal.title, 80)))
                play.Logger.info(s"New proposal created and saved by '${request.webuser.cleanName}' (uuid: ${request.webuser.uuid}, proposal id: '${proposal.id}')")
                Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("saved"))
              } else {
                // Maybe someone tried to edit someone's else proposal...
                Event.storeEvent(Event(proposal.id, uuid, "Tried to edit this talk but he is not the owner."))
                play.Logger.info(s"Proposal cannot be saved, attempt made to edit and save by '${request.webuser.cleanName}' (uuid: ${request.webuser.uuid}, proposal id: '${proposal.id}')")
                Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> "You are trying to edit a proposal that is not yours. This event has been logged.")
              }
          }
        }
      )
  }

  def autoCompleteTag(term: String) = SecuredAction {
    implicit request => {

      val tagsFound = Tag.allTags()
        .filter(tag => tag.value.toLowerCase.contains(term.toLowerCase))
        .sortBy(f => f.value)
        .map(tag => tag.value)
        .take(10)

      Ok(Json.toJson(tagsFound))
    }
  }

  // Load a proposal by its id
  def editOtherSpeakers(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findProposal(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          if (proposal.mainSpeaker == uuid) {
            val proposalForm = Proposal.proposalSpeakerForm.fill(proposal.secondarySpeaker, proposal.otherSpeakers)
            Ok(html.CallForPaper.editOtherSpeaker(Webuser.getName(uuid), proposal, proposalForm))
          } else if (proposal.secondarySpeaker.isDefined && proposal.secondarySpeaker.get == uuid) {
            // Switch the mainSpeaker and the other Speakers
            val proposalForm = Proposal.proposalSpeakerForm.fill(Option(proposal.mainSpeaker), proposal.otherSpeakers)
            Ok(html.CallForPaper.editOtherSpeaker(Webuser.getName(uuid), proposal, proposalForm))
          } else if (proposal.otherSpeakers.contains(uuid)) {
            // let this speaker as a member of the third list
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("speaker.other.error"))
          } else {
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> "Invalid state")
          }
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  // Check that the current authenticated user is the owner
  // validate the form, save and redirect.
  def saveOtherSpeakers(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findProposal(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          Proposal.proposalSpeakerForm.bindFromRequest.fold(
            hasErrors => BadRequest(html.CallForPaper.editOtherSpeaker(Webuser.getName(uuid), proposal, hasErrors)).flashing("error" -> "Errors in the proposal form, please correct errors"),
            validNewSpeakers => {
              (proposal.secondarySpeaker, validNewSpeakers._1) match {
                case (None, Some(newSecondarySpeaker)) =>
                  val newSpeaker = Speaker.findByUUID(newSecondarySpeaker)
                  val validMsg = s"Internal notification : Added [${newSpeaker.map(_.cleanName).get}] as a secondary speaker"
                  if (proposal.state != ProposalState.DRAFT) {
                    ZapActor.actor ! SendMessageToCommittee(uuid, proposal, validMsg)
                  }
                  Event.storeEvent(Event(proposal.id, uuid, validMsg))
                  Proposal.updateSecondarySpeaker(uuid, proposalId, None, Some(newSecondarySpeaker))
                case (Some(oldSpeakerUUID), Some(newSecondarySpeaker)) if oldSpeakerUUID != newSecondarySpeaker =>
                  val oldSpeaker = Speaker.findByUUID(oldSpeakerUUID)
                  val newSpeaker = Speaker.findByUUID(newSecondarySpeaker)
                  val validMsg = s"Internal notification : Removed [${oldSpeaker.map(_.cleanName).get}] and added [${newSpeaker.map(_.cleanName).get}] as a secondary speaker"
                  if (proposal.state != ProposalState.DRAFT) {
                    ZapActor.actor ! SendMessageToCommittee(uuid, proposal, validMsg)
                  }
                  Event.storeEvent(Event(proposal.id, uuid, validMsg))
                  Proposal.updateSecondarySpeaker(uuid, proposalId, Some(oldSpeakerUUID), Some(newSecondarySpeaker))
                case (Some(oldSpeakerUUID), None) =>
                  val oldSpeaker = Speaker.findByUUID(oldSpeakerUUID)
                  val validMsg = s"Internal notification : Removed [${oldSpeaker.map(_.cleanName).get}] as a secondary speaker"
                  if (proposal.state != ProposalState.DRAFT) {
                    ZapActor.actor ! SendMessageToCommittee(uuid, proposal, validMsg)
                  }
                  Event.storeEvent(Event(proposal.id, uuid, validMsg))
                  Proposal.updateSecondarySpeaker(uuid, proposalId, Some(oldSpeakerUUID), None)
                case (Some(oldSpeakerUUID), Some(newSecondarySpeaker)) if oldSpeakerUUID == newSecondarySpeaker =>
                // We kept the 2nd speaker, maybe updated or added a 3rd speaker
                case (None, None) =>
                // Nothing special
              }

              Proposal.updateOtherSpeakers(uuid, proposalId, proposal.otherSpeakers, validNewSpeakers._2)
              Event.storeEvent(Event(proposal.id, uuid, "Updated speakers list for proposal " + StringUtils.abbreviate(proposal.title, 80)))

              Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("speakers.updated"))
            }
          )
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  def deleteProposal(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findProposal(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          Proposal.delete(uuid, proposalId)
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("deleted" -> proposalId)
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  def undeleteProposal(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findDeleted(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          Proposal.draft(uuid, proposalId)
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("talk.draft"))
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  def submitProposal(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findDraft(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          Proposal.submit(uuid, proposalId)
          if (ConferenceDescriptor.notifyProposalSubmitted) {
            // This generates too many emails for France and is useless
            ZapActor.actor ! NotifyProposalSubmitted(uuid, proposal)
          }
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("talk.submitted"))
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  val speakerMsg = Form("msg" -> nonEmptyText(maxLength = 2500))

  def showCommentForProposal(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findProposal(uuid, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          Ok(html.CallForPaper.showCommentForProposal(proposal, Comment.allSpeakerComments(proposal.id), speakerMsg))
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  def sendMessageToCommittee(proposalId: String) = SecuredAction {
    implicit request =>
      val uuid = request.webuser.uuid
      val maybeProposal = Proposal.findProposal(uuid, proposalId).filterNot(_.state == ProposalState.DELETED)
      maybeProposal match {
        case Some(proposal) =>
          speakerMsg.bindFromRequest.fold(
            hasErrors => {
              BadRequest(html.CallForPaper.showCommentForProposal(proposal, Comment.allSpeakerComments(proposal.id), hasErrors))
            },
            validMsg => {
              Comment.saveCommentForSpeaker(proposal.id, uuid, validMsg)
              ZapActor.actor ! SendMessageToCommittee(uuid, proposal, validMsg)
              Redirect(routes.CallForPaper.showCommentForProposal(proposalId)).flashing("success" -> "Message was sent")
            }
          )
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }

  case class TermCount(term: String, count: Int)

  def cloudTags() = SecuredAction.async {
    implicit request =>
      import play.api.Play.current
      import play.api.libs.concurrent.Execution.Implicits.defaultContext

      implicit val termCountFormat = Json.reads[TermCount]

      Cache.getOrElse("elasticSearch", 3600) {
        ElasticSearch.getTag("proposals/proposal").map {
          case r if r.isSuccess =>
            val json = Json.parse(r.get)
            val tags = (json \ "facets" \ "tags" \ "terms").as[List[TermCount]]
            Ok(html.CallForPaper.cloudTags(tags))
          case r if r.isFailure =>
            play.Logger.error(r.get)
            InternalServerError
        }
      }
  }

  val phoneForm = play.api.data.Form("phoneNumber" -> nonEmptyText(maxLength = 15))
  val phoneConfirmForm = play.api.data.Form(tuple(
    "phoneNumber" -> nonEmptyText(maxLength = 15),
    "confirmation" -> nonEmptyText(maxLength = 15)
  )
  )

  def updatePhoneNumber() = SecuredAction.async {
    implicit request =>
      phoneForm.bindFromRequest().fold(
        invalidPhone => Future.successful(Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.phone"))),
        validPhone => {
          Future.successful {
            val code = StringUtils.left(request.webuser.uuid, 4) // Take the first 4 characters as the validation code
            if (ConferenceDescriptor.isTwilioSMSActive()) {
              TwilioSender.send(validPhone, Messages("sms.confirmationTxt", code))
              Ok(views.html.CallForPaper.enterConfirmCode(phoneConfirmForm.fill((validPhone, code))))
            } else {
              val webuser = request.webuser
              Speaker.updatePhone(webuser.uuid, validPhone, request.acceptLanguages.headOption)
              Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("phonenumber.updated.success"))
            }

          }
        }
      )
  }

  def confirmPhone() = SecuredAction {
    implicit request =>
      phoneConfirmForm.bindFromRequest().fold(
        hasErrors => Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.confirmation.code")),
        success => {
          val thePhone = success._1
          val theConfCode = success._2
          val webuser = request.webuser
          val code = StringUtils.left(request.webuser.uuid, 4) // Take the first 4 characters as the validation code
          if (theConfCode == code) {
            Speaker.updatePhone(webuser.uuid, thePhone, request.acceptLanguages.headOption)
            SmsActor.actor ! SendWelcomeAndHelp(thePhone)
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("phonenumber.updated.success"))
          } else {
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.confirmation.code"))
          }

        }
      )
  }
}
