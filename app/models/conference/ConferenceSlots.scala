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
  val registration = SlotBreak("reg", "Registration, Welcome and Breakfast", "Accueil", ConferenceRooms.HALL_EXPO)
  val petitDej = SlotBreak("dej", "Breakfast", "Accueil et petit-déjeuner", ConferenceRooms.HALL_EXPO)
  val coffee = SlotBreak("coffee", "Coffee Break", "Pause café", ConferenceRooms.HALL_EXPO)
  val lunch = SlotBreak("lunch", "Lunch", "Pause déjeuner", ConferenceRooms.HALL_EXPO)
  val shortBreak = SlotBreak("chgt", "Break", "Pause courte", ConferenceRooms.HALL_EXPO)
  val exhibition = SlotBreak("exhib", "Exhibition", "Exhibition", ConferenceRooms.HALL_EXPO)
  val meetAndGreet = SlotBreak("meet", "Meet & Greet (Exhibition) - Evening Keynote 19:00-19:30", "Exhibition", ConferenceRooms.HALL_EXPO)
  val eveningKeynote = SlotBreak("evKey", "Evening Keynote", "Keynote", ConferenceRooms.ROOM8)
  val closingKeynote = SlotBreak("closeKey", "Closing Keynote", "Keynote", ConferenceRooms.ROOM8)
  val movieSpecial = SlotBreak("movie", "Closing keynote 19:00-19:30 - Movie 20:00-22:00", "Movie", ConferenceRooms.HALL_EXPO)
  val noxx = SlotBreak("noxx", "Noxx party", "Soirée au Noxx", ConferenceRooms.HALL_EXPO)
}

object ConferenceSlots {

  // TOOLS IN ACTION

  val tiaSlotsMonday: List[Slot] = {

    val toolsMondayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, "monday", new DateTime("2016-11-07T16:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T17:15:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val toolsMondayAfternoonSlot2 = ConferenceRooms.allRoomsTIA.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, "monday", new DateTime("2016-11-07T17:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T17:55:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val toolsMondayAfternoonSlot3 = ConferenceRooms.allRoomsTIA.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, "monday", new DateTime("2016-11-07T18:05:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T18:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    toolsMondayAfternoonSlot1 ++ toolsMondayAfternoonSlot2 ++ toolsMondayAfternoonSlot3
  }

  val tiaSlotsTuesday: List[Slot] = {

    val toolsTuesdayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday", new DateTime("2016-11-08T16:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T17:15:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val toolsTuesdayAfternoonSlot2 = ConferenceRooms.allRoomsTIA.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday", new DateTime("2016-11-08T17:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T17:55:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val toolsTuesdayAfternoonSlot3 = ConferenceRooms.allRoomsTIA.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday", new DateTime("2016-11-08T18:05:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T18:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    toolsTuesdayAfternoonSlot1 ++ toolsTuesdayAfternoonSlot2 ++ toolsTuesdayAfternoonSlot3
  }

  // HANDS ON LABS

  val labsSlotsMonday: List[Slot] = {

    val labsMondayMorning = ConferenceRooms.allRoomsLabs.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.LAB.id, "monday", new DateTime("2016-11-07T09:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T12:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val labsMondayAfternoon = ConferenceRooms.allRoomsLabs.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.LAB.id, "monday", new DateTime("2016-11-07T13:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T16:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    labsMondayMorning ++ labsMondayAfternoon
  }

  val labsSlotsTuesday: List[Slot] = {

    val labsTuesdayMorning = ConferenceRooms.allRoomsLabs.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday", new DateTime("2016-11-08T09:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T12:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val labsTuesdayAfternoon = ConferenceRooms.allRoomsLabs.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday", new DateTime("2016-11-08T13:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T16:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    labsTuesdayMorning ++ labsTuesdayAfternoon
  }

  val labsSlotsWednesday: List[Slot] = {

    val labsWednesdayAfternoon = ConferenceRooms.oneRoomLabs.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.LAB.id, "wednesday", new DateTime("2016-11-09T14:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T17:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    labsWednesdayAfternoon
  }

  val labsSlotsThursday: List[Slot] = {

    val labsThursdayAfternoon = ConferenceRooms.oneRoomLabs.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.LAB.id, "thursday", new DateTime("2016-11-10T14:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T17:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    labsThursdayAfternoon
  }

  // BOFS

  val bofSlotsMonday: List[Slot] = {

    val bofMondayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "monday", new DateTime("2016-11-07T19:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val bofMondayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "monday", new DateTime("2016-11-07T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val bofMondayEveningSlot3 = ConferenceRooms.allRoomsBOF.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "monday", new DateTime("2016-11-07T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-07T22:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    bofMondayEveningSlot1 ++ bofMondayEveningSlot2 ++ bofMondayEveningSlot3
  }

  val bofSlotsTuesday: List[Slot] = {

    val bofTuesdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "tuesday", new DateTime("2016-11-08T19:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val bofTuesdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "tuesday", new DateTime("2016-11-08T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val bofTuesdayEveningSlot3 = ConferenceRooms.allRoomsBOF.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "tuesday", new DateTime("2016-11-08T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T22:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    bofTuesdayEveningSlot1 ++ bofTuesdayEveningSlot2 ++ bofTuesdayEveningSlot3
  }

  val bofSlotsWednesday: List[Slot] = {

    val bofWednesdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday", new DateTime("2016-11-09T19:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val bofWednesdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday", new DateTime("2016-11-09T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val bofWednesdayEveningSlot3 = ConferenceRooms.allRoomsBOF.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday", new DateTime("2016-11-09T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T22:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    bofWednesdayEveningSlot1 ++ bofWednesdayEveningSlot2 ++ bofWednesdayEveningSlot3
  }

  val bofSlotsThursday: List[Slot] = {

    val bofThursdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "thursday", new DateTime("2016-11-10T19:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val bofThursdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "thursday", new DateTime("2016-11-10T20:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val bofThursdayEveningSlot3 = ConferenceRooms.allRoomsBOF.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.BOF.id, "thursday", new DateTime("2016-11-10T21:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T22:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    bofThursdayEveningSlot1 ++ bofThursdayEveningSlot2 ++ bofThursdayEveningSlot3
  }


  // QUICKIES

  val quickiesSlotsWednesday: List[Slot] = {

    val quickiesWednesdayLunch1 = ConferenceRooms.allRoomsQuick.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday", new DateTime("2016-11-09T13:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val quickiesWednesdayLunch2 = ConferenceRooms.allRoomsQuick.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday", new DateTime("2016-11-09T13:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    quickiesWednesdayLunch1 ++ quickiesWednesdayLunch2
  }

  val quickiesSlotsThursday: List[Slot] = {

    val quickiesThursdayLunch1 = ConferenceRooms.allRoomsQuick.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, "thursday", new DateTime("2016-11-10T13:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val quickiesThursdayLunch2 = ConferenceRooms.allRoomsQuick.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.QUICK.id, "thursday", new DateTime("2016-11-10T13:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    quickiesThursdayLunch1 ++ quickiesThursdayLunch2
  }

  // CONFERENCE KEYNOTES

  val keynoteSlotsWedneday: List[Slot] = {

    ConferenceRooms.keynoteRoom.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
          new DateTime("2016-11-09T09:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")),
          new DateTime("2016-11-09T11:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
  }

  val keynoteSlotsThursday: List[Slot] = {

    val keynoteThursdaySlot1 = ConferenceRooms.eveningKeynoteRoom.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.KEY.id, "thursday",
          new DateTime("2016-11-10T19:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")),
          new DateTime("2016-11-10T19:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }

    keynoteThursdaySlot1
  }

  // CONFERENCE SLOTS

  val conferenceSlotsWedneday: List[Slot] = {

    val conferenceWednesdaySlot1 = ConferenceRooms.allRoomsConf.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday", new DateTime("2016-11-09T12:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val conferenceWednesdaySlot2 = ConferenceRooms.allRoomsConf.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday", new DateTime("2016-11-09T14:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T15:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val conferenceWednesdaySlot3 = ConferenceRooms.allRoomsConf.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday", new DateTime("2016-11-09T15:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T16:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    val conferenceWednesdaySlot4 = ConferenceRooms.allRoomsConf.map {
      r4 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday", new DateTime("2016-11-09T16:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T17:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r4)
    }
    val conferenceWednesdaySlot5 = ConferenceRooms.allRoomsConf.map {
      r5 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday", new DateTime("2016-11-09T17:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T18:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r5)
    }
    conferenceWednesdaySlot1 ++ conferenceWednesdaySlot2 ++ conferenceWednesdaySlot3 ++ conferenceWednesdaySlot4 ++ conferenceWednesdaySlot5
  }

  val conferenceSlotsThursday: List[Slot] = {

    val conferenceThursdaySlot0 = ConferenceRooms.allRoomsConf.map {
      r0 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T09:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T10:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r0)
    }
    val conferenceThursdaySlot1 = ConferenceRooms.allRoomsConf.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T10:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T11:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val conferenceThursdaySlot2 = ConferenceRooms.allRoomsConf.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T12:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val conferenceThursdaySlot3 = ConferenceRooms.allRoomsConf.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T14:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T15:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    val conferenceThursdaySlot4 = ConferenceRooms.allRoomsConf.map {
      r4 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T15:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T16:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r4)
    }


    //      val conferenceThursdaySlot5 = ConferenceRooms.allRoomsConf.filterNot(_.id == "room3").map {
    //        r5 =>
    //          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T16:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T17:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r5)
    //      } ++ List(SlotBuilder(ConferenceProposalTypes.START.id, "thursday", new DateTime("2016-11-10T16:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T17:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), ConferenceRooms.ROOM3),
    //        SlotBuilder(ConferenceProposalTypes.START.id, "thursday", new DateTime("2016-11-10T17:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T17:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), ConferenceRooms.ROOM3))

    val conferenceThursdaySlot6 = ConferenceRooms.allRoomsConf.map {
      r6 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday", new DateTime("2016-11-10T17:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T18:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r6)
    }

    val toReturn = conferenceThursdaySlot0 ++ conferenceThursdaySlot1 ++ conferenceThursdaySlot2 ++ conferenceThursdaySlot3 ++ conferenceThursdaySlot4 ++ conferenceThursdaySlot6

    toReturn

  }
  // ROOM4, ROOM5, ROOM8, ROOM9
  val conferenceSlotsFriday: List[Slot] = {

    val conferenceFridaySlot1 = ConferenceRooms.fridayRoomsConf.map {
      r1 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "friday", new DateTime("2016-11-11T09:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-11T10:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r1)
    }
    val conferenceFridaySlot2 = ConferenceRooms.fridayRoomsConf.map {
      r2 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "friday", new DateTime("2016-11-11T10:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-11T11:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r2)
    }
    val conferenceFridaySlot3 = ConferenceRooms.fridayRoomsConf.map {
      r3 =>
        SlotBuilder(ConferenceProposalTypes.CONF.id, "friday", new DateTime("2016-11-11T11:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-11T12:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), r3)
    }
    conferenceFridaySlot1 ++ conferenceFridaySlot2 ++ conferenceFridaySlot3
  }

  // Ignite slots
  val igniteSlotsWednesday: List[Slot] = {
    ConferenceRooms.igniteRoom.flatMap {
      room => List(
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:05:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:05:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:15:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:15:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:20:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:20:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T13:55:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "wednesday", new DateTime("2016-11-09T13:55:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T14:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room)
      )
    }
  }

  val igniteSlotsThursday: List[Slot] = {
    ConferenceRooms.igniteRoom.flatMap {
      room => List(
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:05:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:05:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:15:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:15:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:20:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:20:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:25:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:45:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T13:55:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room),
        SlotBuilder(ConferenceProposalTypes.IGNITE.id, "thursday", new DateTime("2016-11-10T13:55:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T14:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), room)
      )
    }
  }

  // Registration, coffee break, lunch etc
  val tuesdayBreaks = List(
    SlotBuilder(ConferenceSlotBreaks.registration, "tuesday", new DateTime("2016-11-08T08:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T09:30:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.lunch, "tuesday", new DateTime("2016-11-08T12:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T13:30:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.coffee, "tuesday", new DateTime("2016-11-08T16:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T16:45:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.exhibition, "tuesday", new DateTime("2016-11-08T18:35:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-08T20:00:00.000+01:00"))
  )
  val wednesdayBreaks = List(
    SlotBuilder(ConferenceSlotBreaks.registration, "wednesday", new DateTime("2016-11-09T08:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T09:30:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.coffee, "wednesday", new DateTime("2016-11-09T11:40:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T12:00:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.lunch, "wednesday", new DateTime("2016-11-09T13:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T14:00:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.coffee, "wednesday", new DateTime("2016-11-09T16:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T16:40:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.meetAndGreet, "wednesday", new DateTime("2016-11-09T18:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-09T20:00:00.000+01:00"))
  )

  val thursdayBreaks = List(
    SlotBuilder(ConferenceSlotBreaks.petitDej, "thursday", new DateTime("2016-11-10T08:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T09:30:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday", new DateTime("2016-11-10T10:30:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T10:50:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.lunch, "thursday", new DateTime("2016-11-10T13:00:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T14:00:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday", new DateTime("2016-11-10T16:10:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T16:40:00.000+01:00"))
    , SlotBuilder(ConferenceSlotBreaks.movieSpecial, "thursday", new DateTime("2016-11-10T18:50:00.000+01:00").toDateTime(DateTimeZone.forID("America/Los_Angeles")), new DateTime("2016-11-10T20:00:00.000+01:00"))
  )

  // DEVOXX US DAYS

  val tuesday: List[Slot] = {
    tuesdayBreaks ++ tiaSlotsTuesday ++ labsSlotsTuesday ++ bofSlotsTuesday
  }

  val wednesday: List[Slot] = {
    wednesdayBreaks ++ keynoteSlotsWedneday ++ conferenceSlotsWedneday ++ quickiesSlotsWednesday ++ bofSlotsWednesday ++ labsSlotsWednesday ++ igniteSlotsWednesday
  }

  val thursday: List[Slot] = {
    thursdayBreaks ++ keynoteSlotsThursday ++ conferenceSlotsThursday ++ quickiesSlotsThursday ++ bofSlotsThursday ++ labsSlotsThursday ++ igniteSlotsThursday
  }

  // COMPLETE DEVOXX
  def all: List[Slot] = {
    tuesday ++ wednesday ++ thursday 
  }
}
