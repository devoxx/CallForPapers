package controllers

import library.Redis
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

  private val CONFIG_REDIS_KEY = "config."

  val CONFIG_URL_SPONSORS : String = CONFIG_REDIS_KEY + "url.sponsors"
  val CONFIG_URL_WEBSITE : String = CONFIG_REDIS_KEY + "url.website"
  val CONFIG_URL_INFO : String = CONFIG_REDIS_KEY + "url.info"
  val CONFIG_URL_REGISTRATION : String = CONFIG_REDIS_KEY + "url.registration"
  val CONFIG_URL_CFP_HOSTNAME : String = CONFIG_REDIS_KEY + "cfp.hostname"

  /**
    * Get a key value from Redis, if not available from Configuration and otherwise default value
    *
    * @param keyName the key name
    * @return the key value
    */
  def getKeyValue(keyName: String): Option[String] = Redis.pool.withClient {
    implicit client =>

      if (client.exists(keyName)) {
        client.get(keyName)
      } else if (Play.current.configuration.getString(keyName).isDefined) {
        Play.current.configuration.getString(keyName)
      } else {
        Option(null)
      }
  }

  /**
    * Store the key & value into Redis.
    *
    * @param keyName  key name
    * @param keyValue key value
    * @return
    */
  def setKeyValue(keyName: String, keyValue: String) = Redis.pool.withClient {
    implicit client =>
      client.set(keyName, keyValue)
  }

  def processJson(jsonObject:JsObject): Unit = {

    if ((jsonObject \\ CONFIG_URL_SPONSORS).nonEmpty) {
      val value = (jsonObject \ CONFIG_URL_SPONSORS).as[String]

      setKeyValue(CONFIG_URL_SPONSORS, value)
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
