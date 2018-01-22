package models
import com.sksamuel.elastic4s.mappings.False
import library.Redis
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.apache.commons.lang3.StringUtils
import play.api.i18n.Lang
import play.api.libs.json.{Format, Json}
import play.twirl.api.Html
/**
  * Created by EnvY on 01/05/2017.
  */
case class MailsManager (id:String,
                         Etype:String,
                         content:String,
                         Lang: String
                        ,Subject : String
                         ) {



}
object MailsManager {


  implicit  val mailsmanagerFormat :Format[MailsManager] = Json.format[MailsManager]

 val ListofMail= Seq(
   "approved" -> "approved",
   "rejected" -> "rejected",
   "invite to create talk" -> "invite to create talk",
   "Access Code Speaker" -> "Access Code Speaker",
   "Validate your account" -> "Validate your account",
   "We Create Account For You" -> "We Create Account For You"



 )

    def save(mailsManager: MailsManager) = Redis.pool.withClient {
      client =>
           if (!typeAndLangexists(mailsManager.Etype,mailsManager.Lang)){
           val jsonMailsManager = Json.stringify(Json.toJson(mailsManager))
           client.hset("MailsManager", mailsManager.id, jsonMailsManager)}


    }


  def allMails: Map[String, Option[MailsManager]] = Redis.pool.withClient {
    client =>
      client.hgetAll("MailsManager").map {
        case (key: String, valueJson: String) =>
          (key, Json.parse(valueJson).asOpt[MailsManager])
      }
  }
  def mailByid(id : String) : Option[MailsManager]  = Redis.pool.withClient {
    client =>
      client.hget("MailsManager" , id).map {
        json: String =>
          Json.parse(json).as[MailsManager]
      }
  }
  def updateMail (id : String , newContent:String) = Redis.pool.withClient {

    client =>
      mailByid(id).map {
        mailsmanager =>

          MailsManager.update(id , mailsmanager.copy( content = newContent ))

      }

  }
 def typeAndLangexists ( t: String , l: String): Boolean = {
   val m = MailsManager.allMails
   var b = false
   m.map { mails =>
     mails._2 match {
       case Some(mail) =>  if (mail.Etype.equals(t) && mail.Lang.equals(l)) {
         b = true
       }
       else { b }
       case None =>
     }
     }

   b
 }

  def update ( id: String , mailsManager: MailsManager) = Redis.pool.withClient {

    client =>
    val jsonMailsManager = Json.stringify(Json.toJson(mailsManager.copy(id = id)))

    client.hset("MailsManager" , id , jsonMailsManager)

  }
  def delete ( id : String) = Redis.pool.withClient {
    client =>
      mailByid(id).map {
        mailmanager =>
          val tx = client.multi()
          tx.hdel("MailsManager" , mailmanager.id)
          tx.hdel("MailsManager" , mailmanager.Lang)
          tx.hdel("MailsManager" , mailmanager.Etype)
          tx.hdel("MailsManager" , mailmanager.content)
          tx.hdel("MailsManager" , mailmanager.Subject)
          tx.exec()

      }

  }


  def generateId(): String = Redis.pool.withClient {
    implicit client =>
      val newId = RandomStringUtils.randomAlphabetic(3).toUpperCase + "-" + RandomStringUtils.randomNumeric(4)
      if (client.hexists("MailsManager", newId)) {
        play.Logger.of("MailManager").warn(s"MailManager ID collision with $newId")
        generateId()
      } else {
        newId
      }
  }


  def getMailByTypeAndLang (t:String , l:String):Option[MailsManager] = {
    val m = MailsManager.allMails
    var mai = new MailsManager("","" ,"" , "" , "")
    m.map { mails =>
      mails._2 match {
        case Some(ma) => if (ma.Etype.equals(t)&& ma.Lang.equals(l)) {

          mai = ma
        }

        case None => "fff"
      }
    }
    MailsManager.mailByid(mai.id)
  }

  def changeEmailMode(emailmode: String) = Redis.pool.withClient {
    client =>
      val tx = client.multi()
      tx.del("EmailMode")
      tx.set("EmailMode", emailmode)
       tx.exec()

  }
  def saveEmailMode(emailmode: String) = Redis.pool.withClient {
    client =>

      client.set("EmailMode", emailmode)


  }

  def getEmailMode():Option[String]=Redis.pool.withClient{
  client=>
      client.get("EmailMode")
  }


}
