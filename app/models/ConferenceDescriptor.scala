package models

import java.util.Locale

import org.joda.time.{DateTime, DateTimeZone, Period}
import play.api.Play

/**
  * ConferenceDescriptor.
  * This might be the first file to look at, and to customize.
  * Idea behind this file is to try to collect all configurable parameters for a conference.
  *
  * For labels, please do customize messages and messages.fr
  *
  * Note from Nicolas : the first version of the CFP was much more "static" but hardly configurable.
  *
  * @author Frederic Camblor, BDX.IO 2014
  */

case class ConferenceUrls(faq: String, registration: String,confWebsite: String, cfpHostname: String){
  def cfpURL:String={
    if(Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)){
      s"https://$cfpHostname"
    }else{
      s"http://$cfpHostname"
    }
  }

}


case class ConferenceTiming(
                             datesI18nKey: String,
                             speakersPassDuration: Integer,
                             preferredDayEnabled: Boolean,
                             firstDayFr: String,
                             firstDayEn: String,
                             datesFr: String,
                             datesEn: String,
                             cfpOpenedOn: DateTime,
                             cfpClosedOn: DateTime,
                             scheduleAnnouncedOn: DateTime,
                             days:Iterator[DateTime]
                           )

case class ConferenceSponsor(showSponsorProposalCheckbox: Boolean, sponsorProposalType: ProposalType = ProposalType.UNKNOWN)

case class TrackDesc(id: String, imgSrc: String, i18nTitleProp: String, i18nDescProp: String)

case class ProposalConfiguration(id: String, slotsCount: Int,
                                 givesSpeakerFreeEntrance: Boolean,
                                 freeEntranceDisplayed: Boolean,
                                 htmlClass: String,
                                 hiddenInCombo: Boolean = false,
                                 chosablePreferredDay: Boolean = false,
                                 impliedSelectedTrack: Option[Track] = None)

object ProposalConfiguration {

  val UNKNOWN = ProposalConfiguration(id = "unknown", slotsCount = 0, givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false,
    htmlClass = "", hiddenInCombo = true, chosablePreferredDay = false)

  def parse(propConf: String): ProposalConfiguration = {
    ConferenceDescriptor.ConferenceProposalConfigurations.ALL.find(p => p.id == propConf).getOrElse(ProposalConfiguration.UNKNOWN)
  }

  def totalSlotsCount = ConferenceDescriptor.ConferenceProposalConfigurations.ALL.map(_.slotsCount).sum

  def isDisplayedFreeEntranceProposals(pt: ProposalType): Boolean = {
    ConferenceDescriptor.ConferenceProposalConfigurations.ALL.filter(p => p.id == pt.id).map(_.freeEntranceDisplayed).headOption.getOrElse(false)
  }

  def getProposalsImplyingATrackSelection = {
    ConferenceDescriptor.ConferenceProposalConfigurations.ALL.filter(p => p.impliedSelectedTrack.nonEmpty)
  }

  def getHTMLClassFor(pt: ProposalType): String = {
    ConferenceDescriptor.ConferenceProposalConfigurations.ALL.filter(p => p.id == pt.id).map(_.htmlClass).headOption.getOrElse("unknown")
  }

  def isChosablePreferredDaysProposals(pt: ProposalType): Boolean = {
    ConferenceDescriptor.ConferenceProposalConfigurations.ALL.filter(p => p.id == pt.id).map(_.chosablePreferredDay).headOption.getOrElse(false)
  }

  def doesProposalTypeGiveSpeakerFreeEntrance(pt: ProposalType): Boolean = {
    ConferenceDescriptor.ConferenceProposalConfigurations.ALL.filter(p => p.id == pt.id).map(_.givesSpeakerFreeEntrance).headOption.getOrElse(false)
  }
}

case class ConferenceDescriptor(eventCode: String,
                                confUrlCode: String,
                                frLangEnabled: Boolean,
                                fromEmail: String,
                                committeeEmail: String,
                                bccEmail: Option[String],
                                bugReportRecipient: String,
                                conferenceUrls: ConferenceUrls,
                                timing: ConferenceTiming,
                                hosterName: String,
                                hosterWebsite: String,
                                hashTag: String,
                                conferenceSponsor: ConferenceSponsor,
                                locale: List[Locale],
                                localisation: String,
                                notifyProposalSubmitted:Boolean,
                                maxProposalSummaryCharacters:Int=1200
                               )

object ConferenceDescriptor {

  /**
    * TODO configure here the kind of talks you will propose
    */
  object ConferenceProposalTypes {
    val CONF = ProposalType(id = "conf", label = "conf.label")

    val UNI = ProposalType(id = "uni", label = "uni.label")

    val TIA = ProposalType(id = "tia", label = "tia.label")

    val LAB = ProposalType(id = "lab", label = "lab.label")

    val QUICK = ProposalType(id = "quick", label = "quick.label")

    val BOF = ProposalType(id = "bof", label = "bof.label")

    val KEY = ProposalType(id = "key", label = "key.label")

    val START = ProposalType(id = "start", label = "start.label")

    val IGNITE = ProposalType(id = "ignite", label = "ignite.label")

    val OTHER = ProposalType(id = "other", label = "other.label")

    val ALL = List(CONF, UNI, TIA, LAB, QUICK, BOF, KEY, START, IGNITE, OTHER)

    def valueOf(id: String): ProposalType = id match {
      case "conf" => CONF
      case "uni" => UNI
      case "tia" => TIA
      case "lab" => LAB
      case "quick" => QUICK
      case "bof" => BOF
      case "key" => KEY
      case "start" => START
      case "ignite" => IGNITE
      case "other" => OTHER
      case other => OTHER
    }

  }

  object ConferenceProposalConfigurations {
    val CONF = ProposalConfiguration(id = "conf", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.CONF.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val UNI = ProposalConfiguration(id = "uni", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.UNI.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-laptop",
      chosablePreferredDay = true)
    val TIA = ProposalConfiguration(id = "tia", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.TIA.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-legal",
      chosablePreferredDay = true)
    val LAB = ProposalConfiguration(id = "lab", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.LAB.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-beaker",
      chosablePreferredDay = true)
    val QUICK = ProposalConfiguration(id = "quick", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.QUICK.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-fast-forward",
      chosablePreferredDay = true)
    val BOF = ProposalConfiguration(id = "bof", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.BOF.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-group",
      chosablePreferredDay = false)
    val KEY = ProposalConfiguration(id = "key", slotsCount = 1, givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val IGNITE = ProposalConfiguration(id = "ignite", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.IGNITE.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = false)
    val OTHER = ProposalConfiguration(id = "other", slotsCount = 1, givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      hiddenInCombo = true, chosablePreferredDay = false)

    val ALL = List(CONF, UNI, TIA, LAB, QUICK, BOF, KEY, IGNITE, OTHER)

    def doesItGivesSpeakerFreeEntrance(proposalType: ProposalType): Boolean = {
      ALL.filter(_.id == proposalType.id).exists(_.givesSpeakerFreeEntrance)
    }
  }

  object ConferenceTracks {
    val METHOD_ARCHI = Track("method_archi", "method_archi.label")
    val JAVA = Track("java", "java.label")
    val CLOUD = Track("cloud", "cloud.label")
    val SSJ = Track("ssj", "ssj.label")
    val LANG = Track("lang", "lang.label")
    val BIGDATA = Track("bigdata", "bigdata.label")
    val WEB = Track("web", "web.label")
    val FUTURE = Track("future", "future.label")
    val MOBILE = Track("mobile", "mobile.label")

    val UNKNOWN = Track("unknown", "unknown track")
    val ALL = List(METHOD_ARCHI, JAVA, CLOUD, SSJ, LANG, BIGDATA, WEB, FUTURE, MOBILE, UNKNOWN)
  }

  object ConferenceTracksDescription {
    val METHOD_ARCHI = TrackDesc(ConferenceTracks.METHOD_ARCHI.id, "/assets/devoxxpl2017/images/icon_methodology.png", ConferenceTracks.METHOD_ARCHI.label, "track.method_archi.desc")
    val JAVA = TrackDesc(ConferenceTracks.JAVA.id, "/assets/devoxxpl2017/images/icon_javase.png", ConferenceTracks.JAVA.label, "track.java.desc")
    val CLOUD = TrackDesc(ConferenceTracks.CLOUD.id, "/assets/devoxxpl2017/images/icon_cloud.png", ConferenceTracks.CLOUD.label, "track.cloud.desc")
    val SSJ = TrackDesc(ConferenceTracks.SSJ.id, "/assets/devoxxpl2017/images/icon_javaee.png", ConferenceTracks.SSJ.label, "track.ssj.desc")
    val LANG = TrackDesc(ConferenceTracks.LANG.id, "/assets/devoxxpl2017/images/icon_alternative.png", ConferenceTracks.LANG.label, "track.lang.desc")
    val BIGDATA = TrackDesc(ConferenceTracks.BIGDATA.id, "/assets/devoxxpl2017/images/icon_architecture.png", ConferenceTracks.BIGDATA.label, "track.bigdata.desc")
    val WEB = TrackDesc(ConferenceTracks.WEB.id, "/assets/devoxxpl2017/images/icon_web.png", ConferenceTracks.WEB.label, "track.web.desc")
    val FUTURE = TrackDesc(ConferenceTracks.FUTURE.id, "/assets/devoxxpl2017/images/icon_future.png", ConferenceTracks.FUTURE.label, "track.future.desc")
    val MOBILE = TrackDesc(ConferenceTracks.MOBILE.id, "/assets/devoxxpl2017/images/icon_mobile.png", ConferenceTracks.MOBILE.label, "track.mobile.desc")

    val ALL = List(METHOD_ARCHI
      , JAVA
      , CLOUD
      , SSJ
      , LANG
      , BIGDATA
      , WEB
      , FUTURE
      , MOBILE
    )

    def findTrackDescFor(t: Track): TrackDesc = {
      ALL.find(_.id == t.id).head
    }
  }

  // TODO If you want to use the Devoxx Scheduler, you can describe here the list of rooms, with capacity for seats
  object ConferenceRooms {

    // Tip : I use the ID to sort-by on the view per day... So if the exhibition floor id is "aaa" it will be
    // the first column on the HTML Table
    val HALL_EXPO = Room("a_hall", "Exhibition floor", 2500, "special")

    val ROOM1 = Room("room1", "Room 1", 2100, "theatre")
    val ROOM2 = Room("room2", "Room 2", 600, "theatre")
    val ROOM3 = Room("room3", "Room 3", 300, "theatre")
    val ROOM4 = Room("room4", "Room 4", 400, "theatre")
    val ROOM5 = Room("room5", "Room 5", 300, "theatre")

    val allRooms = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM5)

    val allRoomsQuickies = List(ROOM2, ROOM3, ROOM4, ROOM5)

    val allRoomsBof = List(ROOM1, ROOM2, ROOM3, ROOM4)

    val allRoomsTia = List(ROOM1, ROOM2, ROOM5)

    val keynoteRoom = List(ROOM1)

    val FATJRoom = List(ROOM4)

    val allRoomsConf = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM5)
  }

  object ConferenceSlotBreaks {
    val registration = SlotBreak("reg", "Registration", "Accueil", ConferenceRooms.HALL_EXPO)
    val petitDej = SlotBreak("dej", "Breakfast", "Accueil et petit-déjeuner", ConferenceRooms.HALL_EXPO)
    val coffee = SlotBreak("coffee", "Coffee Break", "Pause café", ConferenceRooms.HALL_EXPO)
    val lunch = SlotBreak("lunch", "Lunch", "Pause déjeuner", ConferenceRooms.HALL_EXPO)
    val shortBreak = SlotBreak("chgt", "Break", "Pause courte", ConferenceRooms.HALL_EXPO)
    val exhibition = SlotBreak("exhib", "Exhibition", "Exhibition", ConferenceRooms.HALL_EXPO)
    val meetAndGreet = SlotBreak("meet", "Meet & Greet (Exhibition)", "Exhibition", ConferenceRooms.HALL_EXPO)
    val eveningKeynote = SlotBreak("evKey", "Evening Keynote", "Keynote", ConferenceRooms.ROOM5)
    val closingKeynote = SlotBreak("closeKey", "Closing Keynote (Room 5)", "Keynote", ConferenceRooms.ROOM3)
    val movieSpecial = SlotBreak("movie", "Movie 20:00-22:00 (Room 8)", "Movie", ConferenceRooms.ROOM3)
  }

  object ConferenceSlots {

    // FATJ
    val fatjThursday: List[Slot] = {
      val fatjSlot = ConferenceRooms.FATJRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T18:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T21:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      fatjSlot
    }


    // TOOLS IN ACTION
    val tiaSlotsThursday: List[Slot] = {
      val toolsThursdayAfternoonSlot1 = ConferenceRooms.allRoomsTia.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id, "thursday",
            new DateTime("2017-06-23T18:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T18:40:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }

      val toolsThursdayAfternoonSlot2 = ConferenceRooms.allRoomsTia.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id, "thursday",
            new DateTime("2017-06-23T18:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T19:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r2)
      }

      toolsThursdayAfternoonSlot1 ++ toolsThursdayAfternoonSlot2
    }

    // UNIVERSITY

    // HANDS ON LABS

    // BOFS

    val bofSlotsWednesday: List[Slot] = {
      val bofWednesdayEveningSlot1 = ConferenceRooms.allRoomsBof.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday",
            new DateTime("2017-06-22T19:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T20:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      bofWednesdayEveningSlot1
    }

    // QUICKIES
    val quickiesSlotsWednesday: List[Slot] = {
      val quickiesWednesday1 = ConferenceRooms.allRoomsQuickies.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday",
            new DateTime("2017-06-22T13:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T13:25:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      quickiesWednesday1
    }

    val quickiesSlotsThursday: List[Slot] = {
      val quickiesThursday1 = ConferenceRooms.allRoomsQuickies.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id, "thursday",
            new DateTime("2017-06-23T12:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T12:45:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      quickiesThursday1
    }

    val quickiesSlotsFriday: List[Slot] = {
      val quickiesFriday1 = ConferenceRooms.allRoomsQuickies.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id, "friday",
            new DateTime("2017-06-24T12:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T12:45:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      quickiesFriday1
    }

    // CONFERENCE KEYNOTES
    val keynoteSlotsWednesday: List[Slot] = {
      val keynoteWednesdayWelcome = ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
            new DateTime("2017-06-22T09:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T09:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      val keynoteWednesdaySlot1 = ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
            new DateTime("2017-06-22T09:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T10:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }

      keynoteWednesdayWelcome ++ keynoteWednesdaySlot1
    }

    val keynoteSlotsThursday: List[Slot] = {
      val keynoteThursdaySlot1 = ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, "thursday",
            new DateTime("2017-06-23T17:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T18:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }

      keynoteThursdaySlot1
    }

    val keynoteSlotsFriday: List[Slot] = {
      val keynoteFridaySlot1 = ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, "friday",
            new DateTime("2017-06-24T15:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T16:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      val keynoteFridayClosing = ConferenceRooms.keynoteRoom.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, "friday",
            new DateTime("2017-06-24T16:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T17:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r2)
      }

      keynoteFridaySlot1 ++ keynoteFridayClosing
    }

    // CONFERENCE SLOTS
    val conferenceSlotsWednesday: List[Slot] = {
      val conferenceWednesdaySlot1 = ConferenceRooms.allRoomsConf.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
            new DateTime("2017-06-22T11:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T11:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      val conferenceWednesdaySlot2 = ConferenceRooms.allRoomsConf.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
            new DateTime("2017-06-22T12:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T13:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r2)
      }
      val conferenceWednesdaySlot3 = ConferenceRooms.allRoomsConf.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
            new DateTime("2017-06-22T14:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T14:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r3)
      }
      val conferenceWednesdaySlot4 = ConferenceRooms.allRoomsConf.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
            new DateTime("2017-06-22T15:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T16:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r4)
      }
      val conferenceWednesdaySlot5 = ConferenceRooms.allRoomsConf.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
            new DateTime("2017-06-22T16:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T17:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r5)
      }
      val conferenceWednesdaySlot6 = ConferenceRooms.allRoomsConf.map {
        r6 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
            new DateTime("2017-06-22T17:40:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-22T18:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r6)
      }
      conferenceWednesdaySlot1 ++ conferenceWednesdaySlot2 ++ conferenceWednesdaySlot3 ++ conferenceWednesdaySlot4 ++ conferenceWednesdaySlot5 ++ conferenceWednesdaySlot6
    }

    val conferenceSlotsThursday: List[Slot] = {

      val conferenceThursdaySlot0 = ConferenceRooms.allRoomsConf.map {
        r0 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
            "thursday",
            new DateTime("2017-11-10T09:30:00.000+01:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-11-10T10:30:00.000+01:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r0)
      }
      val conferenceThursdaySlot1 = ConferenceRooms.allRoomsConf.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T09:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T09:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      val conferenceThursdaySlot2 = ConferenceRooms.allRoomsConf.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T10:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T11:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r2)
      }
      val conferenceThursdaySlot3 = ConferenceRooms.allRoomsConf.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T11:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T12:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r3)
      }
      val conferenceThursdaySlot4 = ConferenceRooms.allRoomsConf.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T13:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T14:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r4)
      }
      val conferenceThursdaySlot5 = ConferenceRooms.allRoomsConf.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T14:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T15:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r5)
      }

      val conferenceThursdaySlot6 = ConferenceRooms.allRoomsConf.map {
        r6 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
            new DateTime("2017-06-23T15:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-23T16:40:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r6)
      }

      conferenceThursdaySlot1 ++ conferenceThursdaySlot2 ++ conferenceThursdaySlot3 ++ conferenceThursdaySlot4 ++ conferenceThursdaySlot5 ++ conferenceThursdaySlot6
    }

    val conferenceSlotsFriday: List[Slot] = {
      val conferenceFridaySlot1 = ConferenceRooms.allRoomsConf.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "friday",
            new DateTime("2017-06-24T09:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T09:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r1)
      }
      val conferenceFridaySlot2 = ConferenceRooms.allRoomsConf.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "friday",
            new DateTime("2017-06-24T10:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T11:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r2)
      }
      val conferenceFridaySlot3 = ConferenceRooms.allRoomsConf.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "friday",
            new DateTime("2017-06-24T11:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T12:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r3)
      }
      val conferenceFridaySlot4 = ConferenceRooms.allRoomsConf.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "friday",
            new DateTime("2017-06-24T13:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T14:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r4)
      }
      val conferenceFridaySlot5 = ConferenceRooms.allRoomsConf.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, "friday",
            new DateTime("2017-06-24T14:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
            new DateTime("2017-06-24T15:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")), r5)
      }
      conferenceFridaySlot1 ++ conferenceFridaySlot2 ++ conferenceFridaySlot3 ++ conferenceFridaySlot4 ++ conferenceFridaySlot5
    }

    // Registration, coffee break, lunch etc
    val wednesdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, "wednesday",
        new DateTime("2017-06-22T08:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T09:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "wednesday",
        new DateTime("2017-06-22T10:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T11:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "wednesday",
        new DateTime("2017-06-22T11:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T12:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "wednesday",
        new DateTime("2017-06-22T13:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T14:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "wednesday",
        new DateTime("2017-06-22T14:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T15:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "wednesday",
        new DateTime("2017-06-22T16:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T16:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "wednesday",
        new DateTime("2017-06-22T17:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T17:40:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.meetAndGreet, "wednesday",
        new DateTime("2017-06-22T18:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-22T21:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
    )

    val thursdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, "thursday",
        new DateTime("2017-06-23T08:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T09:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday",
        new DateTime("2017-06-23T09:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T10:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "thursday",
        new DateTime("2017-06-23T11:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T11:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "thursday",
        new DateTime("2017-06-23T12:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T13:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "thursday",
        new DateTime("2017-06-23T14:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T14:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday",
        new DateTime("2017-06-23T15:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T15:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "thursday",
        new DateTime("2017-06-23T16:40:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T17:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "thursday",
        new DateTime("2017-06-23T17:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-23T18:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
    )

    val fridayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, "friday",
        new DateTime("2017-06-24T08:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-24T09:00:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "friday",
        new DateTime("2017-06-24T09:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-24T10:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "friday",
        new DateTime("2017-06-24T11:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-24T11:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "friday",
        new DateTime("2017-06-24T12:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-24T13:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "friday",
        new DateTime("2017-06-24T14:10:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-24T14:30:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "friday",
        new DateTime("2017-06-24T15:20:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")),
        new DateTime("2017-06-24T15:50:00.000+02:00").toDateTime(DateTimeZone.forID("Europe/Paris")))
    )

    // DEVOXX DAYS

    val wednesdaySchedule: List[Slot] = {
      wednesdayBreaks ++ keynoteSlotsWednesday ++ conferenceSlotsWednesday ++ quickiesSlotsWednesday ++ bofSlotsWednesday
    }

    val thursdaySchedule: List[Slot] = {
      thursdayBreaks ++ keynoteSlotsThursday ++ conferenceSlotsThursday ++ quickiesSlotsThursday ++ tiaSlotsThursday ++ fatjThursday
    }

    val fridaySchedule: List[Slot] = {
      fridayBreaks ++ keynoteSlotsFriday ++ conferenceSlotsFriday ++ quickiesSlotsFriday //Ignite zamiast quickies podczas lunchu?
    }

    // COMPLETE DEVOXX
    def all: List[Slot] = {
      wednesdaySchedule ++ thursdaySchedule ++ fridaySchedule
    }
  }

  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  val fromDay = new DateTime().withYear(2017).withMonthOfYear(6).withDayOfMonth(21)
  val toDay = new DateTime().withYear(2017).withMonthOfYear(6).withDayOfMonth(23)

  def current() = ConferenceDescriptor(
    eventCode = "DevoxxPL2017",
    // You will need to update conf/routes files with this code if modified
    confUrlCode = "devoxxpl2017",
    frLangEnabled = false,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("cfp@devoxx.pl"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("cfp@devoxx.pl"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("info@devoxx.pl"),
    conferenceUrls = ConferenceUrls(
      faq = "http://www.devoxx.pl/faq/",
      registration = "http://reg.devoxx.pl",
      confWebsite = "http://www.devoxx.pl/",
      cfpHostname = Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.pl")
    ),
    timing = ConferenceTiming(
      datesI18nKey = "21 - 23 June 2017",
      speakersPassDuration = 5,
      preferredDayEnabled = true,
      firstDayFr = "20 avril",
      firstDayEn = "21st June",
      datesFr = "du 20 au 22 avril 2017",
      datesEn = "from 21st to 23rd June, 2017",
      cfpOpenedOn = DateTime.parse("2017-11-01T00:00:00+02:00"),
      cfpClosedOn = DateTime.parse("2017-03-31T09:00:00+02:00"),
      scheduleAnnouncedOn = DateTime.parse("2017-04-25T00:00:00+02:00"),
      days=dateRange(fromDay,toDay,new Period().withDays(1))
    ),
    hosterName = "Clever-cloud", hosterWebsite = "http://www.clever-cloud.com/#DevoxxPL",
    hashTag = "#DevoxxPL",
    conferenceSponsor = ConferenceSponsor(showSponsorProposalCheckbox = true, sponsorProposalType = ConferenceProposalTypes.CONF)
    ,  List(Locale.ENGLISH)
    , "ICE Congress Centre, Krakow, Poland"
    , notifyProposalSubmitted = true
    , 1200 // French developers tends to be a bit verbose... we need extra space :-)
  )

  def isCFPOpen: Boolean = {
    Play.current.configuration.getBoolean("cfp.isOpen").getOrElse(false)
  }

  def isGoldenTicketActive:Boolean = Play.current.configuration.getBoolean("goldenTicket.active").getOrElse(false)

  def isFavoritesSystemActive:Boolean = Play.current.configuration.getBoolean("cfp.activateFavorites").getOrElse(false)

  def isHTTPSEnabled = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)

}

