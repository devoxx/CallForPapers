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

import java.text.SimpleDateFormat

import akka.util.Crypt
import library._
import models.Review._
import play.api.data.Form
import library.{NotifyProposalSubmitted, SendMessageToCommitte, ZapActor}
import models._
import play.api.data.Forms._
import play.api.i18n.Messages
import org.joda.time.{DateTime, DateTimeZone, Period}

import scala.concurrent.Future

/**
  * Sans doute le controller le plus sadique du monde qui accepte ou rejette les propositions
  * Created by nmartignole on 30/01/2014.
  */
object ApproveOrRefuse extends SecureCFPController {

  def doApprove(proposalId: String) = SecuredAction(IsMemberOf("cfp")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).map {
        proposal =>
          ApprovedProposal.approve(proposal)
          Event.storeEvent(Event(proposalId, request.webuser.uuid, s"Approved ${Messages(proposal.talkType.id)} [${proposal.title}] in track [${Messages(proposal.track.id)}]"))
          Future.successful(Redirect(routes.CFPAdmin.allVotes(proposal.talkType.id, None)).flashing("success" -> s"Talk ${proposal.id} has been accepted."))
      }.getOrElse {
        Future.successful(Redirect(routes.CFPAdmin.allVotes("all", None)).flashing("error" -> "Talk not found"))
      }
  }

  def doRefuse(proposalId: String) = SecuredAction(IsMemberOf("cfp")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).map {
        proposal =>
          ApprovedProposal.refuse(proposal)
          Event.storeEvent(Event(proposalId, request.webuser.uuid, s"Refused ${Messages(proposal.talkType.id)} [${proposal.title}] in track [${Messages(proposal.track.id)}]"))
          Future.successful(Redirect(routes.CFPAdmin.allVotes(proposal.talkType.id, None)).flashing("success" -> s"Talk ${proposal.id} has been refused."))
      }.getOrElse {
        Future.successful(Redirect(routes.CFPAdmin.allVotes("all", None)).flashing("error" -> "Talk not found"))
      }
  }

  def cancelApprove(proposalId: String) = SecuredAction(IsMemberOf("cfp")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).map {
        proposal =>
          val confType: String = proposal.talkType.id
          ApprovedProposal.cancelApprove(proposal)
          Event.storeEvent(Event(proposalId, request.webuser.uuid, s"Cancel Approved on ${Messages(proposal.talkType.id)} [${proposal.title}] in track [${Messages(proposal.track.id)}]"))
          Future.successful(Redirect(routes.CFPAdmin.allVotes(proposal.talkType.id, Some(confType))).flashing("success" -> s"Talk ${proposal.id} has been removed from Approved list."))
      }.getOrElse {
        Future.successful(Redirect(routes.CFPAdmin.allVotes("all", None)).flashing("error" -> "Talk not found"))
      }
  }

  def cancelRefuse(proposalId: String) = SecuredAction(IsMemberOf("cfp")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).map {
        proposal =>
          val confType: String = proposal.talkType.id
          ApprovedProposal.cancelRefuse(proposal)
          Event.storeEvent(Event(proposalId, request.webuser.uuid, s"Cancel Refused on ${Messages(proposal.talkType.id)} [${proposal.title}] in track [${Messages(proposal.track.id)}]"))
          Future.successful(Redirect(routes.CFPAdmin.allVotes(proposal.talkType.id, Some(confType))).flashing("success" -> s"Talk ${proposal.id} has been removed from Refused list."))
      }.getOrElse {
        Future.successful(Redirect(routes.CFPAdmin.allVotes("all", None)).flashing("error" -> "Talk not found"))
      }
  }

  def allApprovedByTalkType(talkType: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.ApproveOrRefuse.allApprovedByTalkType(ApprovedProposal.allApprovedByTalkType(talkType), talkType,ApprovedProposal.allNotifyRefused()))
  }

  def allRefusedByTalkType(talkType: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.ApproveOrRefuse.allRefusedByTalkType(ApprovedProposal.allRefusedByTalkType(talkType), talkType))
  }
  val formnotifyApprove2 =Form(tuple("email"->optional(text),"subject"->optional(text)))
  def notifyafterrefused(talkType: String, proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      formnotifyApprove2.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val emailContent= validForm._1
          val mybProposal =Proposal.findById(proposalId)
          mybProposal match {
            case None => Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" ->"Invalid Proposal ")
            case  Some(p) =>
              val uuid= p.mainSpeaker
              emailContent match {
                case Some(content)=>
                  ApprovedProposal.cancelrejecte(p)
                  validForm._2 match {
                    case Some(a)=>  ZapActor.actor ! ProposalApprovedAfeterRefese(request.webuser.uuid, p,content,Some(a))
                    case None=>ZapActor.actor ! ProposalApprovedAfeterRefese(request.webuser.uuid, p,content,None)
                  }

                  Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("success" -> s"Notified speakers for Proposal ID $proposalId")
                case None => Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" -> "not find content")

              }
          }}) }

  def notifycustomemailapproved(talkType: String, proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      formnotifyApprove2.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val emailContent= validForm._1
          val mybProposal =Proposal.findById(proposalId)
          mybProposal match {
            case None => Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" ->"Invalid Proposal ")
            case  Some(p) =>
              val uuid= p.mainSpeaker
              emailContent match {
                case Some(content)=>
                  validForm._2 match {
                    case Some(a)=>  ZapActor.actor ! ProposalApprovedAfeterRefese(request.webuser.uuid, p,content,Some(a))
                    case None=>ZapActor.actor ! ProposalApprovedAfeterRefese(request.webuser.uuid, p,content,None)
                  }

                  Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("success" -> s"Notified speakers for Proposal ID $proposalId")
                case None => Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" -> "not find content")

              }
          }}) }
  def notifycustomemailrefused(talkType: String, proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      formnotifyApprove2.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.allRefusedByTalkType(talkType)).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val emailContent= validForm._1
          val mybProposal =Proposal.findById(proposalId)
          mybProposal match {
            case None => Redirect(routes.ApproveOrRefuse.allRefusedByTalkType(talkType)).flashing("error" ->"Invalid Proposal ")
            case  Some(p) =>
              ApprovedProposal.rejecte(p)

              val uuid= p.mainSpeaker
              emailContent match {
                case Some(content)=>
                  validForm._2 match {
                    case Some(a)=>  ZapActor.actor ! ProposalcustomRefese(request.webuser.uuid, p,content,Some(a))
                    case None=>ZapActor.actor ! ProposalcustomRefese(request.webuser.uuid, p,content,None)
                  }

                  Redirect(routes.ApproveOrRefuse.allRefusedByTalkType(talkType)).flashing("success" -> s"Notified speakers for Proposal ID $proposalId")
                case None => Redirect(routes.ApproveOrRefuse.allRefusedByTalkType(talkType)).flashing("error" -> "not find content")

              }
          }}) }
  val formnotifyApprove =Form("deadline"->optional(date("MM/dd/yyyy")))
  def notifyApprove(talkType: String, proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      formnotifyApprove.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
      val deadline = validForm
      val mybProposal =Proposal.findById(proposalId)
          mybProposal match {
            case None => Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" ->"Invalid Proposal ")
            case  Some(p) =>
              val uuid= p.mainSpeaker
             deadline match {
              case None =>
              case Some(d)=>
                val dat:DateTime = new DateTime(new SimpleDateFormat("yyyy-MM-dd").format(d))
                val proposalupdate= p.copy(deadline=Some(dat) )
          Proposal.save(uuid,Proposal.setMainSpeaker(proposalupdate,uuid),p.state)

             }}
          Proposal.findById(proposalId) match {
            case None =>Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" ->"Invalid Proposal ")
            case Some(pp) => ZapActor.actor ! ProposalApproved(request.webuser.uuid, pp)
          Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("success" -> s"Notified speakers for Proposal ID $proposalId")

          }

        }) }


def notifyRefused(talkType: String, proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).foreach {
        proposal: Proposal =>
                ApprovedProposal.rejecte(proposal)

          ZapActor.actor ! ProposalRefused(request.webuser.uuid, proposal)
      }
    Redirect(routes.ApproveOrRefuse.allRefusedByTalkType(talkType)).flashing("success" -> s"Notified speakers for Proposal ID $proposalId")
  }
  def notifyrefusedAll(talkType:String) = SecuredAction(IsMemberOf("cfp")){
    implicit request :SecuredRequest[play.api.mvc.AnyContent]=>
      val prposalslist:List[Proposal]=ApprovedProposal.allRefusedByTalkType(talkType).filter(p=>(p.state.code=="rejected" ))
      prposalslist.foreach(pro=>
        Proposal.findById(pro.id).foreach {
          proposal: Proposal =>
            ApprovedProposal.rejecte(proposal)

            ZapActor.actor ! ProposalRefused(request.webuser.uuid, proposal)
        }
      )
    Redirect(routes.ApproveOrRefuse.allRefusedByTalkType(talkType)).flashing("success" -> "Notified all speakers for Proposal Refused")
  }
  def notifyApproveddAll(talkType:String) = SecuredAction(IsMemberOf("cfp")){
    implicit request :SecuredRequest[play.api.mvc.AnyContent]=>
      formnotifyApprove.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val prposalslist:List[Proposal]=ApprovedProposal.allApprovedByTalkType(talkType).filter(p=>p.state.code=="approved")
          val deadline = validForm
          prposalslist.foreach(pro=>
          Proposal.findById(pro.id) match {
            case None =>
            case  Some(p) =>
              val uuid= p.mainSpeaker
              deadline match {
                case None =>
                case Some(d)=>
                  val dat:DateTime = new DateTime(new SimpleDateFormat("yyyy-MM-dd").format(d))
                  val proposalupdate= p.copy(deadline=Some(dat) )
                  Proposal.save(uuid,Proposal.setMainSpeaker(proposalupdate,uuid),p.state)
                  ZapActor.actor ! ProposalApproved(request.webuser.uuid, proposalupdate)
              }})
          Redirect(routes.ApproveOrRefuse.allApprovedByTalkType(talkType)).flashing("success" -> "Notified all speakers for Proposal Approved")

        })

  }
  def  notifyallAcceptedtoAcceptteTermeAndCondition()=SecuredAction(IsMemberOf("cfp")){
    implicit  request : SecuredRequest[play.api.mvc.AnyContent]=>
     val proposals=Proposal.allAccepted()
      val allSpeakeruuid= proposals.flatMap(x=>x.allSpeakerUUIDs).toList.distinct
      val onlySpeakersThatNotAcceptedTerms: List[String] = allSpeakeruuid.filter(uuid => Speaker.needsToAccept(uuid))
      if(onlySpeakersThatNotAcceptedTerms!=0){
       onlySpeakersThatNotAcceptedTerms.foreach(uuid=>
          ZapActor.actor ! allAcceptedtoAcceptteTermeAndCondition(uuid)
        )
        Redirect(routes.Backoffice.allProposals()).flashing("success" -> "Notified all speakers for Proposal Accepted to accept terms and conditions ")

      }else{
        Redirect(routes.Backoffice.allProposals()).flashing("success" -> " all speakers for Proposal Accepted they have accepted terms and conditions ")

      }

  }


  val formApprove = Form(
    "accept.chk" -> checked("accept.term.checked")
  )

  def showAcceptTerms() = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      if (Speaker.needsToAccept(request.webuser.uuid)) {
        Ok(views.html.ApproveOrRefuse.showAcceptTerms(formApprove))
      } else {
        Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("success" -> Messages("acceptedTerms.msg"))
      }
  }

  def acceptTermsAndConditions() = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      formApprove.bindFromRequest().fold(
        hasErrors => BadRequest(views.html.ApproveOrRefuse.showAcceptTerms(hasErrors)),
        successForm => {
          Speaker.doAcceptTerms(request.webuser.uuid)
          Event.storeEvent(Event("speaker", request.webuser.uuid, "has accepted Terms and conditions"))
          Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks())
        }
      )
  }

  def declineTermsAndConditions() = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Speaker.refuseTerms(request.webuser.uuid)
      Event.storeEvent(Event("speaker", request.webuser.uuid, "has REFUSED Terms and conditions"))
      Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("refused.termsConditions"))
  }

  def showAcceptOrRefuseTalks() = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      import org.apache.commons.lang3.RandomStringUtils
      val allMyProposals = Proposal.allMyProposals(request.webuser.uuid)
      val cssrf = RandomStringUtils.randomAlphanumeric(24)

      val (accepted, rejected) = allMyProposals.partition(p => p.state == ProposalState.APPROVED ||p.state==ProposalState.REPLYLATER||p.state == ProposalState.DECLINED || p.state == ProposalState.ACCEPTED || p.state == ProposalState.BACKUP)
      Ok(views.html.ApproveOrRefuse.acceptOrRefuseTalks(accepted, rejected.filter(_.state == ProposalState.REJECTED), cssrf))
        .withSession(request.session.+(("CSSRF", Crypt.sha1(cssrf))))
  }
  val formreply =Form(tuple("proposalId"->nonEmptyText(maxLength = 8),"deadline"->jodaDate))
  def doReplyLaterTalks()= SecuredAction{
    implicit  request: SecuredRequest[play.api.mvc.AnyContent]=>
      formreply.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val proposalId=validForm._1
          val deadline=Some(validForm._2)
          Proposal.findById(proposalId) match {
            case None =>Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> Messages("ar.proposalNotFound"))
            case Some(p)if Proposal.isSpeaker(proposalId, request.webuser.uuid) =>
              //update deadline  for pproposal
              val uuid = request.webuser.uuid
              val proposalupdate= p.copy(deadline=deadline)
              Proposal.save(uuid,Proposal.setMainSpeaker(proposalupdate,uuid),p.state)
              Proposal.replylater(uuid,proposalId)
              val date=validForm._2.toString("YYYY/MM/dd")
              val validMsg = "Speaker has set the status of this proposal to Replay Later at the latest:"+date
              Comment.saveCommentForSpeaker(proposalId, request.webuser.uuid, validMsg)
              ZapActor.actor ! SendMessageToCommitte(request.webuser.uuid, p, validMsg)
          }
          Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("success" -> Messages("ar.choiceRecorded", proposalId,"reply later"))})
  }
  //accept presentation
  val formAcceptandpreferencedays =Form(tuple("proposalId"->nonEmptyText(maxLength = 8),"preferencedays"->list(text)))
  def doAcceptTalks()= SecuredAction{
    implicit  request: SecuredRequest[play.api.mvc.AnyContent]=>
      formAcceptandpreferencedays.bindFromRequest().fold(hasErrors =>Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val proposalId=validForm._1
          var allpreferenceday:String=""
          validForm._2.foreach(x=> allpreferenceday=allpreferenceday+x+" && ")
          val preferenceDays=Some(allpreferenceday)
          Proposal.findById(proposalId) match {
            case None =>Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> Messages("ar.proposalNotFound"))
            case Some(p)if Proposal.isSpeaker(proposalId, request.webuser.uuid) =>

              if (List(ProposalState.APPROVED, ProposalState.BACKUP, ProposalState.ACCEPTED,ProposalState.REPLYLATER, p.state == ProposalState.DECLINED).contains(p.state)) {

                //update proposal preference
               val uuid = request.webuser.uuid
                val updatedProposal =  p.copy(preferences=preferenceDays)
                Proposal.save(uuid, Proposal.setMainSpeaker(updatedProposal, uuid), p.state)
                /// fin update
                Proposal.accept(uuid, proposalId)
                val validMsg = "Speaker has set the status of this proposal to ACCEPTED"
                Comment.saveCommentForSpeaker(proposalId, request.webuser.uuid, validMsg)
                ZapActor.actor ! SendMessageToCommitte(request.webuser.uuid, p, validMsg)
                 Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("success" -> Messages("ar.choiceRecorded", proposalId,"Accept"))
              } else {
                ZapActor.actor ! SendMessageToCommitte(request.webuser.uuid, p, "un utilisateur a essayé de changer le status de son talk... User:" + request.webuser.cleanName + " talk:" + p.id + " state:" + p.state.code)
                Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks())
              }
          }
          })
  }

  val formAccept = Form(tuple("proposalId" -> nonEmptyText(maxLength = 8), "dec" -> nonEmptyText, "cssrf_t" -> nonEmptyText ))

  def doAcceptOrRefuseTalk() = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      formAccept.bindFromRequest().fold(hasErrors =>
        Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          val cssrf = Crypt.sha1(validForm._3)
          val fromSession = request.session.get("CSSRF")
          if (Some(cssrf) != fromSession) {
            Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> "Invalid CSSRF token")
          } else {

            val proposalId = validForm._1
            val choice = validForm._2
            val maybeProposal = Proposal.findById(proposalId)

            maybeProposal match {

              case None => Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> Messages("ar.proposalNotFound"))

              case Some(p) if Proposal.isSpeaker(proposalId, request.webuser.uuid) =>
                choice match {

                  case "decline" =>
                    if (List(ProposalState.APPROVED, ProposalState.BACKUP,ProposalState.REPLYLATER, ProposalState.ACCEPTED, p.state == ProposalState.DECLINED).contains(p.state)) {
                      Proposal.decline(request.webuser.uuid, proposalId)
                      val validMsg = "Speaker has set the status of this proposal to DECLINED"
                      Comment.saveCommentForSpeaker(proposalId, request.webuser.uuid, validMsg)
                      ZapActor.actor ! SendMessageToCommitte(request.webuser.uuid, p, validMsg)
                    } else {
                      ZapActor.actor ! SendMessageToCommitte(request.webuser.uuid, p, "un utilisateur a essayé de changer le status de son talk... User:" + request.webuser.cleanName + " talk:" + p.id + " state:" + p.state.code)
                    }
                  case "backup" =>
                    val validMsg = "Speaker has set the status of this proposal to BACKUP"
                    Comment.saveCommentForSpeaker(proposalId, request.webuser.uuid, validMsg)
                    ZapActor.actor ! SendMessageToCommitte(request.webuser.uuid, p, validMsg)
                    Proposal.backup(request.webuser.uuid, proposalId)
                  case other => play.Logger.error("Invalid choice for ApproveOrRefuse doAcceptOrRefuseTalk for proposalId " + proposalId + " choice=" + choice)
                }

                Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("success" -> Messages("ar.choiceRecorded", proposalId, choice))
              case other => Redirect(routes.ApproveOrRefuse.showAcceptOrRefuseTalks()).flashing("error" -> "Hmmm not a good idea to try to update someone else proposal... this event has been logged.")
            }
          }
        }
      )
  }


  def prepareMassRefuse(confType: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      ProposalType.all.find(_.id == confType).map {
        proposalType =>
          val reviews: Map[String, (Score, TotalVoter, TotalAbst, AverageNote, StandardDev)] = Review.allVotes()

          val onlyReviewedButNotApproved:Set[String]=reviews.keySet.diff(ApprovedProposal.allApprovedProposalIDs()).diff(ApprovedProposal.allRefusedProposalIDs())

          val allProposals = Proposal.loadAndParseProposals(onlyReviewedButNotApproved, proposalType)

          val listOfProposals = reviews.flatMap {
            case (proposalId, scoreAndVotes) =>
              val maybeProposal = allProposals.get(proposalId)
              if (maybeProposal.isDefined) {
                Option(maybeProposal.get, scoreAndVotes._4)
              } else {
                // It's ok to discard other talk than the confType requested
                None
              }
          }

          val sortedList = listOfProposals.toList.sortBy {
            case (proposal, score) => score.n
          }

          Ok(views.html.ApproveOrRefuse.prepareMassRefuse(sortedList, confType))
      }.getOrElse(NotFound("Proposal not found"))

  }

  def doRefuseAndRedirectToMass(proposalId:String, confType:String)=SecuredAction(IsMemberOf("admin")).async{
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).map {
        proposal =>
          ApprovedProposal.refuse(proposal)
          Event.storeEvent(Event(proposalId, request.webuser.uuid, s"Refused ${Messages(proposal.talkType.id)} [${proposal.title}] in track [${Messages(proposal.track.id)}]"))
          Future.successful(Redirect(routes.ApproveOrRefuse.prepareMassRefuse(confType)))
      }.getOrElse {
        Future.successful(NotFound("Talk not found for this proposalId "+proposalId))
      }
  }
}
