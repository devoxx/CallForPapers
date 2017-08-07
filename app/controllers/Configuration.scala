package controllers

import models.ConferenceDescriptor
import play.api.Play
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.nonEmpty
import play.api.libs.json.{JsObject, JsValue, Json}

/**
  * @author Stephan Janssen
  */
object Configuration extends SecureCFPController {

  val configForm = Form("json" -> (text verifying nonEmpty))

  def showConfig: Action[AnyContent] = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Configuration.showConfiguration(ConferenceDescriptor.current))
  }

  def showImportConfig: Action[AnyContent] = SecuredAction(IsMemberOf("admin")) {
    implicit request: SecuredRequest[play.api.mvc.AnyContent] =>
      Ok(views.html.Configuration.importConfiguration(configForm))
  }

  def doImportConfig() = Action {
    implicit request =>
      configForm.bindFromRequest.fold(
        invalidForm => BadRequest(views.html.Configuration.importConfiguration(invalidForm)).flashing("error" -> Messages("Something went wrong, check logs!")),
        validForm => {

          implicit val json : JsValue = Json.parse(validForm)

          val readableString: String = Json.prettyPrint(json)
          System.out.println(readableString)

          processJson(json.as[JsObject])

          Redirect(routes.Configuration.showConfig()).flashing("success" -> Messages("Configuration updated!"))
        }

      )
  }

  def processJson(jsonObject:JsObject): Unit = {

    if ((jsonObject \\ "cfpOpen").nonEmpty) {
      val value = (jsonObject \ "cfpOpen").as[Boolean]

      // TODO Play.current.configuration.getBoolean("cfp.isOpen").getOrElse(false)
      System.out.println(value)
    }

    if ((jsonObject \\ "timezone").nonEmpty) {
      val value = (jsonObject \ "timezone").as[String]

      // TODO Play.current.configuration.getString("conference.timezone").getOrElse("Europe/Brussels")
      System.out.println(value)
    }

    if ((jsonObject \\ "fromEmail").nonEmpty) {
      val value = (jsonObject \ "fromEmail").as[String]

      // TODO Play.current.configuration.getString("mail.from").getOrElse("info@devoxx.com"),
      System.out.println(value)
    }

    if ((jsonObject \\ "hashtag").nonEmpty) {
      val value = (jsonObject \ "hashtag").as[String]
      
      System.out.println(value)

    }

  }
}
