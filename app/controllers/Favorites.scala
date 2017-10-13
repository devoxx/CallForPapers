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

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.Action

import scala.concurrent.Future

/**
 * Controller used from the program pages, to fav a talk.
 *
 * @author created by N.Martignole, Innoteria, on 26/10/15.
 */
object Favorites extends UserCFPController {
  private val securityGroups = List("cfp", "adminVis" , "admin")
  def home() = SecuredAction {
    implicit request =>

      val proposals = FavoriteTalk.allForUser(request.webuser.uuid)

      val slots = proposals.flatMap {
        talk: Proposal =>
          ScheduleConfiguration.findSlotForConfType(talk.talkType.id, talk.id)
      }.toList.sortBy(_.from.getMillis)
      val rooms = slots.groupBy(_.room).keys.toList.sortBy(_.id)
      Ok(views.html.Favorites.homeFav(slots, rooms))
  }
  def favSchedule() = SecuredAction {
    implicit request =>

      val proposals = FavoriteTalk.getAllfavTalkByVisitor(request.webuser.uuid)

      val slots = proposals.flatMap {
        talk: Proposal =>
          ScheduleConfiguration.findSlotForConfType(talk.talkType.id, talk.id)
      }.sortBy(_.from.getMillis)
      val rooms = slots.groupBy(_.room).keys.toList.sortBy(_.id)
      Ok(views.html.Favorites.homeFavVisitor(slots, rooms))
  }

  val formProposal = Form("proposalId" -> nonEmptyText)

  def likeOrUnlike = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      formProposal.bindFromRequest().fold(
        hasErrors => BadRequest("Invalid proposalId"),
        proposalId => {
        Proposal.findById(proposalId).filterNot(_.state == ProposalState.ARCHIVED).map {
          proposal =>
            if (FavoriteTalk.isFavByThisUser(proposal.id, request.webuser.uuid)) {
              FavoriteTalk.unfavTalk(proposal.id, request.webuser.uuid)
              Ok("{\"status\":\"unfav\"}").as(JSON)
            } else {
              FavoriteTalk.favTalk(proposal.id, request.webuser.uuid)
              Ok("{\"status\":\"fav\"}").as(JSON)
            }
        }.getOrElse {
          NotFound("Proposal not found")
        }
      })

  }

  def toggleProposalToAgenda(proposalId: String) = SecuredAction { implicit request =>
    FavoriteTalk.favTalkByVisitor(proposalId , request.webuser.uuid)
    FavoriteTalk.getAllfavTalkByVisitor(request.webuser.uuid)
      .filter(_.id==proposalId)
      .headOption
      .map(p => Ok(JsObject(Seq("proposalId" -> JsString(proposalId), "status" -> JsString("Favorited")))))
      .getOrElse(Ok(JsObject(Seq("proposalId" -> JsString(proposalId), "status" -> JsString("Not Favorited")))))
  }

  def addAsFavorite(idP:String) = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

     FavoriteTalk.favTalkByVisitor(idP , request.webuser.uuid)

  Redirect(routes.Publisher.showByTalkType(Proposal.findById(idP).get.talkType.id))}
  def removefromFavorite(idP:String) = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      FavoriteTalk.unfavTalkByVisitor(idP , request.webuser.uuid)
      Redirect(routes.Publisher.showByTalkType(Proposal.findById(idP).get.talkType.id))}

  def addAsFavoritev2(idP:String) = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      FavoriteTalk.favTalkByVisitor(idP , request.webuser.uuid)

      Redirect(routes.Favorites.welcomeVisitor(Some(Proposal.findById(idP).get.talkType.id)))}
  def removefromFavoritev2(idP:String) = SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>

      FavoriteTalk.unfavTalkByVisitor(idP , request.webuser.uuid)
      Redirect(routes.Favorites.welcomeVisitor(Some(Proposal.findById(idP).get.talkType.id)))}



  def welcomeVisitor( talkType:Option[String]) = SecuredAction.async {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      talkType match {
        case Some(a)=>
      a match {
      case ConferenceDescriptor.ConferenceProposalTypes.CONF.id =>
      Future.successful(Ok(views.html.Favorites.welcomeVisitor(request.webuser,Proposal.allAcceptedByTalkType(List(ConferenceDescriptor.ConferenceProposalTypes.CONF.id,
      ConferenceDescriptor.ConferenceProposalTypes.CONF.id)), a ,Webuser.newVisitorForm.fill(request.webuser))))
      case other =>
      Future.successful(Ok(views.html.Favorites.welcomeVisitor(request.webuser,Proposal.allAcceptedByTalkType(a), a ,Webuser.newVisitorForm.fill(request.webuser))))

      }
        case None=>Future.successful(Ok(views.html.Favorites.welcomeVisitor(request.webuser,Proposal.allAcceptedByTalkType(List(ConferenceDescriptor.ConferenceProposalTypes.CONF.id,
          ConferenceDescriptor.ConferenceProposalTypes.CONF.id)), ConferenceDescriptor.ConferenceProposalTypes.CONF.id ,Webuser.newVisitorForm.fill(request.webuser))))

      }

  }
  def editProfile()  = SecuredAction{
    implicit request:SecuredRequest[play.api.mvc.AnyContent]=>
      Webuser.newVisitorForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.Favorites.welcomeVisitor(request.webuser,Proposal.allAcceptedByTalkType(List(ConferenceDescriptor.ConferenceProposalTypes.CONF.id, ConferenceDescriptor.ConferenceProposalTypes.CONF.id)), ConferenceDescriptor.ConferenceProposalTypes.CONF.id ,invalidForm)),
        validForm => {
          Webuser.findByEmail(validForm.email) match {
            case Some(v)=>
                Webuser.update(validForm)
              Redirect(routes.Favorites.welcomeVisitor(None))
            case None=>Redirect(routes.Favorites.welcomeVisitor(None))
          }
        }
      )

  }

  def isFav(proposalId: String) = Action {
    implicit request =>
      UserCFPController.findAuthenticator.map {
        uuid =>
          val jsonResponse = JsObject(Seq("proposalId" -> JsString(proposalId)))
          Ok(jsonResponse)
      }.getOrElse {
        NoContent
      }
  }
  def getAllfavByVisitors(webuserId : String)=SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent]=>
      val favs = FavoriteTalk.getAllfavTalkByVisitor(webuserId)

      Ok(views.html.Favorites.ListofMyFav(favs, Webuser.findByUUID(webuserId)))

  }
  def getFavedScheduled (webuserId : String)=SecuredAction {
    implicit request: SecuredRequest[play.api.mvc.AnyContent]=>
      val favs = FavoriteTalk.getAllfavTalkByVisitor(webuserId)
      val slots = favs.flatMap {
        talk: Proposal =>
          ScheduleConfiguration.findSlotForConfType(talk.talkType.id, talk.id)
      }
      Ok(slots.toString())}

  def showAllForAdmin()=SecuredAction(IsMemberOfGroups(securityGroups)){
    implicit r:SecuredRequest[play.api.mvc.AnyContent]=>
      val all=FavoriteTalk.allFavorites().toList.sortBy(_._2).reverse
      Ok(views.html.Favorites.showAllForAdmin(all))
  }

}
