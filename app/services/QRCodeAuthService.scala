package services;

import play.api.libs.Crypto
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.ConferenceDescriptor
import controllers.routes

case class QRCodeData(authToken: String, authEndpoint: String, eventDetailsEndpoint: String)


object QRCodeAuthService {
  implicit val dataReads = Json.reads[QRCodeData]
  implicit val dataWrites = Json.writes[QRCodeData]

  def generate(uuid: String): String = {
    val authToken = Crypto.encryptAES(uuid)
    val authEndpoint = ConferenceDescriptor.getFullRoutePath(routes.Authentication.loginWithAuthToken.url)
    val eventDetailsEndpoint = ConferenceDescriptor.getFullRoutePath(routes.RestAPI.eventDetails.url)
    Json.toJson(
      QRCodeData(
        authToken,
        authEndpoint,
        eventDetailsEndpoint
        )
    ).toString
  }

  def decryptTokenAndGetUUID(token: String): String = {
    Crypto.decryptAES(token)
  }
}
