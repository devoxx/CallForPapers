package controllers

import models.ConferenceDescriptor
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.nonEmpty

/**
  * @author Stephan Janssen
  */
object Configuration extends SecureCFPController {

  val configForm = Form("json" -> (text verifying nonEmpty))

  def showConfig: Action[AnyContent] = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Configuration.showConfiguration(ConferenceDescriptor.current()))
  }

  def showImportConfig: Action[AnyContent] = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Configuration.importConfiguration(configForm))
  }

  def doImportConfig() = Action {
    implicit request =>
      configForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.Configuration.importConfiguration(invalidForm)).flashing("error" -> Messages("Something went wrong, check logs!")),
        validForm => Redirect(routes.Configuration.showConfig()).flashing("success" -> Messages("Configuration updated!"))

      )
  }
}
