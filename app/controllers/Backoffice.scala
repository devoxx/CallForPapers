package controllers

import scala.concurrent.duration._
import library.search.{DoIndexProposal, _}
import library.{DraftReminder, Redis, ZapActor }
import models.{Tag, _}
import org.joda.time.{DateTime, Instant}
import play.api.Play
import library._
import models._
import org.joda.time.{DateMidnight, DateTime, Instant}
import play.api.cache.EhCachePlugin
import play.api.data._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Action

import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent}
import play.api.Play
/**
  * Backoffice actions, for maintenance and validation.
  *
  * Author: nicolas martignole
  * Created: 02/12/2013 21:34
  */
object Backoffice extends SecureCFPController {
  implicit val SCHEDULE_IN_PROGRESS_DISPLAY_STATUS_FIELD = "status";

  def homeBackoffice() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Backoffice.homeBackoffice())
  }

  // Add or remove the specified user from "cfp" security group
  def switchCFPAdmin(uuidSpeaker: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Webuser.findByUUID(uuidSpeaker).filterNot(_.uuid == "bd894205a7d579351609f8dcbde49b9ffc0fae13").map {
        webuser =>
          if (Webuser.hasAccessToCFP(uuidSpeaker)) {
            Event.storeEvent(Event(uuidSpeaker, request.webuser.uuid, s"removed ${webuser.cleanName} from CFP group"))
            Webuser.removeFromCFPAdmin(uuidSpeaker)
          } else {
            Webuser.addToCFPAdmin(uuidSpeaker)
            Event.storeEvent(Event(uuidSpeaker, request.webuser.uuid, s"added ${webuser.cleanName} to CFP group"))
          }
          Redirect(routes.CFPAdmin.allWebusers())
      }.getOrElse {
        NotFound("Webuser not found")
      }
  }

  // Authenticate on CFP on behalf of specified user.
  def authenticateAs(uuidSpeaker: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      // Block redirect if the uuidSpeaker belongs to the ADMIN group and not you
      if (Webuser.isMember(uuidSpeaker, "admin") && Webuser.isNotMember(request.webuser.uuid, "admin")) {
        Unauthorized("Sorry, only admin user can become admin.")
      } else {
        Redirect(routes.CallForPaper.homeForSpeaker()).withSession("uuid" -> uuidSpeaker)
      }
  }

  def authenticateAndCreateTalk(uuidSpeaker: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      if (Webuser.isMember(uuidSpeaker, "admin") && Webuser.isNotMember(request.webuser.uuid, "admin")) {
        Unauthorized("Sorry, only admin user can become admin.")
      } else {
        Redirect(routes.CallForPaper.newProposal()).withSession("uuid" -> uuidSpeaker)
      }
  }

  def allProposals(proposalId: Option[String]) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      proposalId match {
        case Some(id) =>
          val proposal = Proposal.findById(id)
          proposal match {
            case None => NotFound("Proposal not found")
            case Some(pr) => Ok(views.html.Backoffice.allProposals(List(pr)))
          }
        case None =>
          val proposals = Proposal.allProposals().sortBy(_.state.code)
          Ok(views.html.Backoffice.allProposals(proposals))
      }


  }

  // This endpoint is deliberately *not* secured in order to transform a user into an admin
  // only if there isn't any admin in the application
  def bootstrapAdminUser(uuid: String) = Action {
    implicit request =>
      if (Webuser.noBackofficeAdmin()) {
        Webuser.addToBackofficeAdmin(uuid)
        Webuser.addToCFPAdmin(uuid)
        Redirect(routes.Application.index()).flashing("success" -> "Your UUID has been configured as super-admin")
      } else {
        Redirect(routes.Application.index()).flashing("error" -> "There is already an Admin user")
      }
  }

  def clearCaches() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Play.current.plugin[EhCachePlugin].foreach(p => p.manager.clearAll())
      Ok(views.html.Backoffice.homeBackoffice())
  }

  def changeProposalState(proposalId: String, state: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.changeProposalState(request.webuser.uuid, proposalId, ProposalState.parse(state))

      if (state == ProposalState.ACCEPTED.code) {
        Proposal.findById(proposalId).foreach {
          proposal =>
            ApprovedProposal.approve(proposal)
            ElasticSearchActor.masterActor ! DoIndexProposal(proposal.copy(state = ProposalState.ACCEPTED))
        }
      }
      if (state == ProposalState.DECLINED.code) {
        Proposal.findById(proposalId).foreach {
          proposal =>
            ApprovedProposal.refuse(proposal)
            ElasticSearchActor.masterActor ! DoIndexProposal(proposal.copy(state = ProposalState.DECLINED))
        }
      }


      Redirect(routes.Backoffice.allProposals()).flashing("success" -> ("Changed state to " + state))
  }

  val formSecu = Form("secu" -> nonEmptyText())

  def deleteSpeaker(speakerUUIDToDelete: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      if (Webuser.isMember(speakerUUIDToDelete, "cfp") || Webuser.isMember(speakerUUIDToDelete, "admin")) {
        Redirect(routes.CFPAdmin.index()).flashing("error" -> s"We cannot delete CFP admin user...")
      } else {
        formSecu.bindFromRequest.fold(invalid => {
          Redirect(routes.CFPAdmin.index()).flashing("error" -> "You did not enter DEL... are you drunk?")
        }, _ => {
          Speaker.delete(speakerUUIDToDelete)
          Webuser.findByUUID(speakerUUIDToDelete).foreach {
            w =>
              Webuser.delete(w)
              Event.storeEvent(Event(speakerUUIDToDelete, uuid, s"Deleted webuser ${w.cleanName} ${w.uuid}"))
          }
          Redirect(routes.CFPAdmin.index()).flashing("success" -> s"Speaker $speakerUUIDToDelete deleted")
        })
      }
  }

  def doIndexElasticSearch() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      ElasticSearchActor.masterActor ! DoIndexAllSpeakers
      ElasticSearchActor.masterActor ! DoIndexAllProposals
      ElasticSearchActor.masterActor ! DoIndexAllAccepted
      ElasticSearchActor.masterActor ! DoIndexAllHitViews
      ElasticSearchActor.masterActor ! DoIndexSchedule
      Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> "Elastic search actor started...")
  }

  def doResetAndConfigureElasticSearch() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      ElasticSearchActor.masterActor ! DoCreateConfigureIndex
      Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> "Deleted and now creating [speakers] and [proposals] indexes. Please force an indexer in one or two minutes.")
  }

  // If a user is not a member of cfp security group anymore, then we need to delete all its votes.
  def cleanUpVotesIfUserWasDeleted = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.allProposalIDs.foreach {
        proposalID: String =>
          Review.allVotesFor(proposalID).foreach {
            case (reviewerUUID, _) =>
              if (Webuser.doesNotExist(reviewerUUID)) {
                play.Logger.of("application.Backoffice").debug(s"Deleting vote on $proposalID for user $reviewerUUID")
                Review.removeVoteForProposal(proposalID, reviewerUUID)
              }
          }
      }
      Ok("Done")
  }

  def deleteVotesForPropal(proposalId: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Review.allVotesFor(proposalId).foreach {
        case (reviewerUUID, score) =>
          play.Logger.of("application.Backoffice").info(s"Deleting vote on $proposalId by $reviewerUUID of score $score")
          Review.deleteVoteForProposal(proposalId)
          ReviewByGoldenTicket.deleteVoteForProposal(proposalId)
      }
      Redirect(routes.CFPAdmin.showVotesForProposal(proposalId))
  }

  def submittedByDate() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      Redis.pool.withClient {
        client =>
          val toReturn = client.hgetAll("Proposal:SubmittedDate").map {
            case (proposal, submitted) =>
              (proposal, new Instant(submitted.toLong).toDateTime.toString("dd-MM-yyyy"))
          }.groupBy(_._2).map {
            tuple =>
              (tuple._1, tuple._2.size)
          }.toList.sortBy(_._1).map {
            s =>
              s._1 + ", " + s._2 + "\n"
          }

          Ok("Date, total\n" + toReturn.mkString).as("text/plain")
      }
  }

  def sanityCheckSchedule() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val allPublishedProposals = ScheduleConfiguration.loadAllPublishedSlots().filter(_.proposal.isDefined)
      val publishedTalksExceptBOF = allPublishedProposals.filterNot(_.proposal.get.talkType == ConferenceDescriptor.ConferenceProposalTypes.BOF)

      val declined = publishedTalksExceptBOF.filter(_.proposal.get.state == ProposalState.DECLINED)
      val approved = publishedTalksExceptBOF.filter(_.proposal.get.state == ProposalState.APPROVED)
      val accepted = allPublishedProposals.filter(_.proposal.get.state == ProposalState.ACCEPTED)

      val allSpeakersIDs = allPublishedProposals.flatMap(_.proposal.get.allSpeakerUUIDs).toSet
      
      // For Terms&Conditions we focus on all talks except BOF
      val allSpeakersExceptBOF = allPublishedProposals.flatMap(_.proposal.get.allSpeakerUUIDs).toSet
      val onlySpeakersThatNeedsToAcceptTerms: Set[String] = allSpeakersExceptBOF.filter(uuid => Speaker.needsToAccept(uuid)).filter {
        speakerUUID =>
          // Keep only speakers with at least one accepted or approved talk
          approved.exists(_.proposal.get.allSpeakerUUIDs.contains(speakerUUID)) || accepted.exists(_.proposal.get.allSpeakerUUIDs.contains(speakerUUID))
      }

      val allSpeakers = Speaker.loadSpeakersFromSpeakerIDs(onlySpeakersThatNeedsToAcceptTerms)

      // Speaker declined talk AFTER it has been published
      val acceptedThenChangedToOtherState = accepted.filter {
        slot: Slot =>
          val proposal = slot.proposal.get
          !Proposal.findProposalState(proposal.id).contains(ProposalState.ACCEPTED)
      }

      // ALL Talks for Conflict search
      val approvedOrAccepted = allPublishedProposals.filter(p => p.proposal.get.state == ProposalState.ACCEPTED || p.proposal.get.state == ProposalState.APPROVED)

      val specialSpeakers = allSpeakersIDs

      // Speaker that do a presentation with same TimeSlot (which is obviously not possible)
      val allWithConflicts: Set[(Speaker, Map[DateTime, Iterable[Slot]])] =
        for (speakerId <- specialSpeakers) yield {
        val proposalsPresentedByThisSpeaker: List[Slot] = approvedOrAccepted.filter(_.proposal.get.allSpeakerUUIDs.contains(speakerId))
        val groupedByDate = proposalsPresentedByThisSpeaker.groupBy(_.from)
        val conflict = groupedByDate.filter(_._2.size > 1)
        val speaker = Speaker.findByUUID(speakerId).get
        (speaker, conflict)
      }

      Ok(
        views.html.Backoffice.sanityCheckSchedule(
          declined,
          approved,
          acceptedThenChangedToOtherState,
          allSpeakers,
          allWithConflicts.filter(_._2.nonEmpty)
        )
      )

  }

  def sanityCheckProposals() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Redis.pool.withClient {
        client =>
          val toReturn = client.hgetAll("Proposals").map {
            case (proposalId, json) =>
              (proposalId, Json.parse(json).asOpt[Proposal])
          }.filter(_._2.isEmpty).keys
          Ok(views.html.Backoffice.sanityCheckProposals(toReturn))
      }
  }

  def fixToAccepted(slotId: String, proposalId: String, talkType: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val maybeUpdated = for (
        scheduleId <- ScheduleConfiguration.getPublishedSchedule(talkType);
        scheduleConf <- ScheduleConfiguration.loadScheduledConfiguration(scheduleId);
        slot <- scheduleConf.slots.find(_.id == slotId).filter(_.proposal.isDefined).filter(_.proposal.get.id == proposalId)
      ) yield {
        val updatedProposal = slot.proposal.get.copy(state = ProposalState.ACCEPTED)
        val updatedSlot = slot.copy(proposal = Some(updatedProposal))
        val newListOfSlots = updatedSlot :: scheduleConf.slots.filterNot(_.id == slotId)
        newListOfSlots
      }

      maybeUpdated.map {
        newListOfSlots =>
          val newID = ScheduleConfiguration.persist(talkType, newListOfSlots, request.webuser )
          ScheduleConfiguration.publishConf(newID, talkType)

          Redirect(routes.Backoffice.sanityCheckSchedule()).flashing("success" -> s"Created a new scheduleConfiguration ($newID) and published a new agenda.")
      }.getOrElse {
        NotFound("Unable to update Schedule configuration, did not find the slot, the proposal or the scheduleConfiguraiton")
      }
  }

  def sendDraftReminder = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      if ( ! isCFPOpen() ) {
        val notSentDraftMessageAsCFPIsClosed = s"Draft reminders HAVE NOT BEEN sent to speakers, as CFP already CLOSED on ${cfpClosingDate().toString("EEEE, dd/MM/YYYY HH:mm")}."
        play.Logger.debug(notSentDraftMessageAsCFPIsClosed)
        Redirect(routes.Backoffice.homeBackoffice()).flashing("error" -> notSentDraftMessageAsCFPIsClosed)
      } else {
        ZapActor.actor ! DraftReminder()
        Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> "Sent draft reminder to speakers")
      }
  }

  def showAllDeclined() = SecuredAction(IsMemberOf("admin")) {
    implicit request =>

      val allDeclined = Proposal.allDeclinedProposals()
      //      Proposal.decline(request.webuser.uuid, proposalId)
      Ok(views.html.Backoffice.showAllDeclined(allDeclined))
  }

  def exportAgenda = Action {
    implicit request =>
      val publishedConf = ScheduleConfiguration.loadAllPublishedSlots()
      Ok(views.html.Backoffice.exportAgenda(publishedConf))
  }

  // Tag related controllers

  def showAllTags = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val allTags = Tag.allTags().sortBy(t => t.value)
      Ok(views.html.Backoffice.showAllTags(allTags))
  }

  // Tag related controllers

  def newTag = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      Ok(views.html.Backoffice.newTag(Tag.tagForm))
  }

  def saveTag() = SecuredAction(IsMemberOf("admin")) {
    implicit request =>

      Tag.tagForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.Backoffice.newTag(hasErrors)),
        tagData => {
          // Is it an update?
          if (Tag.findById(tagData.id).nonEmpty) {
            Tag.delete(tagData.id)
            Tag.save(Tag.createTag(tagData.value))
          } else {
            Tag.save(tagData)
          }
        }
      )

      Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> Messages("tag.saved"))
  }

  def editTag(uuid: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val foundTag = Tag.findById(uuid)
      foundTag match {
        case None => NotFound("Sorry, this tag does not exit")
        case Some(tag) => {
          Ok(views.html.Backoffice.newTag(Tag.tagForm.fill(tag)))
        }
      }
  }

  def importTags = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      Ok(views.html.Backoffice.importTags(Tag.tagForm))
  }

  def exportTags = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val allTags = Tag.allTags().sortBy(t => t.value)
      Ok(views.html.Backoffice.exportTags(allTags))
  }

  def saveImportTags() = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      Tag.tagForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.Backoffice.importTags(hasErrors)),
        tagData => {
          val tags = tagData.value.split(";")
          tags.foreach(f => Tag.save(Tag.createTag(f)))
        }
      )

      Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> Messages("tag.imported"))
  }

  def deleteTag(id: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      if (Tags.isTagLinkedByProposal(id)) {
        BadRequest("Tag is used by a proposal, unlink tag first.")
      } else {
        val tagValue = Tag.findTagValueById(id)
        if (tagValue.isDefined) {
          Tag.delete(id)
          Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> Messages("tag.removed", tagValue.get))
        } else {
          BadRequest("Tag ID doesn't exist")
        }
      }
  }

  def importRooms = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      Ok(views.html.Backoffice.importRooms(RoomImport.roomImportForm))
  }

  def saveImportRooms() = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val ROOM_NAME = 0
      val ROOM_CAPACITY = 1
      val ROOM_SETUP = 2
      val ROOM_RECORDED = 3

      RoomImport.roomImportForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.Backoffice.importRooms(hasErrors)),
        roomImportData => {
          play.Logger.info(roomImportData.commaSeparatedValues)
          val rooms = roomImportData.commaSeparatedValues.split(";")
          play.Logger.info(rooms.toString)
          rooms.foreach(
            eachRoom => {
              play.Logger.info(eachRoom)
              
              val roomToken = eachRoom.split(",")
              Room.saveRoom(
                Room.createRoom(
                  Room.generateId(),
                  roomToken(ROOM_NAME),
                  roomToken(ROOM_CAPACITY).trim.toInt,
                  roomToken(ROOM_SETUP),
                  roomToken(ROOM_RECORDED)
                )
              )
            }
          )
        }
      )

      Redirect(routes.CFPAdmin.manageRoom()).flashing("success" -> Messages("rooms.imported"))
  }

  def doDailyDigests = SecuredAction(IsMemberOf("admin")) {

    implicit request =>

      import play.api.Play.current
      import library.Contexts.statsContext

      Akka.system.scheduler.scheduleOnce(1 milliseconds, ZapActor.actor, EmailDigests(Digest.DAILY))
      play.Logger.info(s"Scheduled akka system to send out Daily email digests immediately (1 millisecond delay, only once - no interval).")

      Redirect(routes.Backoffice.showDigests()).flashing("success" -> "Daily digest sent")
  }


  def doWeeklyDigests = SecuredAction(IsMemberOf("admin")) {

    implicit request =>

      import play.api.Play.current
      import library.Contexts.statsContext

      Akka.system.scheduler.scheduleOnce(1 milliseconds, ZapActor.actor, EmailDigests(Digest.WEEKLY))
      play.Logger.info(s"Scheduled akka system to send out Weekly email digests immediately (1 millisecond delay, only once - no interval).")

      Redirect(routes.Backoffice.showDigests()).flashing("success" -> "Weekly digest sent")
  }

  def isScheduleInProgressMessageDisplayStatus(): Option[String] = Redis.pool.withClient {
    implicit client => client.hget("InProgress:Schedule", SCHEDULE_IN_PROGRESS_DISPLAY_STATUS_FIELD)
  }

  def setScheduleInProgressMessage(value: String) = Redis.pool.withClient {
    implicit client => client.hset("InProgress:Schedule", SCHEDULE_IN_PROGRESS_DISPLAY_STATUS_FIELD, value)
  }

  def setSchedulingInProgressMessageTo(displayStatus: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      controllers.Backoffice.setScheduleInProgressMessage(displayStatus)
      Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> Messages("scheduling.in.progress.status.changed", displayStatus))
  }

  def newOrUpdateCFPDates() = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      play.Logger.debug("Loading new or update CFP Dates")
      CFPDates.load() match {
        case Some(savedCFPDates) => Ok(views.html.Backoffice.newOrUpdateCFPDates(CFPDates.cfpDatesForm.fill(savedCFPDates)))
        case None => Ok(views.html.Backoffice.newOrUpdateCFPDates(CFPDates.cfpDatesForm))
      }
  }

  def saveCFPDates() = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      play.Logger.warn("Saving new/updated CFP Dates")
      CFPDates.cfpDatesForm.bindFromRequest.fold(
        hasErrors => {
          play.Logger.warn(s"Bad Request due to ${hasErrors.errorsAsJson}")
          BadRequest(views.html.Backoffice.newOrUpdateCFPDates(hasErrors))
        },
        cfpDatesData => {
          play.Logger.info(Messages("cfp.dates.saved"))
          CFPDates.save(cfpDatesData)
          Redirect(routes.Backoffice.homeBackoffice()).flashing("success" -> Messages("cfp.dates.saved"))
        }
      )
  }

  def isCFPOpen(): Boolean = {
    CFPDates.load() match {
      case Some(savedCFPDates) => savedCFPDates.toggleCFPAcceptance == "Y"
      case None => false
    }
  }

  def cfpOpeningDate(): DateTime = {
     DateTime.parse(
        CFPDates.load() match {
          case Some(savedCFPDates) => savedCFPDates.opening ++ "T00:00:00+01:00"
          case None => "2017-11-08T00:00:00+01:00"
        }
     )
  }

  def cfpClosingDate(): DateTime = {
    DateTime.parse(
        CFPDates.load() match {
          case Some(savedCFPDates) => savedCFPDates.closing ++ "T23:59:59+01:00"
          case None => "2018-01-09T23:59:59+01:00"
        }
    )
  }

  def scheduleAnnouncementDate(): DateTime = {
    DateTime.parse(
        CFPDates.load() match {
          case Some(savedCFPDates) => savedCFPDates.scheduleAnnouncement ++ "T00:00:00+01:00"
          case None => "2018-01-23T00:00:00+01:00"
        }
    )
  }

  def pushNotifications() = SecuredAction(IsMemberOf("admin")) {
    implicit request =>

      request.body.asJson.map {
        json =>
          val message = json.\("stringField").as[String]

          ZapActor.actor ! NotifyMobileApps(message)

          Ok(message)
      }.getOrElse {
        BadRequest("{\"status\":\"expecting json data\"}").as("application/json")
      }
  }

  def getProposalsByTags = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val allProposalsByTags = Tags.allProposals()

      Ok(views.html.Backoffice.showAllProposalsByTags(allProposalsByTags))
  }

  def getCloudTag = SecuredAction(IsMemberOf("admin")) {
    implicit request =>
      val termCounts = Tags.countProposalTags()
      if (termCounts.nonEmpty) {
        Ok(views.html.CallForPaper.cloudTags(termCounts))
      } else {
        NotFound("No proposal tags found")
      }
  }
  
  def showDigests = SecuredAction(IsMemberOf("admin")) {
    implicit request =>

      // TODO Can this be condensed in Scala ?  (Stephan)

      val realTimeDigests = Digest.pendingProposals(Digest.REAL_TIME)
      val dailyDigests = Digest.pendingProposals(Digest.DAILY)
      val weeklyDigests = Digest.pendingProposals(Digest.WEEKLY)

      val realTime = realTimeDigests.map {
        case (key: String, value: String) =>
          (Proposal.findById(key).get, value)
      }

      val daily = dailyDigests.map {
        case (key: String, value: String) =>
          (Proposal.findById(key).get, value)
      }

      val weekly = weeklyDigests.map {
        case (key: String, value: String) =>
          (Proposal.findById(key).get, value)
      }

      Ok(views.html.Backoffice.showDigests(realTime, daily, weekly))
  }
}
