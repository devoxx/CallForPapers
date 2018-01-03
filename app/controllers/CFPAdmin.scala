package controllers


import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import java.io._
import javax.swing.text.html.HTML

import play.api.mvc.{SimpleResult, _}
import controllers.CFPAdmin.Redirect
import java.io.{File, FileInputStream, FileOutputStream}

import controllers.Authentication.{BadRequest, Ok, Redirect}
import controllers.Backoffice.{NotFound, Redirect}
import controllers.CallForPaper.{Ok, Redirect}
import sun.misc.{BASE64Decoder, BASE64Encoder}
import library.search.ElasticSearch
import library.{ComputeVotesAndScore, DoCreateTalkAfterCfp, SendMessageInternal, SendMessageToSpeaker, SendScheduledFavorites, ZapActor, _}
import models.Review._
import models._
import notifiers.TransactionalEmails
import models.Speaker._
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTimeZone
import play.api.Play
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import play.api.libs.Crypto
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Action._

import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Cookie

/**
  * The backoffice controller for the CFP technical committee.
  *
  * Author: @nmartignole
  * Created: 11/11/2013 09:09 in Thalys, heading to Devoxx2013
  */
object CFPAdmin extends SecureCFPController {

  private val securityGroups = List("cfp", "adminVis" , "admin")

  val messageForm: Form[String] = Form("msg" -> nonEmptyText(maxLength = 1000))
  val voteForm: Form[Int] = Form("vote" -> number(min = 0, max = 10))
  val editSpeakerForm = Form(
    tuple(
      "uuid" -> text.verifying(nonEmpty, maxLength(50)),
      "firstName" -> text.verifying(nonEmpty, maxLength(30)),
      "lastName" -> text.verifying(nonEmpty, maxLength(30))
    )
  )
  val newWebuserForm: Form[Webuser] = Form(
    mapping(
      "email" -> (email verifying nonEmpty),
      "firstName" -> nonEmptyText(maxLength = 50),
      "lastName" -> nonEmptyText(maxLength = 50)

    )(Webuser.createSpeaker)(Webuser.unapplyForm))

  val speakerForm = play.api.data.Form(mapping(
    "uuid" -> optional(text),
    "email" -> (email verifying nonEmpty),
    "lastName" -> text,
    "bio2" -> nonEmptyText(maxLength = 1200),
    "lang2" -> optional(text),
    "twitter2" -> optional(text) ,
    "avatarUrl2" -> optional(text),
    "picture2" -> optional (text) ,
    "company2" -> optional(text),
    "blog2" -> optional(text),
    "firstName" -> text,
    "acceptTermsConditions" -> boolean,
    "qualifications2" -> nonEmptyText(maxLength = 750),
    "phoneNumber2" -> optional(text),
    "questionAndAnswers2" -> optional(seq(
      mapping(
        "question" -> optional(text),
        "answer" -> optional(text)
      )(QuestionAndAnswer.apply)(QuestionAndAnswer.unapply))
    )
  )(Speaker.createOrEditSpeaker)(Speaker.unapplyFormEdit))

  def index(page: Int,
            sort: Option[String],
            ascdesc: Option[String],
            track: Option[String]) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      val sorter = proposalSorter(sort)
      val orderer = proposalOrder(ascdesc)
      val allNotReviewed = Review.allProposalsNotReviewed(uuid)

      val totalReviewed=Review.totalNumberOfReviewedProposals(uuid)
      val totalVoted = Review.totalProposalsVotedForUser(uuid)

      val maybeFilteredProposals = track match {
        case None => allNotReviewed
        case Some(trackLabel) => allNotReviewed.filter(_.track.id.equalsIgnoreCase(StringUtils.trimToEmpty(trackLabel)))
      }
      val allProposalsForReview = sortProposals(maybeFilteredProposals, sorter, orderer)
      val twentyEvents = Event.loadEvents(20, page)

      val etag = allProposalsForReview.hashCode() + "_" + twentyEvents.hashCode()

      track.map {
        trackValue: String =>
          Ok(views.html.CFPAdmin.cfpAdminIndex(twentyEvents, allProposalsForReview, Event.totalEvents(), page, sort, ascdesc, Some(trackValue), totalReviewed, totalVoted))
            .withHeaders("ETag" -> etag)
      }.getOrElse {
        Ok(views.html.CFPAdmin.cfpAdminIndex(twentyEvents, allProposalsForReview, Event.totalEvents(), page, sort, ascdesc, None, totalReviewed, totalVoted))
          .withHeaders("ETag" -> etag)
      }

  }
  def seeEvents (page: Int,
                 sort: Option[String],
                 ascdesc: Option[String],
                 track: Option[String])=SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      val sorter = proposalSorter(sort)
      val orderer = proposalOrder(ascdesc)
      val allNotReviewed = Review.allProposalsNotReviewed(uuid)

      val totalReviewed=Review.totalNumberOfReviewedProposals(uuid)
      val totalVoted = Review.totalProposalsVotedForUser(uuid)

      val maybeFilteredProposals = track.map {
        trackValue: String =>
          allNotReviewed.filter(_.track.id.equalsIgnoreCase(StringUtils.trimToEmpty(trackValue)))
      }.getOrElse(allNotReviewed)
      val allProposalsForReview = sortProposals(maybeFilteredProposals, sorter, orderer)

      val twentyEvents = Event.loadEvents(20, page)

      val etag = allProposalsForReview.hashCode() + "_" + twentyEvents.hashCode()
      track.map {
        trackValue: String =>
          Ok(views.html.CFPAdmin.events(twentyEvents, Event.totalEvents(), page, sort, ascdesc, Some(trackValue)))
      }.getOrElse { Ok(views.html.CFPAdmin.events(twentyEvents, Event.totalEvents(), page, sort, ascdesc, None))

      }
  }

  def sortProposals(ps: List[Proposal], sorter: Option[Proposal => String], orderer: Ordering[String]) =
    sorter match {
      case None => ps
      case Some(s) => ps.sortBy(s)(orderer)
    }

  def proposalSorter(sort: Option[String]): Option[Proposal => String] = {
    sort match {
      case Some("title") => Some(_.title)
      case Some("mainSpeaker") => Some(_.mainSpeaker)
      case Some("track") => Some(_.track.label)
      case Some("talkType") => Some(_.talkType.label)
      case _ => None
    }
  }

  def proposalOrder(ascdesc: Option[String]) = ascdesc match {
    case Some("desc") => Ordering[String].reverse
    case _ => Ordering[String]
  }

  def openForReview(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      Proposal.findById(proposalId) match {
        case Some(proposal) =>
          val speakerDiscussion = Comment.allSpeakerComments(proposal.id)
          val internalDiscussion = Comment.allInternalComments(proposal.id)
          val maybeMyVote = Review.lastVoteByUserForOneProposal(uuid, proposalId)
          val proposalsByAuths = allProposalByProposal(proposal)
          Ok(views.html.CFPAdmin.showProposal(proposal, proposalsByAuths, speakerDiscussion, internalDiscussion, messageForm, messageForm, voteForm, maybeMyVote, uuid))
        case None => NotFound("Proposal not found").as("text/html")
      }
  }

  def allProposalByProposal(proposal: Proposal): Map[String, Map[String, models.Proposal]] = {
    val authorIds: List[String] = proposal.mainSpeaker :: proposal.secondarySpeaker.toList ::: proposal.otherSpeakers
    authorIds.map {
      case id => id -> Proposal.allProposalsByAuthor(id)
    }.toMap

  }

  def showVotesForProposal(proposalId: String) = SecuredAction(IsMemberOf("cfp")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      import scala.concurrent.ExecutionContext.Implicits.global
      val uuid = request.webuser.uuid
      scala.concurrent.Future {
        Proposal.findById(proposalId) match {
          case Some(proposal) =>
            val currentAverageScore = Review.averageScore(proposalId)
            val countVotesCast = Review.totalVoteCastFor(proposalId)
            // votes exprimes (sans les votes a zero)
            val countVotes = Review.totalVoteFor(proposalId)
            val allVotes = Review.allVotesFor(proposalId)

            // The next proposal I should review
            val allNotReviewed = Review.allProposalsNotReviewed(uuid)
            val (sameTracks, otherTracks) = allNotReviewed.partition(_.track.id == proposal.track.id)
            val (sameTalkType, otherTalksType) = allNotReviewed.partition(_.talkType.id == proposal.talkType.id)

            val nextToBeReviewedSameTrack = (sameTracks.sortBy(_.talkType.id) ++ otherTracks).headOption
            val nextToBeReviewedSameFormat = (sameTalkType.sortBy(_.track.id) ++ otherTalksType).headOption

            // If Golden Ticket is active
            if (ConferenceDescriptor.isGoldenTicketActive) {
              val averageScoreGT = ReviewByGoldenTicket.averageScore(proposalId)
              val countVotesCastGT: Option[Long] = Option(ReviewByGoldenTicket.totalVoteCastFor(proposalId))
              Ok(views.html.CFPAdmin.showVotesForProposal(uuid, proposal, currentAverageScore, countVotesCast, countVotes, allVotes, nextToBeReviewedSameTrack, nextToBeReviewedSameFormat, averageScoreGT, countVotesCastGT))
            } else {
              Ok(views.html.CFPAdmin.showVotesForProposal(uuid, proposal, currentAverageScore, countVotesCast, countVotes, allVotes, nextToBeReviewedSameTrack, nextToBeReviewedSameFormat, 0, None))
            }
          case None => NotFound("Proposal not found").as("text/html")
        }
      }
  }

  def sendMessageToSpeaker(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      Proposal.findById(proposalId) match {
        case Some(proposal) =>
          messageForm.bindFromRequest.fold(
            hasErrors => {
              val speakerDiscussion = Comment.allSpeakerComments(proposal.id)
              val internalDiscussion = Comment.allInternalComments(proposal.id)
              val maybeMyVote = Review.lastVoteByUserForOneProposal(uuid, proposalId)
              val proposals = allProposalByProposal(proposal)
              BadRequest(views.html.CFPAdmin.showProposal(proposal, proposals, speakerDiscussion, internalDiscussion, hasErrors, messageForm, voteForm, maybeMyVote, uuid))
            },
            validMsg => {
              Comment.saveCommentForSpeaker(proposal.id, uuid, validMsg) // Save here so that it appears immediatly
              ZapActor.actor ! SendMessageToSpeaker(uuid, proposal, validMsg)
              Redirect(routes.CFPAdmin.openForReview(proposalId)).flashing("success" -> "Message sent to speaker.")
            }
          )
        case None => NotFound("Proposal not found").as("text/html")
      }
  }

  // Post an internal message that is visible only for program committee
  def postInternalMessage(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      Proposal.findById(proposalId) match {
        case Some(proposal) =>
          messageForm.bindFromRequest.fold(
            hasErrors => {
              val speakerDiscussion = Comment.allSpeakerComments(proposal.id)
              val internalDiscussion = Comment.allInternalComments(proposal.id)
              val maybeMyVote = Review.lastVoteByUserForOneProposal(uuid, proposalId)
              val proposals = allProposalByProposal(proposal)
              BadRequest(views.html.CFPAdmin.showProposal(proposal, proposals, speakerDiscussion, internalDiscussion, messageForm, hasErrors, voteForm, maybeMyVote, uuid))
            },
            validMsg => {
              Comment.saveInternalComment(proposal.id, uuid, validMsg) // Save here so that it appears immediatly
              ZapActor.actor ! SendMessageInternal(uuid, proposal, validMsg)
              Redirect(routes.CFPAdmin.openForReview(proposalId)).flashing("success" -> "Message sent to program committee.")
            }
          )
        case None => NotFound("Proposal not found").as("text/html")
      }
  }

  def voteForProposal(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      Proposal.findById(proposalId) match {
        case Some(proposal) =>
          voteForm.bindFromRequest.fold(
            hasErrors => {
              val speakerDiscussion = Comment.allSpeakerComments(proposal.id)
              val internalDiscussion = Comment.allInternalComments(proposal.id)
              val maybeMyVote = Review.lastVoteByUserForOneProposal(uuid, proposalId)
              val proposals = allProposalByProposal(proposal)
              BadRequest(views.html.CFPAdmin.showProposal(proposal, proposals, speakerDiscussion, internalDiscussion, messageForm, messageForm, hasErrors, maybeMyVote, uuid))
            },
            validVote => {
              Review.voteForProposal(proposalId, uuid, validVote)
              Redirect(routes.CFPAdmin.showVotesForProposal(proposalId)).flashing("vote" -> "Ok, vote submitted")
            }
          )
        case None => NotFound("Proposal not found").as("text/html")
      }
  }

  def clearVoteForProposal(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      Proposal.findById(proposalId) match {
        case Some(proposal) =>
          Review.removeVoteForProposal(proposalId, uuid)
          Redirect(routes.CFPAdmin.showVotesForProposal(proposalId)).flashing("vote" -> "Removed your vote")
        case None => NotFound("Proposal not found").as("text/html")
      }
  }

  def allMyVotes(talkType: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      ConferenceDescriptor.ConferenceProposalTypes.ALL.find(_.id == talkType).map {
        pType =>
          val uuid = request.webuser.uuid
          val allMyVotes = Review.allVotesFromUser(uuid)
          val allProposalIDs = allMyVotes.map(_._1)
          val allProposalsForProposalType = Proposal.loadAndParseProposals(allProposalIDs).filter(_._2.talkType == pType)
          val allProposalsIdsProposalType = allProposalsForProposalType.keySet

          val allMyVotesForSpecificProposalType = allMyVotes.filter {
            proposalIdAndVotes => allProposalsIdsProposalType.contains(proposalIdAndVotes._1)
          }

          val allScoresForProposals: Map[String, Double] = allProposalsIdsProposalType.map {
            pid: String => (pid, Review.averageScore(pid))
          }.toMap

          val sortedListOfProposals = allMyVotesForSpecificProposalType.toList.sortBy {
            case (proposalID, maybeScore) =>
              maybeScore.getOrElse(0.toDouble)
          }.reverse

          Ok(views.html.CFPAdmin.allMyVotes(sortedListOfProposals, allProposalsForProposalType, talkType, allScoresForProposals))
      }.getOrElse {
        BadRequest("Invalid proposal type")
      }
  }

  def advancedSearch(q: Option[String] = None, p: Option[Int] = None) = SecuredAction(IsMemberOf("cfp")).async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      import play.api.libs.concurrent.Execution.Implicits.defaultContext

      ElasticSearch.doAdvancedSearch("speakers,proposals", q, p).map {
        case r if r.isSuccess =>
          val json = Json.parse(r.get)
          val total = (json \ "hits" \ "total").as[Int]
          val hitContents = (json \ "hits" \ "hits").as[List[JsObject]]

          val results = hitContents.sortBy {
            jsvalue =>
              val index = (jsvalue \ "_index").as[String]
              index
          }.map {
            jsvalue =>
              val index = (jsvalue \ "_index").as[String]
              val source = jsvalue \ "_source"
              index match {
                case "proposals" =>
                  val id = (source \ "id").as[String]
                  val title = (source \ "title").as[String]
                  val talkType = Messages((source \ "talkType" \ "id").as[String])
                  val code = (source \ "state" \ "code").as[String]
                  val mainSpeaker = (source \ "mainSpeaker").as[String]
                  s"<p class='searchProposalResult'><i class='icon-folder-open'></i> Proposal <a href='${routes.CFPAdmin.openForReview(id)}'>$title</a> <strong>$code</strong> - by $mainSpeaker - $talkType</p>"
                case "speakers" =>
                  val uuid = (source \ "uuid").as[String]
                  val name = (source \ "name").as[String]
                  val firstName = (source \ "firstName").as[String]
                  s"<p class='searchSpeakerResult'><i class='icon-user'></i> Speaker <a href='${routes.CFPAdmin.showSpeakerAndTalks(uuid)}'>$firstName $name</a></p>"
                case other => "Unknown format " + index
              }
          }

          Ok(views.html.CFPAdmin.renderSearchResult(total, results, q, p)).as("text/html")
        case r if r.isFailure =>
          InternalServerError(r.get)
      }
  }

  def allSponsorTalks = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val proposals = Proposal.allSponsorsTalk()
      Ok(views.html.CFPAdmin.allSponsorTalks(proposals))
  }

  def showSpeakerAndTalks(uuidSpeaker: String) = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      Speaker.findByUUID(uuidSpeaker) match {
        case Some(speaker) =>
          val proposals = Proposal.allProposalsByAuthor(speaker.uuid)
          Ok(views.html.CFPAdmin.showSpeakerAndTalks(speaker, proposals, request.webuser.uuid))
        case None => NotFound("Speaker not found")
      }
  }

  def allVotes(confType: String, track: Option[String]) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val reviews: Map[String, (Score, TotalVoter, TotalAbst, AverageNote, StandardDev)] = Review.allVotes()
      val totalApproved = ApprovedProposal.countApproved(confType)

      val allProposals = Proposal.loadAndParseProposals(reviews.keySet)

      val listOfProposals = reviews.flatMap {
        case (proposalId, scoreAndVotes) =>
          val maybeProposal = allProposals.get(proposalId)
          maybeProposal match {
            case None => play.Logger.of("CFPAdmin").error(s"Unable to load proposal id $proposalId")
              None
            case Some(p) =>
              val goldenTicketScore: Double = ReviewByGoldenTicket.averageScore(p.id)
              val gtVoteCast: Long = ReviewByGoldenTicket.totalVoteCastFor(p.id)
              Option(p, scoreAndVotes, goldenTicketScore, gtVoteCast)
          }
      }

      val tempListToDisplay = confType match {
        case "all" => listOfProposals
        case filterType => listOfProposals.filter(_._1.talkType.id == filterType)
      }
      val listToDisplay = track match {
        case None => tempListToDisplay
        case Some(trackId) => tempListToDisplay.filter(_._1.track.id == trackId)
      }

      val totalRemaining = ApprovedProposal.remainingSlots(confType)
      Ok(views.html.CFPAdmin.allVotes(listToDisplay.toList, totalApproved, totalRemaining, confType))
  }

  def allEagerSpeakers() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      def proposalsBySpeakers: List[(String, Int)] =
        Speaker.allSpeakers()
          .map(speaker => (speaker.uuid, Proposal.allMyDraftAndSubmittedProposals(speaker.uuid).size))
          .filter(_._2 > 0)

      Ok(views.html.CFPAdmin.allEagerSpeakers(proposalsBySpeakers))
  }

  def doComputeVotesTotal() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      ZapActor.actor ! ComputeVotesAndScore()
      Redirect(routes.CFPAdmin.allVotes("conf", None)).flashing("success" -> "Recomputing votes and scores...")
  }

  def removeSponsorTalkFlag(proposalId: String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      Proposal.removeSponsorTalkFlag(uuid, proposalId)
      Redirect(routes.CFPAdmin.allSponsorTalks()).flashing("success" -> s"Removed sponsor talk on $proposalId")
  }

  def allProposalsByTrack(track: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val proposals = Proposal.allSubmitted().filter(_.track.id == track)
      Ok(views.html.CFPAdmin.allProposalsByTrack(proposals, track))
  }

  def allProposalsByType(confType: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val proposals = Proposal.allSubmitted().filter(_.talkType.id == confType)
      Ok(views.html.CFPAdmin.allProposalsByType(proposals, confType))
  }

  def showProposalsNotReviewedCompareTo(maybeReviewer: Option[String]) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val uuid = request.webuser.uuid
      maybeReviewer match {
        case None =>
          Ok(views.html.CFPAdmin.selectAnotherWebuser(Webuser.allCFPWebusers()))
        case Some(otherReviewer) =>
          val diffProposalIDs = Review.diffReviewBetween(otherReviewer, uuid)
          Ok(views.html.CFPAdmin.showProposalsNotReviewedCompareTo(diffProposalIDs, otherReviewer))
      }
  }

  // Returns all speakers
  def allSpeakersExport() = SecuredAction(IsMemberOf("admin")) {

    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val allSpeakers = Speaker.allSpeakers();

      val dir = new File("./public/speakers")
      FileUtils.forceMkdir(dir)

      val conferenceNameSpaces = Messages("CONF.title").replaceAll(" ", "")
      val file = new File(dir, s"speakers${conferenceNameSpaces}.csv")

      val writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), true)

      allSpeakers.sortBy(_.email).foreach {
        s =>

          val proposals: List[Proposal] = Proposal.allAcceptedForSpeaker(s.uuid)

          if (proposals.nonEmpty) {

            writer.print(s.email.toLowerCase)
            writer.print(",")
            writer.print(s.cleanTwitter.getOrElse("").toLowerCase)
            writer.print(",")
            writer.print(s.firstName.getOrElse("?").capitalize)
            writer.print(",")
            writer.print(s.name.getOrElse("?").capitalize)
            writer.print(",")

            proposals.foreach { p =>
              val proposalUrl = "http://" + ConferenceDescriptor.current().conferenceUrls.cfpHostname +
                routes.Publisher.showDetailsForProposal(p.id, p.escapedTitle)

              ScheduleConfiguration.findSlotForConfType(p.talkType.id, p.id).map { slot =>
                writer.print(Messages(p.talkType.id))
                writer.print(": \"" + p.title.replaceAll(",", " ") + "\"")
                writer.print("\", ")
                writer.print("\"" + proposalUrl + "\", ")
                writer.print(s" scheduled on ${slot.day.capitalize} ${slot.room.name} ")
                writer.print(s"from ${slot.from.toDateTime(DateTimeZone.forID(ConferenceDescriptor.timeZone)).toString("HH:mm")} to ${slot.to.toDateTime(DateTimeZone.forID(ConferenceDescriptor.timeZone)).toString("HH:mm")}")
              }.getOrElse {
                writer.print("\"")
                writer.print(p.title.replaceAll(",", " "))
                writer.print("\", ")
                writer.print("\"" + proposalUrl + "\", ")
                writer.print(s" ${Messages(p.talkType.label)} not yet scheduled")
              }

              writer.print(",")
            }

            writer.println()
          }
      }
      writer.close()
      Ok.sendFile(file, inline = false)
  }

  def allSpeakers() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.CFPAdmin.allSpeakersHome())
  }

  def duplicateSpeakers() = SecuredAction(IsMemberOf("cfp")) {
    var uniqueSpeakers = scala.collection.mutable.Set[Speaker]()

    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val allApprovedSpeakers = ApprovedProposal.allApprovedSpeakers()
      val allRefusedSpeakers = ApprovedProposal.allRefusedSpeakers()
      val allSpeakers = allApprovedSpeakers ++ allRefusedSpeakers

      val speakersSortedByUUID = allSpeakers.toList
        .sortBy(_.uuid)
        .groupBy(_.uuid)
        .filter(_._2.size == 1)
        .flatMap { uuid => uuid._2}

      val uniqueSpeakersSortedByName = speakersSortedByUUID.toList
        .sortBy(_.cleanName)
        .groupBy(_.cleanName)
        .filter(_._2.size != 1)
        .flatMap { name => name._2}

      val speakersSortedByEmail = allSpeakers.toList
        .sortBy(_.email)
        .groupBy(_.email)
        .filter(_._2.size != 1)
        .flatMap { email => email._2}

      val uniqueSpeakersSortedByEmail = speakersSortedByEmail.toList
        .sortBy(_.email)
        .groupBy(_.email)
        .filter(_._2.size != 1)
        .flatMap { email => email._2}

      val combinedList = uniqueSpeakersSortedByName.toList ++ uniqueSpeakersSortedByEmail.toList

      Ok(views.html.CFPAdmin.duplicateSpeakers(combinedList))
  }

  def allDevoxxians() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = Webuser.allSpeakers
      Ok(views.html.Backoffice.allDevoxxians(speakers))
  }

  def allSpeakersWithApprovedTalks() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val allSpeakers = ApprovedProposal.allApprovedSpeakers()
      Ok(views.html.CFPAdmin.allSpeakers(allSpeakers.toList.sortBy(_.cleanName)))
  }

  def allApprovedSpeakersByCompany(showQuickiesAndBof: Boolean) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = ApprovedProposal.allApprovedSpeakers()
        .groupBy(_.company.map(_.toLowerCase.trim).getOrElse("Pas de société"))
        .toList
        .sortBy(_._2.size)
        .reverse

      val proposals = speakers.map {
        case (company, subSpeakers) =>
          val setOfProposals = subSpeakers.toList.flatMap {
            s =>
              Proposal.allApprovedProposalsByAuthor(s.uuid).values
          }.toSet.filterNot { p: Proposal =>
            if (showQuickiesAndBof) {
              p == null
            } else {
              p.talkType == ConferenceDescriptor.ConferenceProposalTypes.BOF ||
                p.talkType == ConferenceDescriptor.ConferenceProposalTypes.QUICK
            }
          }
          (company, setOfProposals)
      }

      Ok(views.html.CFPAdmin.allApprovedSpeakersByCompany(speakers, proposals))
  }

  // All speakers that accepted to present a talk (including BOF and Quickies)
  def allSpeakersThatForgetToAccept() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = ApprovedProposal.allApprovedSpeakers()

      val proposals: Set[(Speaker, Iterable[Proposal])] = speakers.map {
        speaker =>
          (speaker, Proposal.allThatForgetToAccept(speaker.uuid).values)
      }.filter(_._2.nonEmpty)

      Ok(views.html.CFPAdmin.allSpeakersThatForgetToAccept(proposals))
  }

  // All speakers with a speaker's badge (it does not include Quickies, BOF and 3rd, 4th speakers)
  def allSpeakersWithAcceptedTalksAndBadge() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = ApprovedProposal.allApprovedSpeakers()
      val proposals: List[(Speaker, Iterable[Proposal])] = speakers.toList.map {
        speaker =>
          val allProposalsForThisSpeaker = Proposal.allApprovedAndAcceptedProposalsByAuthor(speaker.uuid).values
          val onIfFirstOrSecondSpeaker = allProposalsForThisSpeaker.filter(p => p.mainSpeaker == speaker.uuid || p.secondarySpeaker == Some(speaker.uuid))
            .filter(p => ProposalConfiguration.doesProposalTypeGiveSpeakerFreeEntrance(p.talkType))
          (speaker, onIfFirstOrSecondSpeaker)
      }.filter(_._2.nonEmpty)

      Ok(views.html.CFPAdmin.allSpeakersWithAcceptedTalksAndBadge(proposals))
  }

  // All speakers with a speaker's badge
  def allSpeakersWithAcceptedTalks() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = ApprovedProposal.allApprovedSpeakers()
      val proposals: List[(Speaker, Iterable[Proposal])] = speakers.toList.map {
        speaker =>
          val allProposalsForThisSpeaker = Proposal.allApprovedAndAcceptedProposalsByAuthor(speaker.uuid).values
          val onIfFirstOrSecondSpeaker = allProposalsForThisSpeaker.filter(p => p.mainSpeaker == speaker.uuid || p.secondarySpeaker == Some(speaker.uuid))
          (speaker, onIfFirstOrSecondSpeaker)
      }.filter(_._2.nonEmpty)

      Ok(views.html.CFPAdmin.allSpeakersWithAcceptedTalksAndBadge(proposals))
  }

  def allSpeakersWithRejectedTalks() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val refusedSpeakers = ApprovedProposal.allRefusedSpeakerIDs()
      val approvedSpeakers = ApprovedProposal.allApprovedSpeakerIDs()

      val diffRejectedSpeakers: Set[String] = refusedSpeakers.diff(approvedSpeakers)

      val proposals: List[(Speaker, Iterable[Proposal])] = diffRejectedSpeakers.toList.map {
        speakerId =>
          val allProposalsForThisSpeaker = Proposal.allRejectedForSpeaker(speakerId)
          val onIfFirstOrSecondSpeaker = allProposalsForThisSpeaker.filter(p => p.mainSpeaker == speakerId || p.secondarySpeaker == Some(speakerId))
          (Speaker.findByUUID(speakerId).get, onIfFirstOrSecondSpeaker)
      }.filter(_._2.nonEmpty)

     Ok(views.html.CFPAdmin.allSpeakersWithRejectedProposals(proposals))
  }

  import play.api.data.Form
  import play.api.data.Forms._

  def allSpeakersWithAcceptedTalksForExport() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = ApprovedProposal.allApprovedSpeakers()
      val proposals: List[(Speaker, Iterable[Proposal])] = speakers.toList.map {
        speaker =>
          val allProposalsForThisSpeaker = Proposal.allApprovedAndAcceptedProposalsByAuthor(speaker.uuid).values
          val onIfFirstOrSecondSpeaker = allProposalsForThisSpeaker.filter(p => p.mainSpeaker == speaker.uuid || p.secondarySpeaker == Some(speaker.uuid))
          (speaker, onIfFirstOrSecondSpeaker)
      }.filter(_._2.nonEmpty)

      Ok(views.html.CFPAdmin.allSpeakersWithAcceptedTalksForExport(proposals))
  }

  def allSpeakersWhoHaveAnsweredQandA() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val allApprovedSpeakers = ApprovedProposal.allApprovedSpeakers().toList
      val filteredSpeakers = allApprovedSpeakers
                              .filter(
                                speaker => speaker.questionsArePresentAndSpeakerHasAnsweredAtLeastOneQuestion
                              )

    Ok(views.html.CFPAdmin.allSpeakersWhoHaveAnsweredQandA(filteredSpeakers))
  }

  def allSpeakersWhoHaveNotAnsweredQandA() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val allApprovedSpeakers = ApprovedProposal.allApprovedSpeakers().toList
      val filteredSpeakers = allApprovedSpeakers
                              .filter(
                                speaker => ! speaker.questionsArePresentAndSpeakerHasAnsweredAtLeastOneQuestion
                              )

    Ok(views.html.CFPAdmin.allSpeakersWhoHaveNotAnsweredQandA(filteredSpeakers))
  }

  def allUsersWithIncompleteSpeakerProfile() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val mapOfAllWebusers = Webuser.allWebusers
      val filteredWebusersWithNoSpeakerProfiles = mapOfAllWebusers.filter(
          webuserKeyValuePair =>
            Speaker.findByUUID(webuserKeyValuePair._2.get.uuid).isEmpty ||
            Speaker.findByUUID(webuserKeyValuePair._2.get.uuid).get == null
      ).values
        .toList
        .flatten
        .sortBy(_.cleanName)

    Ok(views.html.CFPAdmin.allUsersWithIncompleteSpeakerProfile(filteredWebusersWithNoSpeakerProfiles))
  }

  def allWebusers() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val allSpeakers = Webuser.allSpeakers.sortBy(_.cleanName)
      Ok(views.html.CFPAdmin.allWebusers(allSpeakers , newWebuserForm ))
  }
  def sendEmailForTalk(uuid:String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Webuser.findByUUID(uuid).map { wb: Webuser =>
        // Webuser.activeVip(wb , true)
        ZapActor.actor ! DoCreateTalkAfterCfp(wb)
        Event.storeEvent(Event(Webuser.findByUUID(uuid).get.email, request.webuser.uuid, "invited speaker [" + Webuser.findByUUID(uuid).get.cleanName + "]"))
        Redirect(routes.CFPAdmin.allWebusers()).flashing("success" -> s"Invitation sent for ${wb.email}")
      }.getOrElse(NotFound("userNotfound"))


  }
  def disablecreatetalk(uuid:String) = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Webuser.findByUUID(uuid).map { wb: Webuser =>

        // Webuser.activeVip(wb , false)

      }
      Redirect(routes.CFPAdmin.allWebusers()).flashing("success" -> s"Blocking access to create talk for ${Webuser.findByUUID(uuid).get.email}")

  }
  val newVisitorForm: Form[Webuser] = Form(
    mapping(
      "email" -> (email verifying nonEmpty),
      "firstName" -> optional(text(maxLength = 50)),
      "lastName" -> optional(text(maxLength = 50)),
      "regId" -> optional(text),
      "tel"-> optional(text),
      "pictureurl"-> optional(text)
    )(Webuser.createVisitor)(Webuser.unapplyFormVisitor))
  def allVisitiors() = SecuredAction(IsMemberOfGroups(securityGroups)) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val allvisitors = Webuser.allVisitors.sortBy(_.cleanName)
      Ok(views.html.CFPAdmin.allVisitors(allvisitors, newVisitorForm))
  }
  def  favoriteTalkByVisitor(id :String): Unit = {

  }
  def saveNewVisitoByAdmin = SecuredAction((IsMemberOfGroups(securityGroups))){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      newVisitorForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.Authentication.prepareSignupVisitor(invalidForm)),
        validForm => {
          if (!Webuser.isEmailRegistered(validForm.email)) {

            Webuser.saveAndValidateWebuser(validForm)
            TransactionalEmails.sendWeCreatedAnAccountForYou(validForm.email , validForm.firstName , validForm.password)

            //TransactionalEmails.sendValidateYourEmail(validForm.email, routes.Authentication.validateYourEmailForVisitor(Crypto.sign(validForm.email.toLowerCase.trim), new String(Base64.encodeBase64(validForm.email.toLowerCase.trim.getBytes("UTF-8")), "UTF-8")).absoluteURL())
            Redirect(routes.CFPAdmin.allVisitiors()).flashing("success" -> "Visitor created")
          }
          else {
            Redirect(routes.CFPAdmin.allVisitiors()).flashing("error" -> Messages("speakerExist"))

          }
        }
      )
  }

  def validateYourEmailForVisitorByAdmin(t: String, a: String) = SecuredAction(IsMemberOfGroups(securityGroups)) {
    implicit request:SecuredRequest[play.api.mvc.AnyContent] =>
      val email = new String(Base64.decodeBase64(a), "UTF-8")
      if (Crypto.sign(email) == t) {
        val futureMaybeWebuser = Webuser.findNewUserByEmail(email)
        futureMaybeWebuser.map {
          webuser =>
            val newUUID = Webuser.saveAndValidateWebuser(webuser) // it is generated
            TransactionalEmails.sendAccessCode(webuser.email, webuser.password)

            Redirect(routes.CFPAdmin.allVisitiors()).flashing("success" -> "Visitor created")
          //Redirect(routes.Favorites.welcomeVisitor()).withSession("uuid" -> newUUID).withCookies(cookie).flashing("success" -> ("Your account has been validated. Your new access code is " + webuser.password + " (case-sensitive)")).withSession("uuid" -> webuser.uuid)
        }.getOrElse {
          Redirect(routes.Application.index()).flashing("error" -> "Sorry, your invitation has expired.")
        }
      } else {
        Redirect(routes.Application.index()).flashing("error" -> "Sorry, we could not validate your authentication token. Are you sure that this email is registered?")
      }
  }

  def deleteVisitor(id:String) = SecuredAction(IsMemberOfGroups(securityGroups)) {
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      val vis = Webuser.findByUUID(id)
      vis  match {
        case None => Ok("ddd")
        case Some (v) => { Webuser.removeFromVisitor(v.uuid)

          Redirect(routes.CFPAdmin.allVisitiors()) }
      }
  }

  def allCFPWebusers() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.CFPAdmin.showCFPUsers(Webuser.allCFPAdminUsers()))
  }

  def updateTrackLeadersAndReviewers() = SecuredAction(IsMemberOf("cfp")) {
    implicit req: SecuredRequest[play.api.mvc.AnyContent] =>

      var leadersPerTrack = req.request.body.asFormUrlEncoded.map {
        perTrack =>
            ConferenceDescriptor.ConferenceTracks.ALL.map {
              track => (track.id, perTrack.get(s"leader-${track.id}").get)
        }
      }.get.toMap

      var reviewersPerTrack = req.request.body.asFormUrlEncoded.map {
        perTrack =>
          ConferenceDescriptor.ConferenceTracks.ALL.map {
            track => (track.id, perTrack.get(s"reviewer-${track.id}").get)
          }
      }.get.toMap

      req.request.body.asFormUrlEncoded.map {
        trackLeadersForm =>
          TrackLeader.updateAllTracks(leadersPerTrack, reviewersPerTrack)
          Redirect(routes.CFPAdmin.allCFPWebusers()).flashing("success" -> "List of track leaders and reviewers updated")
      }.getOrElse {
        Redirect(routes.CFPAdmin.allCFPWebusers()).flashing("error" -> "No value received")
      }
  }


  def newOrEditSpeaker(speakerUUID: Option[String]) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      speakerUUID match {
        case Some(uuid) =>
          Speaker.findByUUID(uuid).map {
            speaker: Speaker =>
              Ok(views.html.CFPAdmin.newSpeaker(speakerForm.fill(speaker))).flashing("success" -> "You are currently editing an existing speaker")
          }.getOrElse {
            Ok(views.html.CFPAdmin.newSpeaker(speakerForm)).flashing("error" -> "Speaker not found")
          }
        case None => Ok(views.html.CFPAdmin.newSpeakerAndTalk(speakerForm))
      }
  }
  def newSpeakerOnly(speakerUUID: Option[String]) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      speakerUUID match {
        case Some(uuid) =>
          Speaker.findByUUID(uuid).map {
            speaker: Speaker =>
              Ok(views.html.CFPAdmin.newSpeakerOnly(speakerForm.fill(speaker))).flashing("success" -> "You are currently editing an existing speaker")
          }.getOrElse {
            Ok(views.html.CFPAdmin.newSpeakerOnly(speakerForm)).flashing("error" -> "Speaker not found")
          }
        case None => Ok(views.html.CFPAdmin.newSpeakerOnly(speakerForm))
      }
  }
  def createNewproposal() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>



      if (request.session.get("validSpeaker").isEmpty) {

        Ok(views.html.CFPAdmin.newSpeakerAndTalk(speakerForm))
      } else {
        Ok(views.html.CFPAdmin.newProposal(Proposal.proposalForm))
      }
  }
  /*def manageMails() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val m = new MailsManager(MailsManager.generateId() , "contenu" , "" ,"")
      MailsManager.save(m)
      Ok("c'est bon")
  }*/

  def previewProposal() = SecuredAction {
    implicit request =>
      Proposal.proposalForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.CFPAdmin.newProposal(hasErrors)).flashing("error" -> "invalid.form"),
        validProposal => {
          val summary = validProposal.summaryAsHtml
          // markdown to HTML
          val privateMessage = validProposal.privateMessageAsHtml // markdown to HTML
          Ok(views.html.CFPAdmin.previewProposal(summary, privateMessage, Proposal.proposalForm.fill(validProposal), request.webuser.uuid))
        }
      )
  }


  def saveProposal() = SecuredAction {
    implicit request =>

      Proposal.proposalForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.CFPAdmin.newProposal(hasErrors)),
        proposal => {
          var uuid = ""

          request.session.get("validSpeaker").map { validSpeaker =>
            val jsValuevalidspeaker: JsValue = Json.parse(validSpeaker)
            Json.fromJson(jsValuevalidspeaker).map {
              speaker =>
                uuid = speaker.uuid
                val newWebuser = Webuser.createSpeaker(speaker.email, speaker.firstName.getOrElse("?"), speaker.name.getOrElse("?") )
                val newUUID = Webuser.saveAndValidateWebuser(newWebuser)

                Speaker.save(speaker)


            }

          }

          // If the editor is not the owner then findProposal returns None
          Proposal.findProposal(uuid, proposal.id) match {
            case Some(existingProposal) => {
              // This is an edit operation
              // First we try to reset the speaker's, we do not take the values from the FORM for security reason
              //val updatedProposal = proposal.copy(mainSpeaker = existingProposal.mainSpeaker, secondarySpeaker = existingProposal.secondarySpeaker, otherSpeakers = existingProposal.otherSpeakers)

              // Then because the editor becomes mainSpeaker, we have to update the secondary and otherSpeaker
              /*if (existingProposal.state == ProposalState.DRAFT || existingProposal.state == ProposalState.SUBMITTED) {
            Proposal.save(uuid, Proposal.setMainSpeaker(updatedProposal, uuid), ProposalState.DRAFT)
            Event.storeEvent(Event(proposal.id, uuid, "Updated proposal " + proposal.id + " with title " + StringUtils.abbreviate(proposal.title, 80)))
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("saved1"))
          } else {
            Proposal.save(uuid, Proposal.setMainSpeaker(updatedProposal, uuid), existingProposal.state)
            Event.storeEvent(Event(proposal.id, uuid, "Edited proposal " + proposal.id + " with current state [" + existingProposal.state.code + "]"))
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("success" -> Messages("saved2"))
          }*/
              Ok("speaker already exist")
            }
            case other => {
              // Check that this is really a new id and that it does not exist
              if (Proposal.isNew(proposal.id)) {
                // This is a "create new" operation

                Proposal.save(uuid, proposal, ProposalState.ACCEPTED)
                Event.storeEvent(Event(proposal.id, uuid, "Created a new proposal " + proposal.id + " with title " + StringUtils.abbreviate(proposal.title, 80)))

                Redirect(routes.Backoffice.changeProposalState(proposal.id, ProposalState.ACCEPTED.code)).withSession(request.session - "validSpeaker")
                Redirect(routes.Backoffice.changeProposalState(proposal.id, ProposalState.ACCEPTED.code)).withSession(request.session - "validSpeaker")


                //Redirect(routes.Backoffice.allProposals()).flashing("success" -> Messages("saved"))
              } else {
                // Maybe someone tried to edit someone's else proposal...
                Event.storeEvent(Event(proposal.id, uuid, "Tried to edit this talk but he is not the owner."))
                Redirect(routes.CallForPaper.homeForSpeaker).flashing("error" -> "You are trying to edit a proposal that is not yours. This event has been logged.")

              }
            }
          }
        }
      )
  }
  def saveProposalWithState(state:String) = SecuredAction {
    implicit request =>

    Proposal.proposalForm.bindFromRequest.fold(
      hasErrors => BadRequest(views.html.CFPAdmin.newProposal(hasErrors)),
      proposal => {
        var uuid = ""

        request.session.get("validSpeaker").map { validSpeaker =>
          val jsValuevalidspeaker: JsValue = Json.parse(validSpeaker)
          Json.fromJson(jsValuevalidspeaker).map {
            speaker =>
              uuid = speaker.uuid
              val newWebuser = Webuser.createSpeaker(speaker.email, speaker.firstName.getOrElse("?"), speaker.name.getOrElse("?") )
              val newUUID = Webuser.saveAndValidateWebuser(newWebuser)

              Speaker.save(speaker)


          }

        }

        // If the editor is not the owner then findProposal returns None
        Proposal.findProposal(uuid, proposal.id) match {
          case Some(existingProposal) => {
            Ok("speaker already exist")
          }
          case other => {
            // Check that this is really a new id and that it does not exist
            if (Proposal.isNew(proposal.id)) {
              // This is a "create new" operation

              Proposal.save(uuid, proposal, ProposalState.DRAFT)
              Proposal.changeProposalState(uuid , proposal.id , ProposalState.apply(state))
              if(state.equals("approved") || state.equals("accepted")){
                notifiers.Mails.sendProposalApproved(Webuser.findByUUID(uuid).get , proposal)
              }

              if(state.equals("submitted")){
                notifiers.Mails.sendNotifyProposalSubmitted(Webuser.findByUUID(uuid).get , proposal)
              }
              Event.storeEvent(Event(proposal.id, uuid, "Created a new proposal " + proposal.id + " with title " + StringUtils.abbreviate(proposal.title, 80)))

              Redirect(routes.CFPAdmin.index()).flashing("success" -> s"speaker & proposal created with $state status")


              //Redirect(routes.Backoffice.allProposals()).flashing("success" -> Messages("saved"))
            } else {
              // Maybe someone tried to edit someone's else proposal...
              Event.storeEvent(Event(proposal.id, uuid, "Tried to edit this talk but he is not the owner."))
              Redirect(routes.CallForPaper.homeForSpeaker).flashing("error" -> "You are trying to edit a proposal that is not yours. This event has been logged.")

            }
          }
        }
      }
    )}






  /* def saveNewSpeaker() = SecuredAction(IsMemberOf("cfp")) {
implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
  speakerForm.bindFromRequest.fold(
    invalidForm => BadRequest(views.html.CFPAdmin.newSpeaker(invalidForm)).flashing("error" -> "Invalid form, please check and correct errors. "),
    validSpeaker => {
      Option(validSpeaker.uuid) match {
        case Some(existingUUID) =>
          play.Logger.of("application.CFPAdmin").debug("Updating existing speaker " + existingUUID)
          Webuser.findByUUID(existingUUID).map {
            existingWebuser =>
              Webuser.updateNames(existingUUID, validSpeaker.firstName.getOrElse("?"), validSpeaker.name.getOrElse("?"))
          }.getOrElse {
            val newWebuser = Webuser.createSpeaker(validSpeaker.email, validSpeaker.firstName.getOrElse("?"), validSpeaker.name.getOrElse("?"))
            val newUUID = Webuser.saveAndValidateWebuser(newWebuser)
            play.Logger.warn("Created missing webuser " + newUUID)
          }
          Speaker.save(validSpeaker)
          Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "updated a speaker [" + validSpeaker.uuid + "]"))
          Redirect(routes.CFPAdmin.showSpeakerAndTalks(existingUUID)).flashing("success" -> "Profile updated")
        case None =>
          val webuser = Webuser.createSpeaker(validSpeaker.email, validSpeaker.firstName.getOrElse("Firstname"), validSpeaker.name.getOrElse("Lastname"))
          Webuser.saveNewWebuserEmailNotValidated(webuser)
          val newUUID = Webuser.saveAndValidateWebuser(webuser)
          Speaker.save(validSpeaker.copy(uuid = newUUID))
          Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "created a speaker [" + validSpeaker.uuid + "]"))
          Redirect(routes.CFPAdmin.showSpeakerAndTalks(newUUID)).flashing("success" -> "Profile saved")
      }
    }
  )
}*/
  def saveSpeakerAndTalk() = Action(parse.multipartFormData) {
    implicit request =>
      speakerForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.CFPAdmin.newSpeakerAndTalk(invalidForm)).flashing("error" -> "Invalid form, please check and correct errors. "),
        validSpeaker => {
          request.body.file("picture").map { picture =>
            val file:File = picture.ref.file
            val  targetStream: FileInputStream = new FileInputStream(file)
            val bytarray:Array[Byte]= new Array(targetStream.available())
            targetStream.read(bytarray)
            targetStream.close()
            Play.current.configuration.getString("cfp.imageBase") match {
              case Some(uRL)=>
                val pictureURL=Play.current.configuration.getString("cfp.imageBase").get+validSpeaker.uuid+".png"
                var fos: FileOutputStream  = new FileOutputStream(new File(pictureURL))
                fos.write(bytarray)
                fos.close()
                val speaker=validSpeaker.copy(picture= Some((Play.current.configuration.getString("cfp.imageData.hostname").get)+validSpeaker.uuid+".png"))

                Redirect(routes.CFPAdmin.createNewproposal()).withSession("validSpeaker" -> Json.prettyPrint(Json.toJson(speaker)))
              case None =>  Redirect(routes.CFPAdmin.createNewproposal()).withSession("validSpeaker" -> Json.prettyPrint(Json.toJson(validSpeaker)))
            }
          }.getOrElse {
            Redirect(routes.CFPAdmin.createNewproposal()).withSession("validSpeaker" -> Json.prettyPrint(Json.toJson(validSpeaker)))
          }
        }
      )
  }


  def managecfp () = SecuredAction(IsMemberOf("admin")){
    implicit request : SecuredRequest[play.api.mvc.AnyContent] =>
      if (ConferenceDescriptor.isCFPOpen){
        CfpManager.updateCfpStatut(CfpManager.getCfpStatut("cfp").get , false)
        Event.storeEvent(Event(request.webuser.email, request.webuser.uuid, "CFP was closed by "+ request.webuser.cleanName+""))
      }else{
        CfpManager.updateCfpStatut(CfpManager.getCfpStatut("cfp").get , true)
        Event.storeEvent(Event(request.webuser.email, request.webuser.uuid, "CFP was opened by "+ request.webuser.cleanName+""))
      }
      Redirect(routes.CFPAdmin.index())


  }

  def saveNewSpeaker() = Action(parse.multipartFormData) {
    implicit request =>
      speakerForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.CFPAdmin.newSpeaker(invalidForm)).flashing("error" -> "Invalid form, please check and correct errors. "),
        validSpeaker => {

          Option(validSpeaker.uuid) match {
            case Some(existingUUID) =>
              play.Logger.of("application.CFPAdmin").debug("Updating existing speaker " + existingUUID)
              Webuser.findByUUID(existingUUID).map {
                existingWebuser =>
                  Webuser.updateNames(existingUUID, validSpeaker.firstName.getOrElse("?"), validSpeaker.name.getOrElse("?"))
              }.getOrElse {
                val newWebuser = Webuser.createSpeaker(validSpeaker.email, validSpeaker.firstName.getOrElse("?"), validSpeaker.name.getOrElse("?"))
                val newUUID = Webuser.saveAndValidateWebuser(newWebuser)
                play.Logger.warn("Created missing webuser " + newUUID)
              }
              request.body.file("picture").map { picture =>
                //val filename = picture.filename
                val file:File = picture.ref.file
                val  targetStream: FileInputStream = new FileInputStream(file)
                val bytarray:Array[Byte]= new Array(targetStream.available())
                targetStream.read(bytarray)
                targetStream.close()
                val pictureUrl = Play.current.configuration.getString("cfp.imageBase").get+"/"+validSpeaker.uuid

                var fos: FileOutputStream  = new FileOutputStream(new File(pictureUrl))

                fos.write(bytarray)
                fos.close()
                val speaker = validSpeaker.copy(avatarUrl = Some(ConferenceDescriptor.getFullRoutePath(controllers.routes.RestAPI.profilePicture(validSpeaker.uuid).url)))

                Speaker.save(speaker)
                //Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "updated a speaker [" + validSpeaker.uuid + "]"))
                Redirect(routes.CFPAdmin.showSpeakerAndTalks(existingUUID)).flashing("success" -> "Profile updated")


              }.getOrElse {Speaker.save(validSpeaker)
                //Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "updated a speaker [" + validSpeaker.uuid + "]"))
                Redirect(routes.CFPAdmin.showSpeakerAndTalks(existingUUID)).flashing("success" -> "Profile updated")
              }


            case None =>
              val webuser = Webuser.createSpeaker(validSpeaker.email, validSpeaker.firstName.getOrElse("Firstname"), validSpeaker.name.getOrElse("Lastname") )
              Webuser.saveNewWebuserEmailNotValidated(webuser)
              val newUUID = Webuser.saveAndValidateWebuser(webuser)
              Speaker.save(validSpeaker.copy(uuid = newUUID))
              //Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "created a speaker [" + validSpeaker.uuid + "]"))
              Redirect(routes.CFPAdmin.showSpeakerAndTalks(newUUID)).flashing("success" -> "Profile saved")
          }
        }
      )
  }

  // @TODO refactor saveNewSpeaker & saveNewSpeakerOnly due to duplicate code
  def saveNewSpeakerOnly() = Action(parse.multipartFormData) {
    implicit request =>
      speakerForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.CFPAdmin.newSpeakerOnly(invalidForm)).flashing("error" -> "Invalid form, please check and correct errors. "),
        validSpeaker => {

          Option(validSpeaker.uuid) match {
            case Some(existingUUID) =>
              play.Logger.of("application.CFPAdmin").debug("Updating existing speaker " + existingUUID)
              Webuser.findByUUID(existingUUID).map {
                existingWebuser =>
                  Webuser.updateNames(existingUUID, validSpeaker.firstName.getOrElse("?"), validSpeaker.name.getOrElse("?"))
              }.getOrElse {
                val newWebuser = Webuser.createSpeaker(validSpeaker.email, validSpeaker.firstName.getOrElse("?"), validSpeaker.name.getOrElse("?"))
                val newUUID = Webuser.saveAndValidateWebuser(newWebuser)
                play.Logger.warn("Created missing webuser " + newUUID)
              }
              request.body.file("picture").map { picture =>
                //val filename = picture.filename
                val file:File = picture.ref.file
                val  targetStream: FileInputStream = new FileInputStream(file)
                val bytarray:Array[Byte]= new Array(targetStream.available())
                targetStream.read(bytarray)
                targetStream.close()
                val pictureUrl = Play.current.configuration.getString("cfp.imageBase").get+validSpeaker.uuid+".png"

                var fos: FileOutputStream  = new FileOutputStream(new File(pictureUrl))

                fos.write(bytarray)
                fos.close()
                val speaker = validSpeaker.copy(picture = Some("http://localhost/images/"+validSpeaker.uuid+".png"))

                Speaker.save(speaker)
                //Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "updated a speaker [" + validSpeaker.uuid + "]"))
                Redirect(routes.CFPAdmin.showSpeakerAndTalks(existingUUID)).flashing("success" -> "Profile updated")


              }.getOrElse {Speaker.save(validSpeaker)
                //Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "updated a speaker [" + validSpeaker.uuid + "]"))
                Redirect(routes.CFPAdmin.showSpeakerAndTalks(existingUUID)).flashing("success" -> "Profile updated")
              }


            case None =>
              val webuser = Webuser.createSpeaker(validSpeaker.email, validSpeaker.firstName.getOrElse("Firstname"), validSpeaker.name.getOrElse("Lastname"))
              Webuser.saveNewWebuserEmailNotValidated(webuser)
              val newUUID = Webuser.saveAndValidateWebuser(webuser)
              Speaker.save(validSpeaker.copy(uuid = newUUID))
              //Event.storeEvent(Event(validSpeaker.cleanName, request.webuser.uuid, "created a speaker [" + validSpeaker.uuid + "]"))
              Redirect(routes.CFPAdmin.showSpeakerAndTalks(newUUID)).flashing("success" -> "Profile saved")
          }
        }
      )
  }



  def retour() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      var speakerr: Option[Speaker] = None
      request.session.get("validSpeaker").map { validSpeaker =>
        val jsValuevalidspeaker: JsValue = Json.parse(validSpeaker)
        Json.fromJson(jsValuevalidspeaker).map { speaker =>
          speakerr = Some(speaker)

        }
      }
      speakerr match {

        case Some(spea) => {

          Ok(views.html.CFPAdmin.newSpeakerAndTalk(speakerForm.fill(spea)))
        }

        case None => {
          Ok(views.html.CFPAdmin.newSpeakerAndTalk(speakerForm))
        }

      }


  }

  def setPreferredDay(proposalId: String, day: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.setPreferredDay(proposalId: String, day: String)
      Redirect(routes.CFPAdmin.openForReview(proposalId)).flashing("success" -> ("Preferred day set to " + day))
  }

  def resetPreferredDay(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.resetPreferredDay(proposalId: String)
      Redirect(routes.CFPAdmin.openForReview(proposalId)).flashing("success" -> "No preferences")
  }


  def showProposalsWithNoVotes() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val proposals = Review.allProposalsWithNoVotes
      Ok(views.html.CFPAdmin.showProposalsWithNoVotes(proposals))
  }

  def showProposalsByTagId(tagId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      val tag = Tag.findById(tagId)
      if (tag.isDefined) {
        val proposals = Tags.allProposalsByTagId(tagId)

        Ok(views.html.CFPAdmin.showProposalsByTag(tag.get, proposals))
      } else {
        BadRequest("Invalid tag")
      }
  }

  def history(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Proposal.findById(proposalId).map {
        proposal: Proposal =>
          Ok(views.html.CFPAdmin.history(proposal))
      }.getOrElse(NotFound("Proposal not found"))
  }

  def isProposalStarred(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(StarProposal.isStarred(proposalId, request.webuser.uuid).toString)
  }

  def starProposal(proposalId: String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      if (StarProposal.isStarred(proposalId, request.webuser.uuid)) {
        StarProposal.unassign(proposalId, request.webuser.uuid)
        Event.storeEvent(Event(request.webuser.uuid, proposalId, s"Proposal '${Proposal.findById(proposalId).get.title}' unstar'ed by ${request.webuser.cleanName}"))
        Gone("unstar")
      } else {
        StarProposal.assign(proposalId, request.webuser.uuid)
        Event.storeEvent(Event(request.webuser.uuid, proposalId, s"Proposal '${Proposal.findById(proposalId).get.title}' star'ed by ${request.webuser.cleanName}"))
        Created("star")
      }
  }

  def allStarProposals() = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val starProposals = StarProposal.all().toSeq.sortBy(_._2).toMap
      Ok(views.html.CFPAdmin.starProposals(starProposals))
  }

  val MailsManagerForm = Form(mapping(
    "id" -> text,
    "Etype" -> nonEmptyText,
    "content" -> nonEmptyText,
    "lang"->nonEmptyText,
    "Subject"->nonEmptyText
  )(MailsManager.apply)(MailsManager.unapply))
  def generateTemplateEmailsFromStatics() =SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val web: Webuser = Webuser.apply("", "", "", "", "", "")
      val slots = List[Slot]()
      MailsManager.save(MailsManager.apply(MailsManager.generateId(), "Access Code Speaker", views.html.Mails.sendAccessCode("Speaker.email", "pswd").toString(), "en", "Access Code to CFP"))
      MailsManager.save(MailsManager.apply(MailsManager.generateId(), "Validate your account", views.html.Mails.sendValidateYourEmail("validationLink", "Devoxx UK 2017 CFP").toString(), "en", "Validate your account"))
      MailsManager.save(MailsManager.apply(MailsManager.generateId(), "We Create Account For You", views.html.Mails.sendAccountCreated("speaker.firstName", "Speaker.email", "pswd").toString(), "en", "We Create Account For You"))

    val mailsmanagers = MailsManager.allMails.toList
      Ok(views.html.CFPAdmin.allMailNotification(mailsmanagers))

  }
  def manageNotification () =SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      val mailsmanagers = MailsManager.allMails.toList
      Ok(views.html.CFPAdmin.allMailNotification(mailsmanagers))
  }
  def saveManageNotification () =SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      MailsManagerForm.bindFromRequest().fold(hasErrors =>BadRequest(views.html.CFPAdmin.manageNotification(hasErrors,"save")).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          if (!MailsManager.typeAndLangexists(validForm.Etype , validForm.Lang)) {
            MailsManager.save(validForm)
            Event.storeEvent(Event(request.webuser.email, request.webuser.uuid, " "+validForm.Etype+" email was created by "+ request.webuser.cleanName+""))

            Redirect(routes.CFPAdmin.manageNotification()).flashing("success" -> "New email model added")
          } else {
            Redirect(routes.CFPAdmin.manageNotification()).flashing("error" -> "This type of email is already exist")
          }
        } )  }

  def saveOrUpdateNotification (eventType:String,MailsId:Option[String]) =SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>

      if(eventType =="update"){
        MailsId match {
          case None => Redirect(routes.CFPAdmin.manageNotification())
          case Some(id) =>
            val mybeMails= MailsManager.mailByid(id)
            mybeMails match {
              case None =>Redirect(routes.CFPAdmin.manageNotification()).flashing("error" -> "Email model does not exist")
              case Some(mails) =>  Ok(views.html.CFPAdmin.manageNotification(MailsManagerForm.fill(mails),"update" ))
            }}
      }
      else{
        Ok(views.html.CFPAdmin.manageNotification(MailsManagerForm,eventType )) }
  }
  def deleteMail (id:String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      val mail = MailsManager.mailByid(id)
      mail  match {
        case None => Ok("ddd")
        case Some (m) => { MailsManager.delete(m.id)
          Event.storeEvent(Event(request.webuser.email, request.webuser.uuid, " "+m.Etype+" email was deleted by "+ request.webuser.cleanName+""))

          Redirect(routes.CFPAdmin.manageNotification()) }
      }
  }

  def updateManageNotification () =SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      MailsManagerForm.bindFromRequest().fold(hasErrors =>BadRequest(views.html.CFPAdmin.manageNotification(hasErrors,"update")).flashing("error" -> "Invalid form, please check and validate again")
        , validForm => {
          MailsManager.update(validForm.id,validForm)
          Event.storeEvent(Event(request.webuser.email, request.webuser.uuid, " "+validForm.Etype+" email was updated by "+ request.webuser.cleanName+""))

          Redirect(routes.CFPAdmin.manageNotification()).flashing("success" -> ("Update email model:"+validForm.id))
        })  }
  def activRegId( email:String , reg:String) = SecuredAction(IsMemberOf("speaker")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      //Webuser.activeRegistrationId(email , reg)
      Redirect(routes.Application.home())

  }
  def saveNewSpeakerByAdmin = SecuredAction(IsMemberOf("admin")) {
    implicit request:SecuredRequest[play.api.mvc.AnyContent] =>
      newWebuserForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.Authentication.prepareSignup(invalidForm)),
        validForm => {

          if (!Webuser.isEmailRegistered(validForm.email)) {
            //Webuser.saveNewWebuserEmailNotValidated(validForm)
            //TransactionalEmails.sendValidateYourEmail(validForm.email, routes.Authentication.validateYourEmailForSpeaker(Crypto.sign(validForm.email.toLowerCase.trim), new String(Base64.encodeBase64(validForm.email.toLowerCase.trim.getBytes("UTF-8")), "UTF-8")).absoluteURL())
            Webuser.saveAndValidateWebuser(validForm)
            //Webuser.activeVip(validForm , true)
            ZapActor.actor ! DoCreateTalkAfterCfp(validForm)
            Event.storeEvent(Event(Webuser.findByUUID(validForm.uuid).get.email, request.webuser.uuid, "invited speaker [" + Webuser.findByUUID(validForm.uuid).get.cleanName + "]"))
            //TransactionalEmails.sendAccessCode(validForm.email, validForm.password)
            Redirect(routes.CFPAdmin.allWebusers()).flashing("success" -> "Speaker created and invited to create talks")
          }
          else {
            Redirect(routes.CFPAdmin.allWebusers()).flashing("error" -> Messages("speakerExist"))
          }

        }
      )
  }
  def verifyRegId(regId:String)= Action {
    implicit request =>


      Ok(Webuser.regIdExist(regId).toString)

  }

//  def ifVisitorHasRegId (email:String) = Action {
//    implicit  request =>
//      Ok(Webuser.ifVisitorHasRegId(email).toString)
//  }
  
  def manageSlots() = SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      val allslot=  Slot.allSlot
      Ok(views.html.CFPAdmin.manageSlot(Slot.SlotForm1,allslot,"create"))

  }

  def  saveslot(action:String)=SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent] =>
      val allslot=Slot.allSlot
      Slot.SlotForm1.bindFromRequest.fold(
        invalidForm => if(action=="update"){
          BadRequest(views.html.CFPAdmin.manageSlot(invalidForm,allslot,"update")).flashing("error"->"Form invalid")
        }else{BadRequest(views.html.CFPAdmin.manageSlot(invalidForm,allslot,"create")).flashing("error"->"Form invalid")},
        validForm => {
          if(action=="update") {
            Slot.updateSlot(validForm.id,validForm)
            Redirect(routes.CFPAdmin.manageSlots()).flashing("success" -> "Slot was successfully updated")
          }
          else{


            Slot.saveslot(validForm)
            Redirect(routes.CFPAdmin.manageSlots()).flashing("success" -> "Slot was successfully saved")
          }}


      )
  }
  def updateslot(slotid:String)=SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      Slot.findSlotByUUID(slotid) match{
        case Some(a)=>
          val allslot=  Slot.allSlot
          Ok(views.html.CFPAdmin.manageSlot(Slot.SlotForm1.fill(a),allslot,"update"))
        case None=>Redirect(routes.CFPAdmin.manageSlots()).flashing("error"->" slot not found")
      }
  }
  def deleteSlot(slotid:String)=SecuredAction(IsMemberOf("cfp")){

    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>

      Slot.findSlotByUUID(slotid) match {
        case Some(a)=>
          Slot.deleteSlot(slotid)
          Redirect(routes.CFPAdmin.manageSlots()).flashing("success"->"the slot was  successfully deleted")
        case None => Redirect(routes.CFPAdmin.manageSlots()).flashing("error"->" slot does not exist")
      }



  }
  def manageRoom() =SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      val allroom=Room.allRoom

      Ok(views.html.CFPAdmin.manageRoom(Room.RoomForm,allroom,"create"))
  }


  def  saveRoom(action:String)=SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent] =>
      val allRoom=Room.allRoom
      Room.RoomForm.bindFromRequest.fold(
        invalidForm => if(action=="update"){
          BadRequest(views.html.CFPAdmin.manageRoom(invalidForm,allRoom,"update")).flashing("error"->"Form invalid")
        }else{BadRequest(views.html.CFPAdmin.manageRoom(invalidForm,allRoom,"create")).flashing("error"->"Form invalid")},
        validForm => {
          if(action=="update") {
            Room.updateRoom(validForm.id,validForm)
            Redirect(routes.CFPAdmin. manageRoom()).flashing("success" -> "Room was successfully updated")
          }
          else{
            Room.saveroom(validForm)
            Redirect(routes.CFPAdmin. manageRoom()).flashing("success" -> "Room was successfully saved")
          }

        }
      )
  }
  def updateRoom(roomid:String)=SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      Room.findRoomByUUID(roomid) match{
        case Some(a)=>
          val allroom=  Room.allRoom
          Ok(views.html.CFPAdmin.manageRoom(Room.RoomForm.fill(a),allroom,"update"))
        case None=>Redirect(routes.CFPAdmin.manageRoom()).flashing("error"->" room not found")
      }
  }
  def deleteRoom(roomid:String)=SecuredAction(IsMemberOf("cfp")){

    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>

      Room.findRoomByUUID(roomid) match {
        case Some(a)=>
          Room.deleteRoom(roomid)
          Redirect(routes.CFPAdmin.manageRoom()).flashing("success"->"the room was  successfully deleted")
        case None => Redirect(routes.CFPAdmin.manageRoom()).flashing("error"->" room does not exist")
      }



  }
  def changeEmailConfiguration(statut:String) = SecuredAction(IsMemberOf("cfp")){
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      MailsManager.changeEmailMode(statut)
      Redirect(routes.CFPAdmin.index())


  }

  def notifyVisitors = SecuredAction((IsMemberOfGroups(securityGroups))) {
    implicit request:SecuredRequest [play.api.mvc.AnyContent] =>
      ZapActor.actor ! SendScheduledFavorites()
      Redirect(routes.CFPAdmin.allVisitiors()).flashing("success" -> "Visitors are notified")
  }
  def notifyAllSpeakersForSchedule = SecuredAction(IsMemberOf("cfp")) {
    implicit request:SecuredRequest [play.api.mvc.AnyContent] =>
      ZapActor.actor ! SendScheduleForSpeakers()
      Redirect(routes.CFPAdmin.allWebusers()).flashing("success" -> "Speakers notified")
  }
  def notifySpeakerForSchedule(uuid:String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request:SecuredRequest [play.api.mvc.AnyContent] =>
      ZapActor.actor ! SendScheduleForSpeaker(uuid)
      Redirect(routes.CFPAdmin.allWebusers()).flashing("success" -> s"Speaker ${Webuser.findByUUID(uuid).get.cleanName} notified")
  }
  
//  def switchToAdminVis(uuidSpeaker:String)= SecuredAction(IsMemberOf("admin")) {
//    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
//      Webuser.findByUUID(uuidSpeaker).filterNot(_.uuid == "bd894205a7d579351609f8dcbde49b9ffc0fae13").map {
//        webuser =>
//          if (Webuser.hasAccessToAdminVis(uuidSpeaker)) {
//            Event.storeEvent(Event(uuidSpeaker, request.webuser.uuid, s"removed ${webuser.cleanName} from Admin visitors group"))
//            Webuser.removeFromAdminVis(uuidSpeaker)
//          } else {
//            Webuser.addToAdminVisitors(uuidSpeaker)
//            Event.storeEvent(Event(uuidSpeaker, request.webuser.uuid, s"added ${webuser.cleanName} to Admin visitors group"))
//          }
//          Redirect(routes.CFPAdmin.allWebusers())
//      }.getOrElse {
//        NotFound("Webuser not found")
//      }
//  }
  
  def saveproposalByAdmin(ids: String) = SecuredAction {
    implicit request =>

      Proposal.proposalForm.bindFromRequest.fold(
        hasErrors => BadRequest(views.html.CFPAdmin.newProposal(hasErrors)),
        proposal => {
          val uuid=ids

              val existingProp = Proposal.findProposal(ids , proposal.id)
              val updatedProposal = proposal.copy(mainSpeaker = existingProp.get.mainSpeaker, secondarySpeaker = existingProp.get.secondarySpeaker, otherSpeakers = existingProp.get.otherSpeakers , state = existingProp.get.state)

                Proposal.save(ids, updatedProposal, existingProp.get.state)
                Event.storeEvent(Event(proposal.id, ids, "Edited proposal " + proposal.id + " with current state [" + existingProp.get.state.code + "]"))
                Redirect(routes.Backoffice.allProposals(None))
          }
      )
  }

  def editProposalByAdmin(proposalId: String , id:String) = SecuredAction {
    implicit request =>
      val uuid = id
      val maybeProposal = Proposal.findProposal(id, proposalId)
      maybeProposal match {
        case Some(proposal) =>
          if (proposal.mainSpeaker == id) {
            val proposalForm = Proposal.proposalForm.fill(proposal)
            Ok(views.html.CFPAdmin.editProposal(proposalForm , Webuser.findByUUID(id).get))
          } else if (proposal.secondarySpeaker.isDefined && proposal.secondarySpeaker.get == id) {
            // Switch the mainSpeaker and the other Speakers
            val proposalForm = Proposal.proposalForm.fill(Proposal.setMainSpeaker(proposal, id))
            Ok(views.html.CFPAdmin.editProposal(proposalForm , Webuser.findByUUID(id).get)).flashing("id" ->  id)
          } else if (proposal.otherSpeakers.contains(id)) {
            // Switch the secondary speaker and this speaker
            val proposalForm = Proposal.proposalForm.fill(Proposal.setMainSpeaker(proposal, id))
            Ok(views.html.CFPAdmin.editProposal(proposalForm , Webuser.findByUUID(id).get)).flashing("id" ->  id)
          } else {
            Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> "Invalid state")
          }
        case None =>
          Redirect(routes.CallForPaper.homeForSpeaker()).flashing("error" -> Messages("invalid.proposal"))
      }
  }
}
