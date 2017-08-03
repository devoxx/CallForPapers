package controllers

import models.ConferenceDescriptor
import play.api.mvc.{Action, AnyContent}

/**
  * @author Stephan Janssen
  */
object Configuration extends SecureCFPController {

  def showConfig: Action[AnyContent] = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Configuration.showConfiguration(ConferenceDescriptor.current()))
  }
}
