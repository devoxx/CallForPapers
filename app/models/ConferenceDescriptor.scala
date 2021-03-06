package models

import java.util.Locale

import controllers.Configuration
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

case class ConferenceUrls(info: String, registration: String, confWebsite: String, cfpHostname: String, sponsors: String) {

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

case class TrackDesc(id: String, imgSrc: String, i18nTitleProp: String, i18nDescProp: String, oldId: String)

case class ProposalConfiguration(id: String, slotsCount: Int,
                                 givesSpeakerFreeEntrance: Boolean,
                                 freeEntranceDisplayed: Boolean,
                                 htmlClass: String,
                                 hiddenInCombo: Boolean = false,
                                 chosablePreferredDay: Boolean = false,
                                 impliedSelectedTrack: Option[Track] = None)

object ProposalConfiguration {

  val UNKNOWN = ProposalConfiguration(
    id = "unknown",
    slotsCount = 0,
    givesSpeakerFreeEntrance = false,
    freeEntranceDisplayed = false,
    htmlClass = "",
    hiddenInCombo = true
  )

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
                                maxProposalSummaryCharacters: Int = 1200
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
    val IGNITE = ProposalType(id = "ignite", label = "ignite.label")
    val OTHER = ProposalType(id = "other", label = "other.label")

    val ALL = List(
      CONF,
      UNI,
      TIA,
      LAB,
      QUICK,
      BOF,
      KEY,
      IGNITE,
      OTHER
    )

    def valueOf(id: String): ProposalType = id match {
      case "conf" => CONF
      case "uni" => UNI
      case "tia" => TIA
      case "lab" => LAB
      case "quick" => QUICK
      case "bof" => BOF
      case "key" => KEY
      case "ignite" => IGNITE
      case "other" => OTHER
    }
  }

  object ConferenceProposalConfigurations {
    val CONF = ProposalConfiguration(
      id = "conf",
      slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.CONF.id)),
      givesSpeakerFreeEntrance = true,
      freeEntranceDisplayed = true,
      htmlClass = "icon-microphone",
      chosablePreferredDay = true)

    val UNI = ProposalConfiguration(
      id = "uni",
      slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.UNI.id)),
      givesSpeakerFreeEntrance = true,
      freeEntranceDisplayed = true,
      htmlClass = "icon-laptop",
      chosablePreferredDay = true)

    val TIA = ProposalConfiguration(
      id = "tia",
      slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.TIA.id)),
      givesSpeakerFreeEntrance = true,
      freeEntranceDisplayed = true,
      htmlClass = "icon-legal",
      chosablePreferredDay = true)

    val LAB = ProposalConfiguration(
      id = "lab",
      slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.LAB.id)),
      givesSpeakerFreeEntrance = true,
      freeEntranceDisplayed = true,
      htmlClass = "icon-beaker",
      chosablePreferredDay = true)

    val QUICK = ProposalConfiguration(
      id = "quick",
      slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.QUICK.id)),
      givesSpeakerFreeEntrance = false,
      freeEntranceDisplayed = false,
      htmlClass = "icon-fast-forward",
      chosablePreferredDay = true)

    val BOF = ProposalConfiguration(
      id = "bof",
      slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.BOF.id)),
      givesSpeakerFreeEntrance = true,
      freeEntranceDisplayed = true,
      htmlClass = "icon-group")

    val KEY = ProposalConfiguration(id = "key", slotsCount = 1, givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val IGNITE = ProposalConfiguration(id = "ignite", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.IGNITE.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-bolt",
      chosablePreferredDay = false)
    val OTHER = ProposalConfiguration(id = "other", slotsCount = 1, givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "",
      hiddenInCombo = true, chosablePreferredDay = false)

    val ALL = List(CONF, UNI, TIA, LAB, QUICK, BOF, KEY, IGNITE, OTHER)

    def doesItGivesSpeakerFreeEntrance(proposalType: ProposalType): Boolean = {
      ALL.filter(_.id == proposalType.id).exists(_.givesSpeakerFreeEntrance)
    }
  }

  object ConferenceTracks {
    // val totalTracks : Int = Configuration.getKeyValue("total.tracks").get.toInt

    val TRACK1 = Track("track.1", "track.1.label")
    val TRACK2 = Track("track.2", "track.2.label")
    val TRACK3 = Track("track.3", "track.3.label")
    val TRACK4 = Track("track.4", "track.4.label")
    val TRACK5 = Track("track.5", "track.5.label")
    val TRACK6 = Track("track.6", "track.6.label")
    val TRACK7 = Track("track.7", "track.7.label")
    val TRACK8 = Track("track.8", "track.8.label")
    val TRACK9 = Track("track.9", "track.9.label")
    val TRACK10 = Track("track.10", "track.10.label")

    val ALL = List(
      TRACK1,
      TRACK2,
      TRACK3,
      TRACK4,
      TRACK5,
      TRACK6,
      TRACK7,
      TRACK8,
      TRACK9,
      TRACK10
    )
  }

  val METHOD_ARCHI = Track("method_archi", "method_archi.label")
  val JAVA = Track("java", "java.label")
  val CLOUD = Track("cloud", "cloud.label")
  val SSJ = Track("ssj", "ssj.label")
  val LANG = Track("lang", "lang.label")
  val BIGDATA = Track("bigdata", "bigdata.label")
  val WEB = Track("web", "web.label")
  val FUTURE = Track("future", "future.label")
  val MOBILE = Track("mobile", "mobile.label")
  val SECURITY = Track("security", "security.label")

  object ConferenceTracksDescription {
    val TRACK1 : TrackDesc = if (Configuration.getKeyValue("track.1.img").isDefined) {
        TrackDesc(ConferenceTracks.TRACK1.id,
                  Configuration.getKeyValue("track.1.img").get,
                  ConferenceTracks.TRACK1.label,
                  "track.1.desc", "method_archi")
    } else {
      TrackDesc(ConferenceTracks.TRACK1.id,
                "/assets/devoxx/images/icon_methodology.png",
                ConferenceTracks.TRACK1.label,
                "track.1.desc", "method_archi")
    }

    val TRACK2 : TrackDesc = if (Configuration.getKeyValue("track.2.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK2.id,
                Configuration.getKeyValue("track.2.img").get,
                ConferenceTracks.TRACK2.label,
                "track.2.desc", "java")
    } else {
      TrackDesc(ConferenceTracks.TRACK2.id,
                "/assets/devoxx/images/icon_javase.png",
                ConferenceTracks.TRACK2.label,
                "track.2.desc", "java")
    }

    val TRACK3 : TrackDesc = if (Configuration.getKeyValue("track.3.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK3.id,
                Configuration.getKeyValue("track.3.img").get,
                ConferenceTracks.TRACK3.label,
                "track.3.desc", "cloud")
    } else {
      TrackDesc(ConferenceTracks.TRACK3.id,
                "/assets/devoxx/images/icon_cloud.png",
                ConferenceTracks.TRACK3.label,
                "track.3.desc", "cloud")
    }

    val TRACK4 : TrackDesc = if (Configuration.getKeyValue("track.4.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK4.id,
                Configuration.getKeyValue("track.4.img").get,
                ConferenceTracks.TRACK4.label,
                "track.4.desc", "ssj")
    } else {
      TrackDesc(ConferenceTracks.TRACK4.id,
                "/assets/devoxx/images/icon_javaee.png",
                ConferenceTracks.TRACK4.label,
                "track.4.desc", "ssj")
    }

    val TRACK5 : TrackDesc = if (Configuration.getKeyValue("track.5.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK5.id,
                Configuration.getKeyValue("track.5.img").get,
                ConferenceTracks.TRACK5.label,
                "track.5.desc", "lang")
    } else {
      TrackDesc(ConferenceTracks.TRACK5.id,
                "/assets/devoxx/images/icon_alternative.png",
                ConferenceTracks.TRACK5.label,
                "track.5.desc", "lang")
    }

    val TRACK6 : TrackDesc = if (Configuration.getKeyValue("track.6.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK6.id,
                Configuration.getKeyValue("track.6.img").get,
                ConferenceTracks.TRACK6.label,
                "track.6.desc", "bigdata")
    } else {
      TrackDesc(ConferenceTracks.TRACK6.id,
                "/assets/devoxx/images/icon_architecture.png",
                ConferenceTracks.TRACK6.label,
                "track.6.desc", "bigdata")
    }

    val TRACK7 : TrackDesc = if (Configuration.getKeyValue("track.7.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK7.id,
                Configuration.getKeyValue("track.7.img").get,
                ConferenceTracks.TRACK7.label,
                "track.7.desc", "web")
    } else {
      TrackDesc(ConferenceTracks.TRACK7.id,
                "/assets/devoxx/images/icon_web.png",
                ConferenceTracks.TRACK7.label,
                "track.7.desc", "web")
    }

    val TRACK8 : TrackDesc = if (Configuration.getKeyValue("track.8.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK8.id,
                Configuration.getKeyValue("track.8.img").get,
                ConferenceTracks.TRACK8.label,
                "track.8.desc", "future")
    } else {
      TrackDesc(ConferenceTracks.TRACK8.id,
                "/assets/devoxx/images/icon_future.png",
                ConferenceTracks.TRACK8.label,
                "track.8.desc", "future")
    }

    val TRACK9 : TrackDesc = if (Configuration.getKeyValue("track.9.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK9.id,
                Configuration.getKeyValue("track.9.img").get,
                ConferenceTracks.TRACK9.label,
                "track.9.desc", "mobile")
    } else {
      TrackDesc(ConferenceTracks.TRACK9.id,
                "/assets/devoxx/images/icon_mobile.png",
                ConferenceTracks.TRACK9.label,
                "track.9.desc", "mobile")
    }

    val TRACK10 : TrackDesc = if (Configuration.getKeyValue("track.10.img").isDefined) {
      TrackDesc(ConferenceTracks.TRACK10.id,
                Configuration.getKeyValue("track.10.img").get,
                ConferenceTracks.TRACK10.label,
                "track.10.desc", "security")
    } else {
      TrackDesc(ConferenceTracks.TRACK10.id,
                "/assets/devoxx/images/icon_security.png",
                ConferenceTracks.TRACK10.label,
                "track.10.desc", "security")
    }

    val ALL = List(
      TRACK1,
      TRACK2,
      TRACK3,
      TRACK4,
      TRACK5,
      TRACK6,
      TRACK7,
      TRACK8,
      TRACK9,
      TRACK10
    )

    def findTrackDescFor(t: Track): TrackDesc = {
      if (ALL.exists(_.id == t.id)) {
        ALL.find(_.id == t.id).head
      } else {
        ALL.find(_.oldId == t.id).head
      }
    }
  }

  // TODO If you want to use the Devoxx Scheduler, you can describe here the list of rooms, with capacity for seats
  object ConferenceRooms {

    // Tip : I use the ID to sort-by on the view per day... So if the exhibition floor id is "aaa" it will be
    // the first column on the HTML Table
    val HALL_EXPO = Room("a_hall", "Exhibition floor", 1500, "special")

    val ROOM3 = Room("room3", "Room 3", 300, "theatre")
    val ROOM4 = Room("room4", "Room 4", 347, "theatre")
    val ROOM5 = Room("room5", "Room 5", 641, "theatre")
    val ROOM6 = Room("room6", "Room 6", 372, "theatre")
    val ROOM7 = Room("room7", "Room 7", 370, "theatre")
    val ROOM8 = Room("room8", "Room 8", 696, "theatre")
    val ROOM9 = Room("room9", "Room 9", 393, "theatre")
    val ROOM10 = Room("room10", "Room 10", 286, "theatre")

    val BOF1 = Room("bof1", "BOF 1", 70, "classroom")
    val BOF2 = Room("bof2", "BOF 2", 70, "classroom")

    val allRoomsUni = List(ROOM4, ROOM5, ROOM6, ROOM8, ROOM9)

    val allRoomsTIA = List(ROOM4, ROOM5, ROOM6, ROOM8, ROOM9)

    val keynoteRoom = List(ROOM8, ROOM4, ROOM5, ROOM9)

    val eveningKeynoteRoom = List(ROOM5)

    val allRoomsConf = List(ROOM8, ROOM5, ROOM9, ROOM6, ROOM7, ROOM4, ROOM3, ROOM10)
    val fridayRoomsConf = List(ROOM4, ROOM5, ROOM6, ROOM8, ROOM9)

    val allRoomsQuick = List(ROOM8, ROOM5, ROOM9, ROOM6, ROOM7, ROOM4, ROOM3, ROOM10)

    val allRoomsLabs = List(BOF1, BOF2)
    val oneRoomLabs = List(BOF1)

    val allRoomsBOF = List(BOF1, BOF2)
    val oneRoomBOF = List(BOF1)

    val igniteRoom = List(BOF1)

    val allRooms = List(ROOM8, ROOM5, ROOM9, ROOM6, ROOM7, ROOM4, ROOM3, ROOM10, BOF1, BOF2, HALL_EXPO)

    val allRoomsAsIdsAndLabels:Seq[(String,String)] = allRooms.map(a=>(a.id,a.name)).sorted
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

    val allBreaks = List(registration, petitDej, coffee, lunch, shortBreak, exhibition, meetAndGreet, eveningKeynote, closingKeynote, movieSpecial)
  }

  object ConferenceSlots {

    // VARIABLE CONSTANTS

    private val europeBrussels: String = ConferenceDescriptor.timeZone
    private val MONDAY: String = "monday"
    private val TUESDAY: String = "tuesday"
    private val WEDNESDAY: String = "wednesday"
    private val THURSDAY: String = "thursday"
    private val FRIDAY: String = "friday"

    private val MON_DATE = "2017-11-06T"
    private val TUE_DATE = "2017-11-07T"
    private val WED_DATE = "2017-11-08T"
    private val THU_DATE = "2017-11-09T"
    private val FRI_DATE = "2017-11-10T"
    
    private val MIN_SEC = ":00.000+01:00"

    // DEEP DIVES

    val universitySlotsMonday: List[Slot] = {

      val universityMondayMorning = ConferenceRooms.allRoomsUni.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.UNI.id,
                      MONDAY,
                      new DateTime(MON_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "12:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val universityMondayAfternoon = ConferenceRooms.allRoomsUni.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.UNI.id,
                      MONDAY,
                      new DateTime(MON_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "16:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      universityMondayMorning ++ 
      universityMondayAfternoon
    }

    val universitySlotsTuesday: List[Slot] = {

      val universityTuesdayMorning = ConferenceRooms.allRoomsUni.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.UNI.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "12:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val universityTuesdayAfternoon = ConferenceRooms.allRoomsUni.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.UNI.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "16:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      universityTuesdayMorning ++ 
      universityTuesdayAfternoon
    }

    // TOOLS IN ACTION

    val tiaSlotsMonday: List[Slot] = {

      val toolsMondayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id,
                      MONDAY,
                      new DateTime(MON_DATE + "16:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "17:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val toolsMondayAfternoonSlot2 = ConferenceRooms.allRoomsTIA.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id,
                      MONDAY,
                      new DateTime(MON_DATE + "17:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "17:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val toolsMondayAfternoonSlot3 = ConferenceRooms.allRoomsTIA.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id,
                      MONDAY,
                      new DateTime(MON_DATE + "18:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "18:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
//      val toolsMondayAfternoonSlot4 = ConferenceRooms.allRoomsTIA.map {
//        r4 =>
//          SlotBuilder(ConferenceProposalTypes.TIA.id,
//                      MONDAY,
//                      new DateTime(MON_DATE + "18:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
//                      new DateTime(MON_DATE + "19:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r4)
//      }
      toolsMondayAfternoonSlot1 ++ 
      toolsMondayAfternoonSlot2 ++ 
      toolsMondayAfternoonSlot3
//      toolsMondayAfternoonSlot4
    }

    val tiaSlotsTuesday: List[Slot] = {

      val toolsTuesdayAfternoonSlot1 = ConferenceRooms.allRoomsTIA.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "16:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "17:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val toolsTuesdayAfternoonSlot2 = ConferenceRooms.allRoomsTIA.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "17:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "17:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val toolsTuesdayAfternoonSlot3 = ConferenceRooms.allRoomsTIA.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.TIA.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "18:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "18:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
//      val toolsTuesdayAfternoonSlot4 = ConferenceRooms.allRoomsTIA.map {
//        r4 =>
//          SlotBuilder(ConferenceProposalTypes.TIA.id,
//                      TUESDAY,
//                      new DateTime(TUE_DATE + "18:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
//                      new DateTime(TUE_DATE + "19:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r4)
//      }
      toolsTuesdayAfternoonSlot1 ++ 
      toolsTuesdayAfternoonSlot2 ++ 
      toolsTuesdayAfternoonSlot3
//      toolsTuesdayAfternoonSlot4
    }

    // HANDS ON LABS

    val labsSlotsMonday: List[Slot] = {

      val labsMondayMorning = ConferenceRooms.oneRoomLabs.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id,
                      MONDAY,
                      new DateTime(MON_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "12:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val labsMondayAfternoon = ConferenceRooms.allRoomsLabs.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id,
                      MONDAY,
                      new DateTime(MON_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "16:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      labsMondayMorning ++ 
      labsMondayAfternoon
    }

    val labsSlotsTuesday: List[Slot] = {

      val labsTuesdayMorning = ConferenceRooms.allRoomsLabs.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "12:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val labsTuesdayAfternoon = ConferenceRooms.allRoomsLabs.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.LAB.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "16:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }


      labsTuesdayMorning ++ 
      labsTuesdayAfternoon
    }

    // BOFS

    val bofSlotsMonday: List[Slot] = {

      val bofMondayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      MONDAY,
                      new DateTime(MON_DATE + "19:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "20:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val bofMondayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      MONDAY,
                      new DateTime(MON_DATE + "20:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(MON_DATE + "21:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      bofMondayEveningSlot1 ++ 
      bofMondayEveningSlot2
    }

    val bofSlotsTuesday: List[Slot] = {

      val bofTuesdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "19:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "20:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val bofTuesdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      TUESDAY,
                      new DateTime(TUE_DATE + "20:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(TUE_DATE + "21:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      bofTuesdayEveningSlot1 ++ 
      bofTuesdayEveningSlot2
    }

    val bofSlotsWednesday: List[Slot] = {

      val bofWednesdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "19:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val bofWednesdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "21:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val bofWednesdayEveningSlot3 = ConferenceRooms.allRoomsBOF.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "21:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "22:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
      bofWednesdayEveningSlot1 ++
      bofWednesdayEveningSlot2 ++
      bofWednesdayEveningSlot3
    }

    val bofSlotsThursday: List[Slot] = {

      val bofThursdayEveningSlot1 = ConferenceRooms.allRoomsBOF.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "19:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val bofThursdayEveningSlot2 = ConferenceRooms.allRoomsBOF.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.BOF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "21:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }

      bofThursdayEveningSlot1 ++
      bofThursdayEveningSlot2
    }

    // QUICKIES

    val quickiesSlotsWednesday: List[Slot] = {

      val quickiesWednesdayLunch1 = ConferenceRooms.allRoomsQuick.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "13:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "13:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val quickiesWednesdayLunch2 = ConferenceRooms.allRoomsQuick.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "13:35" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "13:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      quickiesWednesdayLunch1 ++ quickiesWednesdayLunch2
    }

    val quickiesSlotsThursday: List[Slot] = {

      val quickiesThursdayLunch1 = ConferenceRooms.allRoomsQuick.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "13:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "13:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val quickiesThursdayLunch2 = ConferenceRooms.allRoomsQuick.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.QUICK.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "13:35" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "13:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      quickiesThursdayLunch1 ++ quickiesThursdayLunch2
    }

    // CONFERENCE KEYNOTES

    val keynoteSlotsWednesday: List[Slot] = {

      val keynoteSlot1 = ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, WEDNESDAY,
            new DateTime(WED_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(WED_DATE + "09:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val keynoteSlot2 = ConferenceRooms.keynoteRoom.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, WEDNESDAY,
            new DateTime(WED_DATE + "09:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(WED_DATE + "10:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val keynoteSlot3 = ConferenceRooms.keynoteRoom.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, WEDNESDAY,
            new DateTime(WED_DATE + "10:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(WED_DATE + "10:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
      val keynoteSlot4 = ConferenceRooms.keynoteRoom.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, WEDNESDAY,
            new DateTime(WED_DATE + "10:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(WED_DATE + "11:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r4)
      }
      keynoteSlot1 ++
      keynoteSlot2 ++
      keynoteSlot3 ++
      keynoteSlot4
    }

    val keynoteSlotsThursday: List[Slot] = {

      ConferenceRooms.keynoteRoom.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.KEY.id, THURSDAY,
            new DateTime(THU_DATE + "19:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "19:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
    }

    // CONFERENCE SLOTS

    val conferenceSlotsWednesday: List[Slot] = {

      val conferenceWednesdaySlot1 = ConferenceRooms.allRoomsConf.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "12:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "12:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val conferenceWednesdaySlot2 = ConferenceRooms.allRoomsConf.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "14:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "14:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val conferenceWednesdaySlot3 = ConferenceRooms.allRoomsConf.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "15:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "16:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
      val conferenceWednesdaySlot4 = ConferenceRooms.allRoomsConf.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "16:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "17:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r4)
      }
      val conferenceWednesdaySlot5 = ConferenceRooms.allRoomsConf.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      WEDNESDAY,
                      new DateTime(WED_DATE + "17:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(WED_DATE + "18:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r5)
      }
      conferenceWednesdaySlot1 ++
      conferenceWednesdaySlot2 ++
      conferenceWednesdaySlot3 ++
      conferenceWednesdaySlot4 ++
      conferenceWednesdaySlot5
    }

    val conferenceSlotsThursday: List[Slot] = {

      val conferenceThursdaySlot0 = ConferenceRooms.allRoomsConf.map {
        r0 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "10:20" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r0)
      }
      val conferenceThursdaySlot1 = ConferenceRooms.allRoomsConf.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "10:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "11:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val conferenceThursdaySlot2 = ConferenceRooms.allRoomsConf.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "12:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "12:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val conferenceThursdaySlot3 = ConferenceRooms.allRoomsConf.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "14:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "14:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
      val conferenceThursdaySlot4 = ConferenceRooms.allRoomsConf.map {
        r4 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "15:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "16:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r4)
      }

      val conferenceThursdaySlot5 = ConferenceRooms.allRoomsConf.map {
        r5 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
            THURSDAY,
            new DateTime(THU_DATE + "16:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "17:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r5)
      }

      val conferenceThursdaySlot6 = ConferenceRooms.allRoomsConf.map {
        r6 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      THURSDAY,
                      new DateTime(THU_DATE + "17:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(THU_DATE + "18:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r6)
      }

      // Closing sessions on Thursday
      val closingSessions = List(SlotBuilder(ConferenceSlotBreaks.closingKeynote,
        THURSDAY,
        new DateTime(THU_DATE + "19:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(THU_DATE + "19:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels))),

        SlotBuilder(ConferenceSlotBreaks.movieSpecial,
          THURSDAY,
          new DateTime(THU_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
          new DateTime(THU_DATE + "22:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels))))

      val toReturn = conferenceThursdaySlot0 ++
                     conferenceThursdaySlot1 ++
                     conferenceThursdaySlot2 ++
                     conferenceThursdaySlot3 ++
                     conferenceThursdaySlot4 ++
                     conferenceThursdaySlot5 ++
                     conferenceThursdaySlot6 ++
                     closingSessions

      toReturn

    }
    // ROOM4, ROOM5, ROOM8, ROOM9, ROOM10
    val conferenceSlotsFriday: List[Slot] = {

      val conferenceFridaySlot1 = ConferenceRooms.fridayRoomsConf.map {
        r1 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      FRIDAY,
                      new DateTime(FRI_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(FRI_DATE + "10:20" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r1)
      }
      val conferenceFridaySlot2 = ConferenceRooms.fridayRoomsConf.map {
        r2 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      FRIDAY,
                      new DateTime(FRI_DATE + "10:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(FRI_DATE + "11:35" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r2)
      }
      val conferenceFridaySlot3 = ConferenceRooms.fridayRoomsConf.map {
        r3 =>
          SlotBuilder(ConferenceProposalTypes.CONF.id,
                      FRIDAY,
                      new DateTime(FRI_DATE + "11:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
                      new DateTime(FRI_DATE + "12:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), r3)
      }
      conferenceFridaySlot1 ++ conferenceFridaySlot2 ++ conferenceFridaySlot3
      }

    // Ignite slots
    val igniteSlotsThursday: List[Slot] = {
      ConferenceRooms.igniteRoom.flatMap {
        room => List(
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:05" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:05" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:20" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:20" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:25" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:35" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:35" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "13:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room),
          SlotBuilder(ConferenceProposalTypes.IGNITE.id, THURSDAY,
            new DateTime(THU_DATE + "13:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
            new DateTime(THU_DATE + "14:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)), room)
        )
      }
    }

    // Registration, coffee break, lunch etc
    val mondayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, MONDAY,
        new DateTime(MON_DATE + "08:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(MON_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, MONDAY,
        new DateTime(MON_DATE + "12:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(MON_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, MONDAY,
        new DateTime(MON_DATE + "16:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(MON_DATE + "16:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.shortBreak, MONDAY,
        new DateTime(MON_DATE + "17:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(MON_DATE + "18:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
    )

    val tuesdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, TUESDAY,
        new DateTime(TUE_DATE + "08:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(TUE_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, TUESDAY,
        new DateTime(TUE_DATE + "12:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(TUE_DATE + "13:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, TUESDAY,
        new DateTime(TUE_DATE + "16:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(TUE_DATE + "16:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.exhibition, TUESDAY,
        new DateTime(TUE_DATE + "17:55" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(TUE_DATE + "18:15" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
    )

    val wednesdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, WEDNESDAY,
        new DateTime(WED_DATE + "08:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(WED_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, WEDNESDAY,
        new DateTime(WED_DATE + "11:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(WED_DATE + "12:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, WEDNESDAY,
        new DateTime(WED_DATE + "13:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(WED_DATE + "14:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, WEDNESDAY,
        new DateTime(WED_DATE + "16:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(WED_DATE + "16:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.meetAndGreet, WEDNESDAY,
        new DateTime(WED_DATE + "18:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(WED_DATE + "20:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
    )

    val thursdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.petitDej, THURSDAY,
        new DateTime(THU_DATE + "08:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(THU_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, THURSDAY,
        new DateTime(THU_DATE + "10:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(THU_DATE + "10:50" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, THURSDAY,
        new DateTime(THU_DATE + "13:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(THU_DATE + "14:00" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, THURSDAY,
        new DateTime(THU_DATE + "16:10" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(THU_DATE + "16:40" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
    )

    val fridayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.petitDej, FRIDAY,
        new DateTime(FRI_DATE + "08:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(FRI_DATE + "09:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels))),
      SlotBuilder(ConferenceSlotBreaks.coffee, FRIDAY,
        new DateTime(FRI_DATE + "10:30" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)),
        new DateTime(FRI_DATE + "10:45" + MIN_SEC).toDateTime(DateTimeZone.forID(europeBrussels)))
    )

    // DEVOXX DAYS

    val mondaySchedule: List[Slot] = {
      mondayBreaks ++
      universitySlotsMonday ++
      tiaSlotsMonday ++
      labsSlotsMonday ++
      bofSlotsMonday
    }

    val tuesdaySchedule: List[Slot] = {
      tuesdayBreaks ++
      universitySlotsTuesday ++
      tiaSlotsTuesday ++
      labsSlotsTuesday ++
      bofSlotsTuesday
    }

    val wednesdaySchedule: List[Slot] = {
      wednesdayBreaks ++
      keynoteSlotsWednesday ++
      conferenceSlotsWednesday ++
      quickiesSlotsWednesday ++
      bofSlotsWednesday
    }

    val thursdaySchedule: List[Slot] = {
      thursdayBreaks ++
      keynoteSlotsThursday ++
      conferenceSlotsThursday ++
      quickiesSlotsThursday ++
      bofSlotsThursday ++
      igniteSlotsThursday
    }

    val fridaySchedule: List[Slot] = {
      fridayBreaks ++
      conferenceSlotsFriday
    }

    // COMPLETE DEVOXX
    def all: List[Slot] = {
      mondaySchedule ++
      tuesdaySchedule ++
      wednesdaySchedule ++
      thursdaySchedule ++
      fridaySchedule
    }
  }

  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  val fromDay: DateTime = new DateTime().withYear(2017).withMonthOfYear(11).withDayOfMonth(6)
  val toDay: DateTime = new DateTime().withYear(2017).withMonthOfYear(11).withDayOfMonth(9)

  def current() = ConferenceDescriptor(
    eventCode = "DVBE17",
    // You will need to update conf/routes files with this code if modified
    confUrlCode = "devoxxbe2017",
    frLangEnabled = Configuration.getKeyBoolean(Configuration.CONFIG_LOCALE_FR_ENABLED),
    fromEmail = Configuration.getKeyValue(Configuration.CONFIG_MAIL_FROM).getOrElse("info@devoxx.com"),
    committeeEmail = Configuration.getKeyValue(Configuration.CONFIG_MAIL_COMMITTEE).getOrElse("program@devoxx.com"),
    bccEmail = Configuration.getKeyValue(Configuration.CONFIG_MAIL_BCC),
    bugReportRecipient = Configuration.getKeyValue(Configuration.CONFIG_MAIL_BUGREPORT).getOrElse("nicolas.martignole@devoxx.fr"),
    conferenceUrls = ConferenceUrls(
      info = Configuration.getKeyValue(Configuration.CONFIG_URL_INFO).getOrElse("https://devoxx.be/faq/"),
      registration = Configuration.getKeyValue(Configuration.CONFIG_URL_REGISTRATION).getOrElse("https://reg.devoxx.be"),
      confWebsite = Configuration.getKeyValue(Configuration.CONFIG_URL_WEBSITE).getOrElse("https://devoxx.be/"),
      cfpHostname = Configuration.getKeyValue(Configuration.CONFIG_URL_CFP_HOSTNAME).getOrElse("cfp.devoxx.be"),
      sponsors = Configuration.getKeyValue(Configuration.CONFIG_URL_SPONSORS).getOrElse("https://devoxx.be/sponsors-2017")
    ),
    timing = ConferenceTiming(
      datesI18nKey = Configuration.getKeyValue(Configuration.CONFIG_TIMING_DATES).getOrElse("6th-10th November"),
      speakersPassDuration = 5,
      preferredDayEnabled = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_PREFERRED_DAY_ENABLED),
      firstDayFr = Configuration.getKeyValue(Configuration.CONFIG_TIMING_FIRST_DAY_FR).getOrElse("6 novembre"),
      firstDayEn = Configuration.getKeyValue(Configuration.CONFIG_TIMING_FIRST_DAY_EN).getOrElse("november 6th"),
      datesFr = Configuration.getKeyValue(Configuration.CONFIG_TIMING_DATES_FR).getOrElse("du 6 au 11 Novembre 2017"),
      datesEn = Configuration.getKeyValue(Configuration.CONFIG_TIMING_DATES_EN).getOrElse("from 6th to 11th of November, 2017"),
      cfpOpenedOn = DateTime.parse(Configuration.getKeyValue(Configuration.CONFIG_TIMING_CFP_OPEN).getOrElse("2017-05-22T00:00:00+02:00")),
      cfpClosedOn = DateTime.parse(Configuration.getKeyValue(Configuration.CONFIG_TIMING_CFP_CLOSED).getOrElse("2017-07-07T23:59:59+02:00")),
      scheduleAnnouncedOn = DateTime.parse(Configuration.getKeyValue(Configuration.CONFIG_TIMING_SCHEDULE_DATE).getOrElse("2017-09-15T00:00:00+02:00")),
      days=dateRange(fromDay, toDay, new Period().withDays(1))
    ),
    hosterName = Configuration.getKeyValue(Configuration.CONFIG_HOST_NAME).getOrElse("Clever-cloud"),
    hosterWebsite = Configuration.getKeyValue(Configuration.CONFIG_HOST_WEBSITE).getOrElse("http://www.clever-cloud.com/#DevoxxBE"),
    hashTag = "#Devoxx",
    conferenceSponsor = ConferenceSponsor(
      showSponsorProposalCheckbox = true,
      sponsorProposalType = ConferenceProposalTypes.CONF
    )
    , List(Locale.ENGLISH)
    , "Metropolis Antwerp, Groenendaallaan 394, 2030 Antwerp,Belgium"
    // Do not send an email for each talk submitted for France
    , notifyProposalSubmitted = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_NOTIFY_NEW_PROPOSAL)
    , 1200 // French developers tends to be a bit verbose... we need extra space :-)
  )

  def conference2016() = ConferenceDescriptor(
    eventCode = "DV16",
    // You will need to update conf/routes files with this code if modified
    confUrlCode = "devoxxbe2016",
    frLangEnabled = false,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("info@devoxx.com"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("program@devoxx.com"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("nicolas.martignole@devoxx.fr"),
    conferenceUrls = ConferenceUrls(
      info = "https://devoxx.be/faq/",
      registration = "http://reg.devoxx.be",
      confWebsite = "https://devoxx.be/",
      cfpHostname = Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.be"),
      sponsors = "https://devoxx.be/sponsors-2016"
    ),
    timing = ConferenceTiming(
      datesI18nKey = "7th-11th November",
      speakersPassDuration = 5,
      preferredDayEnabled = true,
      firstDayFr = "7 novembre",
      firstDayEn = "november 7th",
      datesFr = "du 7 au 11 Novembre 2016",
      datesEn = "from 7th to 11th of November, 2016",
      cfpOpenedOn = DateTime.parse("2016-05-23T00:00:00+02:00"),
      cfpClosedOn = DateTime.parse("2016-07-06T23:59:59+02:00"),
      scheduleAnnouncedOn = DateTime.parse("2016-09-15T00:00:00+02:00"),
      days=dateRange(fromDay,toDay,new Period().withDays(1))
    ),
    hosterName = "Clever-cloud", hosterWebsite = "http://www.clever-cloud.com/#DevoxxBE",
    hashTag = "#Devoxx",
    conferenceSponsor = ConferenceSponsor(showSponsorProposalCheckbox = true, sponsorProposalType = ConferenceProposalTypes.CONF)
    , List(Locale.ENGLISH)
    , "Metropolis Antwerp, Groenendaallaan 394, 2030 Antwerp,Belgium"
    , notifyProposalSubmitted = false // Do not send an email for each talk submitted for France
    , 1200 // French developers tends to be a bit verbose... we need extra space :-)
  )

  def isCFPOpen: Boolean = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_CFP_OPEN)

  // All timezone sensitive methods are using this constant variable.
  // Defaults to "Europe/London" if not set in the Clever Cloud env. variables page.
  def timeZone: String = Configuration.getKeyValue(Configuration.CONFIG_SETTINGS_TIMEZONE).getOrElse("Europe/Brussels")

  def isGoldenTicketActive:Boolean = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_GOLDEN_TICKET_ACTIVE)

  def isFavoritesSystemActive:Boolean = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_ACTIVATE_FAVORITES)

  def isHTTPSEnabled:Boolean = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_HTTPS_ENABLE)

  // Reset all votes when a Proposal with state=SUBMITTED (or DRAFT) is updated
  // This is to reflect the fact that some speakers are eavluated, then they update the talk, and we should revote for it
  def isResetVotesForSubmitted:Boolean = Configuration.getKeyBoolean(Configuration.CONFIG_SETTINGS_RESET_VOTES_FOR_SUBMITTED)

  // The Gluon Push notification credentials
  def gluonAuthorization(): String = Play.current.configuration.getString("gluon.authorization").getOrElse("")
  def gluonUsername(): String = Play.current.configuration.getString("gluon.username").getOrElse("")
  def gluonPassword(): String = Play.current.configuration.getString("gluon.password").getOrElse("")

  // Define the maximum number of proposals a speaker can submit
  def maxProposals(): Int = Play.current.configuration.getInt("max.proposals").getOrElse(5)
}
