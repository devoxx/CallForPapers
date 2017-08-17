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

  // Conference URLs
  val CONFIG_URL_SPONSORS : String = "url.sponsors"
  val CONFIG_URL_WEBSITE : String = "url.website"
  val CONFIG_URL_INFO : String = "url.info"
  val CONFIG_URL_REGISTRATION : String = "url.registration"
  val CONFIG_URL_CFP_HOSTNAME : String = "cfp.hostname"

  // Conference Dates
  val CONFIG_TIMING_DATES : String = "timing.dates"
  val CONFIG_TIMING_FIRST_DAY_FR : String = "timing.firstDayFR"
  val CONFIG_TIMING_FIRST_DAY_EN : String = "timing.firstDayEN"
  val CONFIG_TIMING_DATES_FR : String = "timing.datesFR"
  val CONFIG_TIMING_DATES_EN : String = "timing.datesEN"
  val CONFIG_TIMING_CFP_OPEN : String = "timing.cfpOpen"
  val CONFIG_TIMING_CFP_CLOSED : String = "timing.cfpClosed"
  val CONFIG_TIMING_SCHEDULE_DATE : String = "timing.cfpSchedule"

  // Mail
  val CONFIG_MAIL_FROM : String = "mail.from"
  val CONFIG_MAIL_COMMITTEE : String = "mail.committee.email"
  val CONFIG_MAIL_BCC : String = "mail.bcc"
  val CONFIG_MAIL_BUGREPORT : String = "mail.bugreport.recipient"

  // Host
  val CONFIG_HOST_NAME : String = "host.name"
  val CONFIG_HOST_WEBSITE : String = "host.website"

  // I18N
  val CONFIG_LOCALE_FR_ENABLED : String = "locale.enableFR"

  // General Settings
  val CONFIG_SETTINGS_PREFERRED_DAY_ENABLED : String = "settings.preferredDayEnabled"
  val CONFIG_SETTINGS_NOTIFY_NEW_PROPOSAL : String = "settings.notifyNewProposal"
  val CONFIG_SETTINGS_GOLDENTICKET_ACTIVE : String = "goldenTicket.active"
  
  /**
    * Get a key value from Redis, if not available from Configuration and otherwise default value
    *
    * @param keyName the key name
    * @return the key value
    */
  def getKeyValue(keyName: String): Option[String] = Redis.pool.withClient {
    implicit client =>

      if (client.exists(CONFIG_REDIS_KEY + keyName)) {
        client.get(CONFIG_REDIS_KEY + keyName)
      } else if (Play.current.configuration.getString(keyName).isDefined) {
        Play.current.configuration.getString(keyName)
      } else {
        Option(null)
      }
  }

  def getKeyBoolean(keyName: String) : Boolean = Redis.pool.withClient {
    implicit client =>

      if (client.exists(CONFIG_REDIS_KEY + keyName)) {
        client.get(CONFIG_REDIS_KEY + keyName).asInstanceOf[Boolean]
      } else if (Play.current.configuration.getString(keyName).isDefined) {
        Play.current.configuration.getBoolean(keyName).get
      } else {
        false
      }
  }

  val allConfigurationKeys =
    List(
      CONFIG_URL_SPONSORS,
      CONFIG_URL_REGISTRATION,
      CONFIG_URL_INFO,
      CONFIG_URL_WEBSITE,
      CONFIG_URL_CFP_HOSTNAME,

      CONFIG_TIMING_DATES,
      CONFIG_TIMING_FIRST_DAY_FR,
      CONFIG_TIMING_FIRST_DAY_EN,
      CONFIG_TIMING_DATES_FR,
      CONFIG_TIMING_DATES_EN,
      CONFIG_TIMING_CFP_OPEN,
      CONFIG_TIMING_CFP_CLOSED,
      CONFIG_TIMING_SCHEDULE_DATE,

      CONFIG_MAIL_FROM,
      CONFIG_MAIL_COMMITTEE,
      CONFIG_MAIL_BCC,
      CONFIG_MAIL_BUGREPORT,

      CONFIG_HOST_NAME,
      CONFIG_HOST_WEBSITE,

      CONFIG_LOCALE_FR_ENABLED,

      CONFIG_SETTINGS_PREFERRED_DAY_ENABLED,
      CONFIG_SETTINGS_NOTIFY_NEW_PROPOSAL
    )

  def processJson(jsonObject:JsObject): Unit = {

    allConfigurationKeys.foreach(key => {
      if ((jsonObject \\ key).nonEmpty) {
        val value = (jsonObject \ key).as[String]
        setKeyValue(key, value)
      }
    })
  }

  /**
    * Store the key & value into Redis.
    *
    * @param keyName  key name
    * @param keyValue key value
    * @return
    */
  def setKeyValue(keyName: String, keyValue: String) : String = Redis.pool.withClient {
    implicit client =>
      client.set(CONFIG_REDIS_KEY + keyName, keyValue)
  }
}
