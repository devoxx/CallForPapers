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

package models

import library.{Redis, ZapJson}
import models.Proposal.{unapplyProposalForm, validateNewProposal}
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.Crypto

/**
  * Time slots and Room are defined as static file.
  * Instead of using a database, it's way simpler to define once for all everything as scala.
  * Trust me.
  * Created by Nicolas Nartignole on 01/02/2014.
  * Frederic Camblor added ConferenceDescriptor 07/06/2014
  */

case class Room(id: String, name: String, capacity: Int, setup: String, recorded: String) extends Ordered[Room] {

  def index: Int = {
    val regexp = "[\\D\\s]+(\\d+)".r
    id match {
      case regexp(x) => x.toInt
      case _ => 0
    }
  }

  def compare(that: Room): Int = {
    // TODO a virer apres Devoxx FR 2016
    // Hack for Devoxx France => I cannot change the Room IDs so I fix the order in an IndexedSeq here
    if (Room.fixedOrderForRoom.indexOf(this.id) < Room.fixedOrderForRoom.indexOf(that.id)) {
      return -1
    }
    if (Room.fixedOrderForRoom.indexOf(this.id) > Room.fixedOrderForRoom.indexOf(that.id)) {
      return 1
    }
    return 0
  }
}

object Room {
  implicit val roomFormat = Json.format[Room]

  val OTHER = Room("other_room", "Other room", 100, "sans objet", "")

  val allAsId = ConferenceDescriptor.ConferenceRooms.allRooms.map(a => (a.id, a.name)).toSeq.sorted

  def parse(roomId: String): Room = {
    ConferenceDescriptor.ConferenceRooms.allRooms.find(r => r.id == roomId).getOrElse(OTHER)
  }

  // TODO à virer apres Devoxx FR 2016
  val fixedOrderForRoom = IndexedSeq("a_hall",
    "b_amphi",
    "c_maillot",
    "d_par241",
    "f_neu251",
    "e_neu252",
    "par242AB",
    "par242A",
    "par242AT",
    "par242B",
    "par242BT",
    "par243",
    "neu253",
    "neu253_t",
    "par243_t",
    "par201",
    "par202_203",
    "par204",
    "par221M-222M",
    "par224M-225M",
    "neu_232_232",
    "neu_234_235",
    "neu_212_213",
    "x_hall_a"
  )
def createRoom(id: String, name: String, capacity: Int, setup: String, recorded: String):Room ={
  Room(id,name,capacity,setup,recorded)

}
 def unapplyRoom(r:Room): Option[(String,String,Int,String,String)] ={
   Some(r.id,r.name,r.capacity,r.setup,r.recorded)

 }
  val RoomForm = play.api.data.Form(mapping(
    "id" -> text,
    "name" -> text,
    "capacity" -> number,
    "setup" -> text,
    "recorded" -> text

  )(createRoom)(unapplyRoom))

  def saveroom(room: Room): String = Redis.pool.withClient {
    client =>
      val roomupdate=room.copy(id= generateId())
      val json = Json.toJson(roomupdate).toString()

      val roomId = roomupdate.id
      val tx = client.multi()
      tx.hset("Room", roomupdate.id, json)
      tx.exec()
      roomId
  }

  def allRoom: Map[String, Option[Room]] = Redis.pool.withClient {
    client =>
      client.hgetAll("Room").map {
        case (key: String, valueJson: String) =>
          (key, Json.parse(valueJson).asOpt[Room])
      }
  }

  def deleteRoom(roomid: String) = Redis.pool.withClient {
    client =>
      client.hdel("Room",roomid)
  }
  def findRoomByUUID( roomid: String): Option[Room] = Redis.pool.withClient {
    client =>
      client.hget("Room",roomid ).flatMap {
        json: String =>
          Json.parse(json).validate[Room].fold(invalid => {
            play.Logger.error("Invalid json format for slot, unable to unmarshall " + ZapJson.showError(invalid))
            None
          }, validSlot => Some(validSlot))
      }
  }
  def updateRoom(roomid: String, room: Room) = Redis.pool.withClient {
    client =>
      val jsonSlot = Json.stringify(Json.toJson(room.copy(id = roomid)))
      client.hset("Room",roomid, jsonSlot)
  }

  def generateId(): String = Redis.pool.withClient {
    implicit client =>
      val newId = RandomStringUtils.randomAlphabetic(3).toUpperCase + "-" + RandomStringUtils.randomNumeric(4)
      if (client.hexists("Room", newId)) {
        play.Logger.of("room").warn(s"room ID collision with $newId")
        generateId()
      } else {
        newId
      }
  }

}

case class SlotBreak(id: String, nameEN: String, nameFR: String, room: Room)

object SlotBreak {
  implicit val slotBreakFormat = Json.format[SlotBreak]

  def createsBreak(id: String, nameEN: String, nameFR: String, room: Room)
  : SlotBreak = {
    SlotBreak(id, nameEN,nameFR,room)
  }

  def unapplyBreak(s: SlotBreak): Option[(String, String, String,Room)] = {

    Some(s.id, s.nameFR, s.nameEN,  s.room)
  }
}

case class Slot(id: String, name: String, day: String, from: DateTime, to: DateTime, room: Room,
                proposal: Option[Proposal], break: Option[SlotBreak]) {
  override def toString: String = {
    s"Slot[$id] hasProposal=${proposal.isDefined} isBreak=${break.isDefined}"
  }

  def parleysId: String = {
    ConferenceDescriptor.current().eventCode + "_" + from.toString("dd") + "_" + room.id + "_" + from.toString("HHmm")
  }

  def notAllocated: Boolean = {
    break.isEmpty && proposal.isEmpty
  }

}

object SlotBuilder {

  def apply(name: String, day: String, from: DateTime, to: DateTime, room: Room): Slot = {
    val id = name + "_" + room.id + "_" + day + "_" + from.getDayOfMonth + "_" + from.getHourOfDay + "h" + from.getMinuteOfHour + "_" + to.getHourOfDay + "h" + to.getMinuteOfHour
    Slot(id, name, day, from, to, room, None, None)
  }

  def apply(name: String, day: String, from: DateTime, to: DateTime, room: Room, proposal: Option[Proposal]): Slot = {
    val id = name + "_" + room.id + "_" + day + "_" + from.getDayOfMonth + "_" + from.getHourOfDay + "h" + from.getMinuteOfHour + "_" + to.getHourOfDay + "h" + to.getMinuteOfHour
    Slot(id, name, day, from, to, room, proposal, None)
  }

  def apply(slotBreak: SlotBreak, day: String, from: DateTime, to: DateTime): Slot = {
    val id = slotBreak.id + "_" + day + "_" + from.getDayOfMonth + "_" + from.getHourOfDay + "h" + from.getMinuteOfHour + "_" + to.getHourOfDay + "h" + to.getMinuteOfHour
    Slot(id, slotBreak.nameEN, day, from, to, slotBreak.room, None, Some(slotBreak))
  }
}

// See https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8
object Slot {
  implicit val slotFormat = Json.format[Slot]
  implicit  val read = Json.reads[Speaker]
  implicit  val write = Json.writes[Speaker]

  def byType(proposalType: ProposalType): Seq[Slot] = {
    ConferenceDescriptor.ConferenceSlots.all.filter(s => s.name == proposalType.id)
  }

  def createslot(id: String, name: String, day: String, from: DateTime, to: DateTime, room: String, break: Option[String])
  : Slot = {
break match {
  case Some(a)=> Slot(id, name, day, from, to, Room.findRoomByUUID(room).get, None, findSlotbreakByid(a))
  case  None=>Slot(id, name, day, from, to, Room.findRoomByUUID(room).get, None, None)
}

  }

  def unapplyForm(s: Slot): Option[(String, String, String, DateTime, DateTime, String, Option[String])] = {
s.break match {
  case Some(a)=>Some(s.id, s.name, s.day, s.from, s.to, s.room.id, Some(a.id))
  case None=>Some(s.id, s.name, s.day, s.from, s.to, s.room.id, None)
}

  }

  def generateUUID(email: String): String = {
    Crypto.sign(StringUtils.abbreviate(email.trim().toLowerCase, 255))
  }



  val SlotForm1 = Form(mapping(
    "id" -> text,
    "name" -> text,
    "day" -> nonEmptyText,
    "from" -> jodaDate("HH:mm"),
    "to" -> jodaDate("HH:mm"),
    "room" -> nonEmptyText,
    "break" -> optional(text)
  )(Slot.createslot)(Slot.unapplyForm))

  def getAllRooms: Seq[(String, String)] = {
    val allRooms = Room.allRoom.map( x =>
      x._2 match {
        case Some(a)=>(a.id, a.name)
      }
     )
    allRooms.toSeq.sortBy(_._2)
  }

  def getAllSlotBreaks: Seq[(String, String)] = {
    val allSlotBreak = ConferenceDescriptor.ConferenceSlotBreaks.allSlotBreak.map(x => (x.id, x.nameEN))
    allSlotBreak.toSeq
  }

def findSlotbreakByid(slotBreakid: String):Option[SlotBreak]={
  ConferenceDescriptor.ConferenceSlotBreaks.allSlotBreak.find(slob=>slob.id==slotBreakid)
}
  def changeDate(dat:DateTime , day:String):DateTime = {
   var newdate = dat

    if(day.equals("wednesday")){
      newdate = dat.withDate(2017 , 11 , 15)
      newdate = dat.withDate(2017 , 11 , 15)
    }else
    if(day.equals("thursday")){
      newdate = dat.withDate(2017 , 11 , 16)
      newdate = dat.withDate(2017 , 11 , 16)
    }else
    if(day.equals("tuesday")){
      newdate = dat.withDate(2017 , 11 , 14)
      newdate = dat.withDate(2017 , 11 , 14)
    }
    newdate
  }

  def saveslot(slot:Slot): String = Redis.pool.withClient {
    client =>
   var f = Slot.changeDate(slot.from , slot.day)
      var t = Slot.changeDate(slot.to , slot.day)
      val newid = slot.name + "_" + slot.room.id + "_" + slot.day + "_" + f.getDayOfMonth + "_" + f.getHourOfDay + "h" + f.getMinuteOfHour + "_" + t.getHourOfDay + "h" + t.getMinuteOfHour
      val slotupdate=slot.copy(id= newid , from = Slot.changeDate(slot.from , slot.day) , to = Slot.changeDate(slot.to , slot.day)
      )

     /*val jsvalue: JsValue = Json.obj(
        "id"->slotupdate.id,
    "name"-> slotupdate.name,
    "day"->slotupdate.day,
    "from"-> slotupdate.from,
    "to"-> slotupdate.to,
    "room"-> Json.obj("id"->slotupdate.room.id),
      "break" -> Json.obj("id"->slotupdate.break.get.id)

      )
      val json1 = Json.toJson(jsvalue).toString()*/
      val json = Json.toJson(slotupdate).toString()

      val slotId = slot.id
      val tx = client.multi()
      tx.hset("Slot", slotupdate.id, json)
      tx.exec()
      slotId
  }

  def allSlot: Map[String, Option[Slot]] = Redis.pool.withClient {
    client =>
      client.hgetAll("Slot").map {
        case (key: String, valueJson: String) =>

          (key, Json.parse(valueJson).asOpt[Slot])
      }
  }

  def deleteSlot(slotID: String) = Redis.pool.withClient {
    client =>
      client.hdel("Slot",slotID)
  }
  def findSlotByUUID( slotid: String): Option[Slot] = Redis.pool.withClient {
    client =>
      client.hget("Slot", slotid).flatMap {
        json: String =>
          Json.parse(json).validate[Slot].fold(invalid => {
            play.Logger.error("Invalid json format for slot, unable to unmarshall " + ZapJson.showError(invalid))
            None
          }, validSlot => Some(validSlot))
      }
  }
  def updateSlot(slotID: String, slot: Slot) = Redis.pool.withClient {
    client =>
      val jsonSlot = Json.stringify(Json.toJson(slot.copy(id = slotID)))
      client.hset("Slot",slotID, jsonSlot)
  }

  def generateId(): String = Redis.pool.withClient {
    implicit client =>
      val newId = RandomStringUtils.randomAlphabetic(3).toUpperCase + "-" + RandomStringUtils.randomNumeric(4)
      if (client.hexists("Slot", newId)) {
        play.Logger.of("slot").warn(s"slot ID collision with $newId")
        generateId()
      } else {
        newId
      }
  }
}
