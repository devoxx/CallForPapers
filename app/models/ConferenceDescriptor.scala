package models

import java.util.Locale

import controllers.Backoffice
import org.joda.time.{DateTime, DateTimeZone, Period}
import play.api.Play
import play.api.i18n.Messages

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

case class ConferenceUrls(info: String, registration: String, sponsors: String, confWebsite: String, cfpHostname: String) {
  def cfpURL(): String = {
    if (Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)) {
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
                                notifyProposalSubmitted: Boolean,
                                maxProposalSummaryCharacters: Int = 1200)

object ConferenceDescriptor {

  /**
    * TODO configure here the kind of talks you will propose
    */
  object ConferenceProposalTypes {
    val CONF = ProposalType(id = "conf", label = "conf.label")

    val DEEP_DIVE = ProposalType(id = "deep_dive", label = "deep.dive.label")

    val LAB = ProposalType(id = "lab", label = "lab.label")

    val QUICK = ProposalType(id = "quick", label = "quick.label")

    val BOF = ProposalType(id = "bof", label = "bof.label")

    val OPENING_KEY = ProposalType(id = "opening_key", label = "opening.key.label")

    val CLOSING_KEY = ProposalType(id = "closing_key", label = "closing.key.label")

    val IGNITE = ProposalType(id = "ignite", label = "ignite.label")

    val ALL = List(CONF, LAB, DEEP_DIVE, QUICK, BOF, OPENING_KEY, CLOSING_KEY, IGNITE)

    def valueOf(id: String): ProposalType = id match {
      case "conf" => CONF
      case "lab" => LAB
      case "deep_dive" => DEEP_DIVE
      case "quick" => QUICK
      case "bof" => BOF
      case "opening_key" => OPENING_KEY
      case "closing_key" => CLOSING_KEY
      case "ignite" => IGNITE
    }
  }

  object ConferenceProposalConfigurations {
    val CONF = ProposalConfiguration(id = "conf", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.CONF.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val LAB = ProposalConfiguration(id = "lab", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.LAB.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-beaker",
      chosablePreferredDay = true)
    val DEEP_DIVE = ProposalConfiguration(id = "deep_dive", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.DEEP_DIVE.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-anchor",
      chosablePreferredDay = true)
    val QUICK = ProposalConfiguration(id = "quick", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.QUICK.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-fast-forward",
      chosablePreferredDay = true)
    val BOF = ProposalConfiguration(id = "bof", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.BOF.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-group",
      chosablePreferredDay = false)
    val OPENING_KEY = ProposalConfiguration(id = "opening_key", slotsCount = 1, givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val CLOSING_KEY = ProposalConfiguration(id = "closing_key", slotsCount = 1, givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val IGNITE = ProposalConfiguration(id = "ignite", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.IGNITE.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = false)

    val ALL = List(CONF, LAB, QUICK, BOF, OPENING_KEY, CLOSING_KEY, IGNITE)

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
    val GEEK = Track("geek", "geek.label")
    val SECURITY = Track("security", "security.label")
    val ARCHITECTURE = Track("architecture", "architecture.label")
    val UNKNOWN = Track("unknown", "unknown track")
    val ALL = List(METHOD_ARCHI, JAVA, CLOUD, SSJ, LANG, BIGDATA, WEB, GEEK, SECURITY, ARCHITECTURE, UNKNOWN)
  }

  val DEVOXX_CONF_URL_CODE = "devoxxpl"

  object ConferenceTracksDescription {
    val METHOD_ARCHI = TrackDesc(ConferenceTracks.METHOD_ARCHI.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_methodology.png", ConferenceTracks.METHOD_ARCHI.label, "track.method_archi.desc")
    val JAVA = TrackDesc(ConferenceTracks.JAVA.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_javase.png", ConferenceTracks.JAVA.label, "track.java.desc")
    val CLOUD = TrackDesc(ConferenceTracks.CLOUD.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_cloud.png", ConferenceTracks.CLOUD.label, "track.cloud.desc")
    val SSJ = TrackDesc(ConferenceTracks.SSJ.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_javaee.png", ConferenceTracks.SSJ.label, "track.ssj.desc")
    val LANG = TrackDesc(ConferenceTracks.LANG.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_alternative.png", ConferenceTracks.LANG.label, "track.lang.desc")
    val BIGDATA = TrackDesc(ConferenceTracks.BIGDATA.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_bigdata.png", ConferenceTracks.BIGDATA.label, "track.bigdata.desc")
    val WEB = TrackDesc(ConferenceTracks.WEB.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_web.png", ConferenceTracks.WEB.label, "track.web.desc")
    val GEEK = TrackDesc(ConferenceTracks.GEEK.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_geek.png", ConferenceTracks.GEEK.label, "track.geek.desc")

    val SECURITY = TrackDesc(ConferenceTracks.SECURITY.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_security.png", ConferenceTracks.SECURITY.label, "track.security.desc")
    val ARCHITECTURE = TrackDesc(ConferenceTracks.ARCHITECTURE.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_architecture.png", ConferenceTracks.ARCHITECTURE.label, "track.architecture.desc")
    val UNKNOWN = TrackDesc(ConferenceTracks.UNKNOWN.id, "/assets/" + DEVOXX_CONF_URL_CODE + "/images/icon_web.png", ConferenceTracks.UNKNOWN.label, "track.unknown.desc")

    val ALL = List(METHOD_ARCHI
      , JAVA
      , CLOUD
      , SSJ
      , LANG
      , BIGDATA
      , WEB
      , GEEK
      , SECURITY
      , ARCHITECTURE
    )

    def findTrackDescFor(t: Track): TrackDesc = {
      if (ALL.exists(_.id == t.id)) {
        ALL.find(_.id == t.id).head
      } else {
        ConferenceTracksDescription.UNKNOWN
      }
    }
  }

  // TODO If you want to use the Devoxx Scheduler, you can describe here the list of rooms, with capacity for seats
  object ConferenceRooms {

    // Tip : I use the ID to sort-by on the view per day... So if the exhibition floor id is "aaa" it will be
    // the first column on the HTML Table

    val HALL_EXPO = Room("z_hall", "Exhibition floor", 2500, "special")

    val ROOM_1 = Room("a_room_1", "Room 1", 2100, "theatre")
    val ROOM_2 = Room("room_2", "Room 2", 600, "theatre")
    val ROOM_3 = Room("room_3", "Room 3", 400, "theatre")
    val ROOM_4 = Room("room_4", "Room 4", 400, "theatre")
    val ROOM_5 = Room("room_5", "Room 5", 300, "theatre")

    val LAB_ROOM_A = Room("x_lab_room1", "Lab Room A", 50, "classroom")
    val LAB_ROOM_B = Room("x_lab_room2", "Lab Room B", 50, "classroom")

    val keynoteRoom = List(ROOM_1)

    val conferenceRooms = List(ROOM_1, ROOM_2, ROOM_3, ROOM_4, ROOM_5)
    val conferenceRooms2 = List(ROOM_1, ROOM_2, ROOM_3)
    val labRooms = List(LAB_ROOM_A, LAB_ROOM_B)

    val bofRooms = List(ROOM_4, ROOM_5)
    val igniteRooms = List(ROOM_1)
    val quickieRooms = List(ROOM_2, ROOM_3, ROOM_4)

    val allRooms = List(ROOM_1, ROOM_2, ROOM_3, ROOM_4, ROOM_5, LAB_ROOM_A, LAB_ROOM_B, HALL_EXPO)
  }

  object ConferenceSlotBreaks {
    val registrationAndCoffee = SlotBreak("reg", "Registration & Coffee", "Accueil", ConferenceRooms.HALL_EXPO)
    val breakfast = SlotBreak("dej", "Breakfast", "Accueil et petit-déjeuner", ConferenceRooms.HALL_EXPO)
    val coffee = SlotBreak("coffee", "Coffee Break", "Pause café", ConferenceRooms.HALL_EXPO)
    val shortBreak = SlotBreak("short", "Short Coffee Break", "Pause café", ConferenceRooms.HALL_EXPO)
    val lunch = SlotBreak("lunch", "Lunch", "Pause déjeuner", ConferenceRooms.HALL_EXPO)
    val snack = SlotBreak("snack", "Snack Break", "Pause déjeuner", ConferenceRooms.HALL_EXPO)
    val eveningReception = SlotBreak("reception", "Evening Reception", "Evening Reception", ConferenceRooms.HALL_EXPO)
    val eveningReception2 = SlotBreak("reception", "Evening Reception", "Evening Reception", ConferenceRooms.HALL_EXPO)
    val openingKeynote = SlotBreak("openKey", "Opening Keynote", "Keynote", ConferenceRooms.ROOM_1)
    val closingKeynote = SlotBreak("closeKey", "Closing Keynote", "Keynote", ConferenceRooms.ROOM_1)
  }

  object ConferenceSlots {

    // VARIABLE CONSTANTS

    private val WEDNESDAY: String = "wednesday"
    private val THURSDAY: String = "thursday"
    private val FRIDAY: String = "friday"

    private val WED_DATE = "2018-06-20T"
    private val THU_DATE = "2018-06-21T"
    private val FRI_DATE = "2018-06-22T"

    private val MIN_SEC = ":00.000+02:00"

/*
    // DEEP DIVE
    val deepDiveSlotWednesday: List[Slot] = {

      val deepDiveWednesdaySlot1 = ConferenceRooms.dee.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.DEEP_DIVE.id,
            WEDNESDAY,
            new DateTime(WED_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE + "13:00" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }

      val deepDiveWednesdaySlot2 = ConferenceRooms.deepDiveWed.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.DEEP_DIVE.id,
            WEDNESDAY,
            new DateTime(WED_DATE + "14:00" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE + "17:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }

      deepDiveWednesdaySlot1 ++ deepDiveWednesdaySlot2
    }
*/

    // HANDS ON LABS
    val labsSlotsWednesday: List[Slot] = {

      val labsWednesdaySlot1 = ConferenceRooms.labRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id,
            WEDNESDAY,
            new DateTime(WED_DATE + "12:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE + "15:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }

      labsWednesdaySlot1
    }

    val labSlotsThursday: List[Slot] = {
      val labsThursdaySlot1 = ConferenceRooms.labRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id, THURSDAY,
            new DateTime(THU_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"12:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      val labsThursdaySlot2 = ConferenceRooms.labRooms.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id, THURSDAY,
            new DateTime(THU_DATE+"13:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"15:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }
      labsThursdaySlot1 ++ labsThursdaySlot2
    }

    val labFridayFriday: List[Slot] = {
      val labsFridaySlot1 = ConferenceRooms.labRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id, FRIDAY,
            new DateTime(FRI_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"12:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      val labsFridaySlot2 = ConferenceRooms.labRooms.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id, FRIDAY,
            new DateTime(FRI_DATE+"13:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"16:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }
      labsFridaySlot1 ++ labsFridaySlot2
    }

    // BOFS

    val bofSlotWednesday: List[Slot] = {

      val bofWednesdayEveningSlot = ConferenceRooms.bofRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "19:00" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
                      new DateTime(WED_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      bofWednesdayEveningSlot
    }

    // QUICKIES

    val quickiesSlotsWednesday: List[Slot] = {
      val quickiesWednesday1 = ConferenceRooms.quickieRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id, WEDNESDAY,
            new DateTime(WED_DATE+"12:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"12:35"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      quickiesWednesday1
    }

    val quickiesSlotsThursday: List[Slot] = {

      val quickiesThursdayLunch1 = ConferenceRooms.quickieRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "12:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
                      new DateTime(THU_DATE + "13:05" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      quickiesThursdayLunch1
    }

    val quickiesSlotsFriday: List[Slot] = {

      val quickiesFridayLunch1 = ConferenceRooms.quickieRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id,
            FRIDAY,
            new DateTime(FRI_DATE + "12:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE + "13:05" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      quickiesFridayLunch1
    }

    // CONFERENCE KEYNOTES

    val keynoteSlotWednesday: List[Slot] = {

      val keynoteSlot1 = ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.OPENING_KEY.id, WEDNESDAY,
            new DateTime(WED_DATE + "09:00" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }

      val keynoteSlot2 = ConferenceRooms.keynoteRoom.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.OPENING_KEY.id, WEDNESDAY,
            new DateTime(WED_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE + "10:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }

      keynoteSlot1 ++ keynoteSlot2
    }

    val keynoteSlotThursday: List[Slot] = {

      ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CLOSING_KEY.id, THURSDAY,
            new DateTime(THU_DATE + "15:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "16:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
    }

    val keynoteSlotFriday: List[Slot] = {

      ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CLOSING_KEY.id, FRIDAY,
            new DateTime(FRI_DATE + "15:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE + "16:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
    }

    // CONFERENCE SLOTS

    val conferenceSlotsWednesday: List[Slot] = {
      val conferenceWednesdaySlot1 = ConferenceRooms.conferenceRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"11:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"11:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      val conferenceWednesdaySlot2 = ConferenceRooms.conferenceRooms.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"12:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"13:40"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }
      val conferenceWednesdaySlot3 = ConferenceRooms.conferenceRooms.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"14:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"14:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r3)
      }
      val conferenceWednesdaySlot4 = ConferenceRooms.conferenceRooms.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"15:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"16:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r4)
      }
      val conferenceWednesdaySlot5 = ConferenceRooms.conferenceRooms.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"16:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"17:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r5)
      }
      val conferenceWednesdaySlot6 = ConferenceRooms.conferenceRooms.map {
        r6 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"17:40"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"18:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r6)
      }
      val conferenceWednesdaySlot7 = ConferenceRooms.conferenceRooms2.map {
        r7 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, WEDNESDAY,
            new DateTime(WED_DATE+"19:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(WED_DATE+"20:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r7)
      }
      conferenceWednesdaySlot1 ++ conferenceWednesdaySlot2 ++ conferenceWednesdaySlot3 ++ conferenceWednesdaySlot4 ++ conferenceWednesdaySlot5 ++ conferenceWednesdaySlot6 ++ conferenceWednesdaySlot7
    }

    val conferenceSlotsThursday: List[Slot] = {

      val conferenceThursdaySlot1 = ConferenceRooms.conferenceRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
            new DateTime(THU_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"09:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      val conferenceThursdaySlot2 = ConferenceRooms.conferenceRooms.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
            new DateTime(THU_DATE+"10:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"11:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }
      val conferenceThursdaySlot3 = ConferenceRooms.conferenceRooms.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
            new DateTime(THU_DATE+"11:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"12:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r3)
      }
      val conferenceThursdaySlot4 = ConferenceRooms.conferenceRooms.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
            new DateTime(THU_DATE+"13:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"14:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r4)
      }
      val conferenceThursdaySlot5 = ConferenceRooms.conferenceRooms.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
            new DateTime(THU_DATE+"14:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"15:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r5)
      }

      val conferenceThursdaySlot6 = ConferenceRooms.conferenceRooms.map {
        r6 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, THURSDAY,
            new DateTime(THU_DATE+"17:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE+"18:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r6)
      }

      conferenceThursdaySlot1 ++ conferenceThursdaySlot2 ++ conferenceThursdaySlot3 ++ conferenceThursdaySlot4 ++ conferenceThursdaySlot5 ++ conferenceThursdaySlot6
    }

    val conferenceSlotsFriday: List[Slot] = {
      val conferenceFridaySlot1 = ConferenceRooms.conferenceRooms.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, FRIDAY,
            new DateTime(FRI_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"09:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r1)
      }
      val conferenceFridaySlot2 = ConferenceRooms.conferenceRooms.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, FRIDAY,
            new DateTime(FRI_DATE+"10:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"11:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r2)
      }
      val conferenceFridaySlot3 = ConferenceRooms.conferenceRooms.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, FRIDAY,
            new DateTime(FRI_DATE+"11:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"12:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r3)
      }
      val conferenceFridaySlot4 = ConferenceRooms.conferenceRooms.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, FRIDAY,
            new DateTime(FRI_DATE+"13:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"14:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r4)
      }
      val conferenceFridaySlot5 = ConferenceRooms.conferenceRooms.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id, FRIDAY,
            new DateTime(FRI_DATE+"14:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(FRI_DATE+"15:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), r5)
      }
      conferenceFridaySlot1 ++ conferenceFridaySlot2 ++ conferenceFridaySlot3 ++ conferenceFridaySlot4 ++ conferenceFridaySlot5
    }


/*
    // Ignite slots
    val igniteSlotsThursday: List[Slot] = {
      ConferenceRooms.igniteThu.flatMap {
        room => List(
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:20" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:25" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:25" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:30" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:35" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:35" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:40" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:40" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:45" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:45" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "18:50" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
            new DateTime(THU_DATE + "18:55" + MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)), room)
        )
      }
    }
*/

    // Registration, coffee break, lunch etc
    val wednesdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registrationAndCoffee, "wednesday",
        new DateTime(WED_DATE+"08:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "wednesday",
        new DateTime(WED_DATE+"10:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"11:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "wednesday",
        new DateTime(WED_DATE+"11:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"12:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "wednesday",
        new DateTime(WED_DATE+"13:40"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"14:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "wednesday",
        new DateTime(WED_DATE+"14:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"15:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.snack, "wednesday",
        new DateTime(WED_DATE+"16:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"16:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "wednesday",
        new DateTime(WED_DATE+"17:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"17:40"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.eveningReception, "wednesday",
        new DateTime(WED_DATE+"18:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(WED_DATE+"21:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
    )

    val thursdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registrationAndCoffee, "thursday",
        new DateTime(THU_DATE+"08:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday",
        new DateTime(THU_DATE+"09:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"10:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "thursday",
        new DateTime(THU_DATE+"11:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"11:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "thursday",
        new DateTime(THU_DATE+"12:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"13:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "thursday",
        new DateTime(THU_DATE+"14:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"14:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday",
        new DateTime(THU_DATE+"15:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"15:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.snack, "thursday",
        new DateTime(THU_DATE+"16:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"17:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.eveningReception2, "thursday",
        new DateTime(THU_DATE+"18:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(THU_DATE+"21:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
    )

    val fridayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registrationAndCoffee, "friday",
        new DateTime(FRI_DATE+"08:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(FRI_DATE+"09:00"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "friday",
        new DateTime(FRI_DATE+"09:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(FRI_DATE+"10:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "friday",
        new DateTime(FRI_DATE+"11:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(FRI_DATE+"11:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "friday",
        new DateTime(FRI_DATE+"12:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(FRI_DATE+"13:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, "friday",
        new DateTime(FRI_DATE+"14:10"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(FRI_DATE+"14:30"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "friday",
        new DateTime(FRI_DATE+"15:20"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)),
        new DateTime(FRI_DATE+"15:50"+MIN_SEC).toDateTime(DateTimeZone.forID(timeZone)))
    )


    // DEVOXX DAYS

    val wednesdaySchedule: List[Slot] = {
      wednesdayBreaks ++ conferenceSlotsWednesday ++ quickiesSlotsWednesday ++ bofSlotWednesday ++ labsSlotsWednesday ++ keynoteSlotWednesday
    }

    val thursdaySchedule: List[Slot] = {
      thursdayBreaks ++ conferenceSlotsThursday ++ quickiesSlotsThursday ++ labSlotsThursday ++ keynoteSlotThursday
    }

    val fridaySchedule: List[Slot] = {
      fridayBreaks ++ conferenceSlotsFriday ++ quickiesSlotsFriday ++ labFridayFriday ++ keynoteSlotFriday
    }

    // COMPLETE DEVOXX
    def all: List[Slot] = {
      wednesdaySchedule ++
      thursdaySchedule ++
      fridaySchedule
    }
  }

  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  val fromDay = new DateTime().withYear(2018).withMonthOfYear(6).withDayOfMonth(20)
  val toDay = new DateTime().withYear(2018).withMonthOfYear(6).withDayOfMonth(22)

  val MAXIMUM_SUMMARY_CHARACTERS = 1200

  val DEVOXX_EVENT_CODE = "DevoxxPL2018"
  val DEVOXX_HASH_TAG = "#DevoxxPL"
  val DEVOXX_VENUE_ADDRESS = "ICE Krakow Congress Centre, Marii Konopnickiej 17, 30-302 Krakow, Poland"
  val DEVOXX_FIRST_DAY_IN_FRENCH = "9 mai"
  val DEVOXX_FIRST_DAY_IN_ENGLISH = "June 20th"
  val DEVOXX_DAY_RANGE_IN_FRENCH = "mer 9 au 11 Mai 2018"
  val DEVOXX_DAY_RANGE_IN_ENGLISH = "from 20th to 22nd of June, 2018"
  val DEVOXX_CONF_SITE_URL = "http://www.devoxx.pl/"
  val DEVOXX_INFO_SITE_URL = DEVOXX_CONF_SITE_URL + "/faq"
  val DEVOXX_SPONSORS_SITE_URL = DEVOXX_CONF_SITE_URL + "/sponsors"
  val DEVOXX_TICKETS_SITE_URL = "http://reg.devoxx.pl"
  val DEVOXX_CFP_OPENED_ON_DATE = "2018-01-05T00:00:00+02:00"
  val DEVOXX_CFP_CLOSED_ON_DATE = "2018-03-23T23:59:59+02:00"
  val DEVOXX_SCHEDULE_ANNOUNCEMENT_DATE = "2018-04-30T00:00:00+02:00"

  def current(): ConferenceDescriptor = new ConferenceDescriptor(
    eventCode = DEVOXX_EVENT_CODE,
    // You will need to update conf/routes files with this code if modified
    confUrlCode = DEVOXX_CONF_URL_CODE,
    frLangEnabled = false,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("cfp@devoxx.pl"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("cfp@devoxx.pl"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("info@devoxx.pl"),
    conferenceUrls = ConferenceUrls(
      info = DEVOXX_INFO_SITE_URL,
      sponsors = DEVOXX_SPONSORS_SITE_URL,
      registration = DEVOXX_TICKETS_SITE_URL,
      confWebsite = DEVOXX_CONF_SITE_URL,
      cfpHostname = {
        val h=Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.pl")
        if(h.endsWith("/")){
          h.substring(0,h.length - 1)
        }else{
          h
        }
      }
    ),
    timing = ConferenceTiming(
      datesI18nKey = Messages("conference.dates"),
      speakersPassDuration = 3,
      preferredDayEnabled = true,
      firstDayFr = DEVOXX_FIRST_DAY_IN_FRENCH,
      firstDayEn = DEVOXX_FIRST_DAY_IN_ENGLISH,
      datesFr = DEVOXX_DAY_RANGE_IN_FRENCH,
      datesEn = DEVOXX_DAY_RANGE_IN_ENGLISH,
      cfpOpenedOn = DateTime.parse(DEVOXX_CFP_OPENED_ON_DATE),
      cfpClosedOn = DateTime.parse(DEVOXX_CFP_CLOSED_ON_DATE),
      scheduleAnnouncedOn = DateTime.parse(DEVOXX_SCHEDULE_ANNOUNCEMENT_DATE),
      days=dateRange(fromDay, toDay,new Period().withDays(1))
    ),
    hosterName = "Clever-cloud", hosterWebsite = "http://www.clever-cloud.com/" + DEVOXX_HASH_TAG,
    hashTag = DEVOXX_HASH_TAG,
    conferenceSponsor = ConferenceSponsor(showSponsorProposalCheckbox = true, sponsorProposalType = ConferenceProposalTypes.CONF)
    , List(Locale.ENGLISH)
    , DEVOXX_VENUE_ADDRESS
    , notifyProposalSubmitted = true
    , MAXIMUM_SUMMARY_CHARACTERS // French developers tends to be a bit verbose... we need extra space :-)
  )

  def conference() = ConferenceDescriptor(
    eventCode = DEVOXX_EVENT_CODE,
    // You will need to update conf/routes files with this code if modified
    confUrlCode = DEVOXX_CONF_URL_CODE,
    frLangEnabled = false,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("cfp@devoxx.pl"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("cfp@devoxx.pl"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("info@devoxx.pl"),
    conferenceUrls = ConferenceUrls(
      info = DEVOXX_INFO_SITE_URL,
      sponsors = DEVOXX_SPONSORS_SITE_URL,
      registration = DEVOXX_TICKETS_SITE_URL,
      confWebsite = DEVOXX_CONF_SITE_URL,
      cfpHostname = Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.pl")
    ),
    timing = ConferenceTiming(
      datesI18nKey = Messages("conference.dates"),
      speakersPassDuration = 3,
      preferredDayEnabled = true,
      firstDayFr = DEVOXX_FIRST_DAY_IN_FRENCH,
      firstDayEn = DEVOXX_FIRST_DAY_IN_ENGLISH,
      datesFr = DEVOXX_DAY_RANGE_IN_FRENCH,
      datesEn = DEVOXX_DAY_RANGE_IN_ENGLISH,
      cfpOpenedOn = DateTime.parse(DEVOXX_CFP_OPENED_ON_DATE),
      cfpClosedOn = DateTime.parse(DEVOXX_CFP_CLOSED_ON_DATE),
      scheduleAnnouncedOn = DateTime.parse(DEVOXX_SCHEDULE_ANNOUNCEMENT_DATE),
      days=dateRange(fromDay,toDay,new Period().withDays(2))
    ),
    hosterName = "Clever-cloud", hosterWebsite = "http://www.clever-cloud.com/" + DEVOXX_HASH_TAG,
    hashTag = DEVOXX_HASH_TAG,
    conferenceSponsor = ConferenceSponsor(showSponsorProposalCheckbox = true, sponsorProposalType = ConferenceProposalTypes.CONF)
    , List(Locale.ENGLISH)
    , DEVOXX_VENUE_ADDRESS
    , notifyProposalSubmitted = true
    , MAXIMUM_SUMMARY_CHARACTERS // French developers tends to be a bit verbose... we need extra space :-)
  )

  def isCFPOpen: Boolean = {
    Backoffice.isCFPOpen()
  }

  // All timezone sensitive methods are using this constant variable.
  // Defaults to "Europe/London" if not set in the Clever Cloud env. variables page.
  def timeZone: String = Play.current.configuration.getString("conference.timezone").getOrElse("Europe/Warsaw")

  def isGoldenTicketActive: Boolean = Play.current.configuration.getBoolean("goldenTicket.active").getOrElse(false)

  def isFavoritesSystemActive: Boolean = Play.current.configuration.getBoolean("cfp.activateFavorites").getOrElse(false)

  def isHTTPSEnabled: Boolean = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)

  // Reset all votes when a Proposal with state=SUBMITTED (or DRAFT) is updated
  // This is to reflect the fact that some speakers are eavluated, then they update the talk, and we should revote for it
  def isResetVotesForSubmitted = Play.current.configuration.getBoolean("cfp.resetVotesForSubmitted").getOrElse(false)

  // Set this to true temporarily
  // I will implement a new feature where each CFP member can decide to receive one digest email per day or a big email
  def notifyProposalSubmitted = true

  def gluonAuthorization(): String = Play.current.configuration.getString("gluon.authorization").getOrElse("")
  def gluonUsername(): String = Play.current.configuration.getString("gluon.username").getOrElse("")
  def gluonPassword(): String = Play.current.configuration.getString("gluon.password").getOrElse("")
}
