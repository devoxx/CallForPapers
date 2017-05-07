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

import models.{Event, Invitation, Speaker}
import play.api.mvc.{Action, AnyContent}

/**
 * A controller that is now responsible for the invitation system, introduced for Devoxx BE 2014.
 *
 * Created by nicolas martignole on 30/07/2014.
 */
object InviteController extends SecureCFPController{

  def allInvitations(): Action[AnyContent] = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = Invitation.all.flatMap{uuid=>
        Speaker.findByUUID(uuid)
      }

      Ok(views.html.InviteController.allInvitations(speakers))
  }

  def invite(speakerUUID:String): Action[AnyContent] = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speaker = Speaker.findByUUID(speakerUUID)
      if (speaker.isDefined) {
        Invitation.inviteSpeaker(speakerUUID,request.webuser.uuid)
        Event.storeEvent(Event(speakerUUID, request.webuser.uuid, s"Speaker ${speaker.get.cleanName} invited by ${request.webuser.cleanName}"))
        Created("{\"status\":\"created\"}").as(JSON)
      } else {
        NotFound("{\"status\":\"speaker not found\"}").as(JSON)
      }
  }

  def cancelInvite(speakerUUID:String): Action[AnyContent] = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speaker = Speaker.findByUUID(speakerUUID)
      if (speaker.isDefined) {
        Invitation.removeInvitation(speakerUUID)
        Event.storeEvent(Event(speakerUUID, request.webuser.uuid, s"Speaker ${speaker.get.cleanName} invite canceled by ${request.webuser.cleanName}"))
        Created("{\"status\":\"deleted\"}").as(JSON)
      } else {
        NotFound("{\"status\":\"speaker not found\"}").as(JSON)
      }
  }
}