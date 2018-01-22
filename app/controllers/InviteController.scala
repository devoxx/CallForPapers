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

import models.{Invitation, Proposal, ProposalState, Speaker}

/**
 * A controller that is now responsible for the invitation system, introduced for Devoxx BE 2014.
 *
 * Created by nicolas martignole on 30/07/2014.
 */
object InviteController extends SecureCFPController{

  def allInvitations()=SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      val speakers = Invitation.all.flatMap{uuid=>
        Speaker.findByUUID(uuid)
      }

      val toReturn = speakers.map{
        s=>
          (s,Proposal.allProposalsByAuthor(s.uuid).filter(s=> s._2.state==ProposalState.SUBMITTED || s._2.state==ProposalState.APPROVED ))
      }

      Ok(views.html.InviteController.allInvitations(toReturn))
  }

  def invite(speakerUUID:String) = SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Invitation.inviteSpeaker(speakerUUID,request.webuser.uuid)
      Created("{\"status\":\"created\"}").as(JSON)
  }

  def cancelInvite(speakerUUID:String)= SecuredAction(IsMemberOf("cfp")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Invitation.removeInvitation(speakerUUID)
      Ok("{\"status\":\"deleted\"}").as(JSON)
  }
}