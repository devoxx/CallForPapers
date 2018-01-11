package models

import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import play.api.Play.current

/**
  * Created by EnvY on 02/11/2017.
  */
object NotificationService {
  val port = 4545
  def isSubscribed(uuid:String , event:String):Boolean = {


    var result = Await.result(WS.url(s"http://localhost:${NotificationService.port}/verifySubscription/${uuid}/${event}").get().map( res => res.body ).recover{ case _ => None}, Duration.apply(3000,"ms"))

   result.equals("true")
  }



  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  def loadNotif(id:String): List[JsValue] ={
    /*
        implicit val NotificationReads = Json.reads[Notification]

        implicit val NotificationWrites = Json.writes[Notification]*/

    //var futureResponse: Future[WSResponse]=
    /*val futureResponse: Future[JsResult[Notification]]= WS.url(s"http://localhost:7080/notifs/receiver/${id.toString}").get()
      .map {
        response => (response.json \ "").validate[Notification]
      }*/
    //futureResponse.recover{case e:java.net.ConnectException => e.getMessage}
    val result = Await.result(WS.url(s"http://localhost:${NotificationService.port}/notifs/receiver/${id.toString}").get().map(res => res.json).recover{case _ => JsArray()}, Duration.apply(3000 , "ms"))

    var j =  result.asInstanceOf[JsArray]
    /*val response:Future[String] = WS.url("http://localhost:7080/notifs/receiver").withQueryString("receiver" -> id).get().map(_.body)
      response*/
    j.value.toList

  }

}
