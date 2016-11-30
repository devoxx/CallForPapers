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

package models.conference

import models.{Slot, SlotBreak, SlotBuilder}
import org.joda.time.{DateTime, DateTimeZone}

object ConferenceSlotBreaks {
  val registration = SlotBreak("registration", "Registration, Welcome and Breakfast", "Accueil", ConferenceRooms.ROOM1)
  val breakfast = SlotBreak("breakfast", "Breakfast", "Accueil et petit-déjeuner", ConferenceRooms.ROOM1)
  val coffee = SlotBreak("coffee", "Coffee Break", "Pause café", ConferenceRooms.ROOM1)
  val lunch = SlotBreak("lunch", "Lunch", "Pause déjeuner", ConferenceRooms.ROOM1)
  val shortBreak = SlotBreak("break", "Break", "Pause courte", ConferenceRooms.ROOM1)
  val exhibition = SlotBreak("exhib", "Exhibitors' Reception", "Exhibition", ConferenceRooms.ROOM1)
}

object ConferenceSlots {

  private val AmericaLosAngeles = "America/Los_Angeles"

  val TUESDAY = "tuesday"
  val WEDNESDAY = "wednesday"
  val THURSDAY = "thursday"

  private val TUE_DATE = "2017-03-21T"
  private val WED_DATE = "2017-03-22T"
  private val THU_DATE = "2017-03-23T"

  private val MINSEC = ":00.000-07:00"

  // TOOLS IN ACTION
  val tiaSlotsTuesday: List[Slot] = {

    val toolsTuesdayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, TUESDAY,
          new DateTime(TUE_DATE + "17:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "18:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    toolsTuesdayAfternoonSlot1
  }

  val tiaSlotsWednesday: List[Slot] = {

    val toolsWednesdayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, WEDNESDAY,
          new DateTime(WED_DATE + "15:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "16:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val toolsWednesdayAfternoonSlot2 = ConferenceRooms.allRoomsTIA.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, WEDNESDAY,
          new DateTime(WED_DATE + "17:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "18:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    toolsWednesdayAfternoonSlot1 ++ toolsWednesdayAfternoonSlot2
  }

  val tiaSlotsThursday: List[Slot] = {

    val toolsThursdayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, THURSDAY,
          new DateTime(THU_DATE + "14:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "15:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val toolsThursdayAfternoonSlot2 = ConferenceRooms.allRoomsTIA.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, THURSDAY,
          new DateTime(THU_DATE + "15:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "16:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    val toolsThursdayAfternoonSlot3 = ConferenceRooms.allRoomsTIA.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, THURSDAY,
          new DateTime(THU_DATE + "16:10" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "16:40" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r3)
    }
    toolsThursdayAfternoonSlot1 ++
    toolsThursdayAfternoonSlot2 ++
    toolsThursdayAfternoonSlot3
  }

  // HANDS ON LABS

  val labsSlotsTuesday: List[Slot] = ConferenceRooms.LabsDayOne.map {
    r1 => {
      SlotBuilder(ConferenceProposalTypes.LAB.id, TUESDAY,
        new DateTime(TUE_DATE + "14:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
        new DateTime(TUE_DATE + "17:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
  }

  val labsSlotsWednesday: List[Slot] = ConferenceRooms.LabsDayTwoThree.map {
    r1 => {
      SlotBuilder(ConferenceProposalTypes.LAB.id, WEDNESDAY,
        new DateTime(WED_DATE + "14:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
        new DateTime(WED_DATE + "18:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
  }

  val labsSlotsThursday: List[Slot] = ConferenceRooms.LabsDayTwoThree.map {
    r1 => {
      SlotBuilder(ConferenceProposalTypes.LAB.id, THURSDAY,
        new DateTime(THU_DATE + "13:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
        new DateTime(THU_DATE + "16:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
  }

  // BOF SESSIONS

  val bofSlotsTuesday: List[Slot] = {

    val bofTuesdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, TUESDAY,
          new DateTime(TUE_DATE + "19:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "20:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val bofTuesdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, TUESDAY,
          new DateTime(TUE_DATE + "20:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "21:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    bofTuesdayEveningSlot1 ++ bofTuesdayEveningSlot2
  }

  val bofSlotsWednesday: List[Slot] = {

    val bofWednesdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, WEDNESDAY,
          new DateTime(WED_DATE + "20:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "21:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val bofWednesdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, WEDNESDAY,
          new DateTime(WED_DATE + "21:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "22:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    bofWednesdayEveningSlot1 ++
    bofWednesdayEveningSlot2
  }


  // QUICKIES

  val quickiesSlotsTuesday: List[Slot] = {

    val quickiesTuesdayLunch1 = ConferenceRooms.allRoomsQuick.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, TUESDAY,
          new DateTime(TUE_DATE + "12:45" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "13:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val quickiesTuesdayLunch2 = ConferenceRooms.allRoomsQuick.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, TUESDAY,
          new DateTime(TUE_DATE + "13:15" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "13:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    quickiesTuesdayLunch1 ++
    quickiesTuesdayLunch2
  }

  val quickiesSlotsWednesday: List[Slot] = {

    val quickiesWednesdayLunch1 = ConferenceRooms.allRoomsQuick.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, WEDNESDAY,
          new DateTime(WED_DATE + "13:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "13:35" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val quickiesWednesdayLunch2 = ConferenceRooms.allRoomsQuick.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, WEDNESDAY,
          new DateTime(WED_DATE + "13:55" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "14:10" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    quickiesWednesdayLunch1 ++
    quickiesWednesdayLunch2
  }

  // CONFERENCE KEYNOTES

  val keynoteSlotsTuesday: List[Slot] = {

    val keynoteTuesday1 = ConferenceRooms.keynoteRoom.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, TUESDAY,
          new DateTime(TUE_DATE + "09:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "09:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val keynoteTuesday2 = ConferenceRooms.keynoteRoom.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, TUESDAY,
          new DateTime(TUE_DATE + "09:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "10:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    keynoteTuesday1 ++
    keynoteTuesday2
  }

  val keynoteSlotsWednesday: List[Slot] = {

    val keynoteWednesdaySlot1 = ConferenceRooms.eveningKeynoteRoom.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, WEDNESDAY,
          new DateTime(WED_DATE + "09:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "09:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }

    keynoteWednesdaySlot1
  }

  val keynoteSlotsThursday: List[Slot] = {

    val keynoteThursdaySlot1 = ConferenceRooms.eveningKeynoteRoom.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, THURSDAY,
          new DateTime(THU_DATE + "09:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "09:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }

    val keynoteThursdaySlot2 = ConferenceRooms.eveningKeynoteRoom.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, THURSDAY,
          new DateTime(THU_DATE + "17:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "17:45" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }

    keynoteThursdaySlot1 ++
    keynoteThursdaySlot2
  }


  // CONFERENCE SLOTS

  val conferenceSlotsTuesday: List[Slot] = {

    val conferenceTuesdaySlot1 = ConferenceRooms.morningConfRooms.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, TUESDAY,
          new DateTime(TUE_DATE + "10:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "11:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val conferenceTuesdaySlot2 = ConferenceRooms.morningConfRooms.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, TUESDAY,
          new DateTime(TUE_DATE + "11:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "12:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }

    // -------------- Lunch

    val conferenceTuesdaySlot3 = ConferenceRooms.afternoonConfRoomsDayOne.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, TUESDAY,
          new DateTime(TUE_DATE + "14:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "14:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r3)
    }
    val conferenceTuesdaySlot4 = ConferenceRooms.afternoonConfRoomsDayOne.map {
      r4 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, TUESDAY,
          new DateTime(TUE_DATE + "15:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "15:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r4)
    }
    val conferenceTuesdaySlot5 = ConferenceRooms.afternoonConfRoomsDayOne.map {
      r5 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, TUESDAY,
          new DateTime(TUE_DATE + "16:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(TUE_DATE + "17:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r5)
    }

    conferenceTuesdaySlot1 ++
    conferenceTuesdaySlot2 ++
    conferenceTuesdaySlot3 ++
    conferenceTuesdaySlot4 ++
    conferenceTuesdaySlot5
  }

  val conferenceSlotsWednesday: List[Slot] = {

    val conferenceWednesdaySlot1 = ConferenceRooms.morningConfRooms.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
          new DateTime(WED_DATE + "10:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "11:10" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val conferenceWednesdaySlot2 = ConferenceRooms.morningConfRooms.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
          new DateTime(WED_DATE + "11:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "12:10" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    val conferenceWednesdaySlot3 = ConferenceRooms.morningConfRooms.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
          new DateTime(WED_DATE + "12:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "13:10" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r3)
    }

    // -------------- Lunch

    val conferenceWednesdaySlot4 = ConferenceRooms.afternoonConfRoomsDayTwoThree.map {
      r4 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
          new DateTime(WED_DATE + "14:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "15:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r4)
    }
    val conferenceWednesdaySlot5 = ConferenceRooms.afternoonConfRoomsDayTwoThree.map {
      r5 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
          new DateTime(WED_DATE + "16:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "17:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r5)
    }
      conferenceWednesdaySlot1 ++
      conferenceWednesdaySlot2 ++
      conferenceWednesdaySlot3 ++
      conferenceWednesdaySlot4 ++
      conferenceWednesdaySlot5
  }

  val conferenceSlotsThursday: List[Slot] = {

    val conferenceThursdaySlot1 = ConferenceRooms.morningConfRooms.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
          new DateTime(THU_DATE + "10:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "10:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r1)
    }
    val conferenceThursdaySlot2 = ConferenceRooms.morningConfRooms.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
          new DateTime(THU_DATE + "11:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "11:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r2)
    }
    val conferenceThursdaySlot3 = ConferenceRooms.morningConfRooms.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
          new DateTime(THU_DATE + "12:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "12:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r3)
    }

    // -------------- Lunch

    val conferenceThursdaySlot4 = ConferenceRooms.afternoonConfRoomsDayTwoThree.map {
      r4 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
          new DateTime(THU_DATE + "13:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(THU_DATE + "14:40" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), r4)
    }

    val toReturn =  conferenceThursdaySlot1 ++
                    conferenceThursdaySlot2 ++
                    conferenceThursdaySlot3 ++
                    conferenceThursdaySlot4

    toReturn
  }

  val igniteSlotsWednesday: List[Slot] = {
    ConferenceRooms.igniteRoom.flatMap {
      room => List(
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, WEDNESDAY,
          new DateTime(WED_DATE + "13:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
          new DateTime(WED_DATE + "14:25" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)), room)
      )
    }
  }

  // Registration, coffee break, lunch etc
  val tuesdayBreaks = List(
    SlotBuilder(ConferenceSlotBreaks.registration, TUESDAY,
      new DateTime(TUE_DATE + "08:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(TUE_DATE + "09:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
    , SlotBuilder(ConferenceSlotBreaks.lunch, TUESDAY,
      new DateTime(TUE_DATE + "12:20" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(TUE_DATE + "14:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
    , SlotBuilder(ConferenceSlotBreaks.coffee, TUESDAY,
      new DateTime(TUE_DATE + "15:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(TUE_DATE + "16:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
    , SlotBuilder(ConferenceSlotBreaks.exhibition, TUESDAY,
      new DateTime(TUE_DATE + "18:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(TUE_DATE + "19:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
  )

  val wednesdayBreaks = List(
    SlotBuilder(ConferenceSlotBreaks.registration, WEDNESDAY,
      new DateTime(WED_DATE + "08:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(WED_DATE + "09:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
    , SlotBuilder(ConferenceSlotBreaks.lunch, WEDNESDAY,
      new DateTime(WED_DATE + "13:10" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(WED_DATE + "14:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
    , SlotBuilder(ConferenceSlotBreaks.coffee, WEDNESDAY,
      new DateTime(WED_DATE + "16:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(WED_DATE + "16:30" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
  )

  val thursdayBreaks = List(
    SlotBuilder(ConferenceSlotBreaks.breakfast, THURSDAY,
      new DateTime(THU_DATE + "08:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(THU_DATE + "09:00" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
    , SlotBuilder(ConferenceSlotBreaks.lunch, WEDNESDAY,
      new DateTime(THU_DATE + "12:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)),
      new DateTime(THU_DATE + "13:50" + MINSEC).toDateTime(DateTimeZone.forID(AmericaLosAngeles)))
  )

  // DEVOXX US DAYS

  val tuesdaySchedule: List[Slot] = {
      tuesdayBreaks ++
      quickiesSlotsTuesday ++
      conferenceSlotsTuesday ++
      keynoteSlotsTuesday ++
      tiaSlotsTuesday ++
      labsSlotsTuesday ++
      bofSlotsTuesday
  }

  val wednesdaySchedule: List[Slot] = {
      wednesdayBreaks ++
      keynoteSlotsWednesday ++
      conferenceSlotsWednesday ++
      quickiesSlotsWednesday ++
      bofSlotsWednesday ++
      labsSlotsWednesday ++
      tiaSlotsWednesday ++
      igniteSlotsWednesday
  }

  val thursdaySchedule: List[Slot] = {
      thursdayBreaks ++
      keynoteSlotsThursday ++
      conferenceSlotsThursday ++
      tiaSlotsThursday ++
      labsSlotsThursday
  }

  // Complete DEVOXX US Schedule
  def all: List[Slot] = {
      tuesdaySchedule ++
      wednesdaySchedule ++
      thursdaySchedule
  }
}