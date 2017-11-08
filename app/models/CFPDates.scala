package models

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.Format
import library.Redis
import play.api.libs.json.Json

case class CFPDates(opening: String,
                    closing: String,
                    scheduleAnnouncement: String,
                    toggleCFPAcceptance: String) {
  lazy val asJson: String = {
    Json.stringify(Json.toJson(this))
  }
}

object CFPDates {

  implicit val cfpDatesFormat: Format[CFPDates] = Json.format[CFPDates]

  private val cfpDates = "CfpDates"

  val cfpDatesForm = Form(mapping(
    "opening" -> text,
    "closing" -> text,
    "scheduleAnnouncement" -> text,
    "toggleCFPAcceptance" -> text
  )(validateCfpDates)(unapplyCfpDatesForm))

  def validateCfpDates(opening: String,
                       closing: String,
                       scheduleAnnouncement: String,
                       toggleCFPAcceptance: String): CFPDates = {
    CFPDates(
      opening,
      closing,
      scheduleAnnouncement,
      toggleCFPAcceptance
    )
  }

  def unapplyCfpDatesForm(cfpDates: CFPDates): Option[(String, String, String, String)] = {
    Option(
      cfpDates.opening,
      cfpDates.closing,
      cfpDates.scheduleAnnouncement,
      cfpDates.toggleCFPAcceptance)
  }

  def save(newCfpDates: CFPDates): Long = Redis.pool.withClient {
    val conferenceCode = ConferenceDescriptor.current().eventCode
    play.Logger.debug(s"Saving CFP Dates: ${newCfpDates.asJson}")
    client =>
      client.hset(cfpDates, s"$conferenceCode", newCfpDates.asJson)
  }

  def load(): Option[CFPDates] = Redis.pool.withClient {
    val conferenceCode = ConferenceDescriptor.current().eventCode
    client =>
      client.hget(cfpDates, s"$conferenceCode").flatMap {
        cfpDates: String =>
          Json.parse(cfpDates).asOpt[CFPDates]
      }
  }
}
