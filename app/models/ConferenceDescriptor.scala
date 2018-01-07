package models

import java.util.Locale
import java.nio.file.{Paths, Files,Path}
import library.Redis
import org.joda.time.{DateTime, DateTimeZone, Period}
import play.api.Play
import play.api.libs.json.{Format, Json}

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

case class ConferenceUrls(faq: String, registration: String, confWebsite: String, cfpHostname: String) {
  def cfpURL(): String = {
    if (Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)) {
      s"https://$cfpHostname"
    } else {
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
                             days: Iterator[DateTime]
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
                                maxProposalSummaryCharacters: Int = 1200,
                                isHTTPSEnabled: Boolean = false
                               )

object ConferenceDescriptor {
  var cfpopen = true

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

    val TRN = ProposalType(id = "trn", label = "trn.label")

    val ALL = List(CONF, UNI, TIA, LAB, QUICK, BOF, KEY, TRN)

    def valueOf(id: String): ProposalType = id match {
      case "conf" => CONF
      case "uni" => UNI
      case "tia" => TIA
      case "lab" => LAB
      case "quick" => QUICK
      case "bof" => BOF
      case "key" => KEY
      case "trn" => TRN
    }
    def getallPropTypes: Seq[(String , String)]= {
      val allrom = Room.allRoom.map( x=>
        x._2 match {
          case Some(a)=>(a.id, a.name)

        }
      )
      allrom.toSeq



    }

  }

  // TODO Configure here the slot, with the number of slots available, if it gives a free ticket to the speaker, some CSS icons
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
    val BOF = ProposalConfiguration(id = "bof", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.BOF.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-group",
      chosablePreferredDay = false)
    val KEY = ProposalConfiguration(id = "key", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.KEY.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val TRN = ProposalConfiguration(id = "trn", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.TRN.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "graduation-cap",
      chosablePreferredDay = true)

    val ALL = List(CONF, UNI, TIA, LAB, QUICK, BOF, KEY, TRN)

    def doesItGivesSpeakerFreeEntrance(proposalType: ProposalType): Boolean = {
      ALL.filter(_.id == proposalType.id).exists(_.givesSpeakerFreeEntrance)
    }
  }

  // TODO Configure here your Conference's tracks.
  object ConferenceTracks {
    val JAVA = Track("java", "java.label")
    val MOBILE = Track("mobile", "mobile.label")
    val WEB = Track("wm", "web.label")
    val ARCHISEC = Track("archisec", "archisec.label")
    val CLOUD = Track("cldops", "cloud.label")
    val AGILE_DEVOPS = Track("agTest", "agile_devops.label")
    val BIGDATA = Track("bigd", "bigdata.label")
    val FUTURE = Track("future", "future.label")
    val LANG = Track("lang", "lang.label")
    val Startup = Track("Startup", "Startup.label")
    val UNKNOWN = Track("unknown", "unknown track")
    val ALL = List(AGILE_DEVOPS,ARCHISEC,BIGDATA,CLOUD, FUTURE, JAVA, LANG,MOBILE, WEB, Startup  ,UNKNOWN)
  }

  // TODO configure the description for each Track
  object ConferenceTracksDescription {
    val JAVA = TrackDesc(ConferenceTracks.JAVA.id, "/assets/dvfr2015/images/ServerSideJava.png", "track.java.title", "track.java.desc")
    val MOBILE = TrackDesc(ConferenceTracks.MOBILE.id, "/assets/dvfr2015/images/icon_mobile.png", "track.mobile.title", "track.mobile.desc")
    val WEB = TrackDesc(ConferenceTracks.WEB.id, "/assets/dvfr2015/images/icon_web.png", "track.web.title", "track.web.desc")
    val ARCHISEC = TrackDesc(ConferenceTracks.ARCHISEC.id, "/assets/dvfr2015/images/icon_architecture.png", "track.archisec.title", "track.archisec.desc")
    val AGILE_DEVOPS = TrackDesc(ConferenceTracks.AGILE_DEVOPS.id, "/assets/dvfr2015/images/icon_methodology.png", "track.agile_devops.title", "track.agile_devops.desc")
    val CLOUD = TrackDesc(ConferenceTracks.CLOUD.id, "/assets/dvfr2015/images/icon_cloud.png", "track.cloud.title", "track.cloud.desc")
    val BIGDATA = TrackDesc(ConferenceTracks.BIGDATA.id, "/assets/dvfr2015/images/CloudBigData.png", "track.bigdata.title", "track.bigdata.desc")
    val FUTURE = TrackDesc(ConferenceTracks.FUTURE.id, "/assets/dvfr2015/images/icon_future.png", "track.future.title", "track.future.desc")
    val LANG = TrackDesc(ConferenceTracks.LANG.id, "/assets/dvfr2015/images/icon_alternative.png", "track.lang.title", "track.lang.desc")
    val STURTUP = TrackDesc(ConferenceTracks.Startup.id, "/assets/dvfr2015/images/icon_startup.png", "track.Startup.label.title", "track.Startup.label.title.desc")
    val ALL = List(JAVA, MOBILE, WEB, ARCHISEC, AGILE_DEVOPS, CLOUD, BIGDATA, FUTURE, LANG,STURTUP)

    def findTrackDescFor(t: Track): TrackDesc = {
      ALL.find(_.id == t.id).getOrElse(JAVA)
    }
  }

  // TODO If you want to use the Devoxx Scheduler, you can describe here the list of rooms, with capacity for seats
  object ConferenceRooms {

    // Tip : I use the ID to sort-by on the view per day... So if the exhibition floor id is "aaa" it will be
    // the first column on the HTML Table
    val HALL_EXPO = Room("a_hall", "Exhibition floor", 2300, "special", "")
    val Room1 = Room("room_01", "Oud", 86, "classroom", "rien")
    val Room2 = Room("room_02", "Canelle", 64, "classroom", "rien")
    val Room3 = Room("room_03", "Citron", 96, "classroom", "rien")
    val Room4 = Room("room_04", "Verveine", 72, "classroom", "rien")
    val Room5 = Room("room_05", "Jasmine", 48, "classroom", "rien")
    val Room6 = Room("room_06", "Anfa Ballroom", 500, "theatre", "camera")
    val Room7 = Room("room_07", "Santal", 40, "classroom", "rien")
    val Room8 = Room("room_08", "Musk", 26, "classroom", "rien")

    def allRooms:List[Room]={
      //var list :List[Room] = List.empty[Room]
      //Room.allRoom.map(x=>x._2 match {
      //  case Some(a)=>  list = a::list
      //  case None=>

      //})
      //list
      List(Room1, Room2, Room3, Room4, Room5, Room6, Room7, Room8)
    }

  }

  // TODO if you want to use the Scheduler, you can configure the breaks
  object ConferenceSlotBreaks {
    val registration = SlotBreak("reg", "Registration", "Accueil", ConferenceRooms.HALL_EXPO)
    val petitDej = SlotBreak("dej", "Breakfast", "Accueil et petit-déjeuner", ConferenceRooms.HALL_EXPO)
    val coffee = SlotBreak("coffee", "Coffee Break", "Pause café", ConferenceRooms.HALL_EXPO)
    val lunch = SlotBreak("lunch", "Lunch", "Pause déjeuner", ConferenceRooms.HALL_EXPO)
    val shortBreak = SlotBreak("chgt", "Break", "Pause courte", ConferenceRooms.HALL_EXPO)
    val exhibition = SlotBreak("exhib", "Exhibition", "Exhibition", ConferenceRooms.HALL_EXPO)
    val meetAndGreet = SlotBreak("meet", "Meet & Greet", "Exhibition", ConferenceRooms.HALL_EXPO)
    val allSlotBreak=List(registration,petitDej,coffee,lunch,shortBreak,exhibition,meetAndGreet)
  }

  // TODO The idea here is to describe in term of Agenda, for each rooms, the slots. This is required only for the Scheduler
  object ConferenceSlots {


    val firstDay = "2017-11-14"
    val secondDay = "2017-11-15"
    val thirdDay = "2017-11-16"
    val timezone = "Africa/Casablanca"

val conferenceday=Seq(("tuesday","tuesday"),("wednesday","wednesday"),("thursday","thursday"))

    val tuesdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, "tuesday",
        new DateTime(s"${firstDay}T07:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "tuesday",
        new DateTime(s"${firstDay}T12:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "tuesday",
        new DateTime(s"${firstDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)))
    )

    // Registration, coffee break, lunch etc
    val wednesdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, "wednesday",
        new DateTime(s"${secondDay}T07:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T09:00:00.000+00:00"))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "wednesday",
        new DateTime(s"${secondDay}T12:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:00:00.000+00:00"))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "wednesday",
        new DateTime(s"${secondDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T16:30:00.000+00:00"))
    )

    val thursdayBreaks = List(
      SlotBuilder(ConferenceSlotBreaks.registration, "thursday",
        new DateTime(s"${thirdDay}T07:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T09:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)))
      , SlotBuilder(ConferenceSlotBreaks.lunch, "thursday",
        new DateTime(s"${thirdDay}T12:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)))
      , SlotBuilder(ConferenceSlotBreaks.coffee, "thursday",
        new DateTime(s"${thirdDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)))
    )

    val tuesdayScheduleMock: List[Slot] = List(
      // 9:30 - 11:30
      SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday",
        new DateTime(s"${firstDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday",
        new DateTime(s"${firstDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      // 11:40 - 12:10
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T11:40:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T12:10:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T11:40:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T12:10:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T11:40:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T12:10:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T11:40:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T12:10:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      // 13:15 - 13:45
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "tuesday",
        new DateTime(s"${firstDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      // 14:00 - 16:00
      , SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday",
        new DateTime(s"${firstDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday",
        new DateTime(s"${firstDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T16:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      // 16:30 - 18:30
      , SlotBuilder(ConferenceProposalTypes.LAB.id, "tuesday",
        new DateTime(s"${firstDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T18:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T18:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T18:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.UNI.id, "tuesday",
        new DateTime(s"${firstDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T18:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      // 08:00 - 18:30
       , SlotBuilder(ConferenceProposalTypes.TRN.id, "tuesday",
        new DateTime(s"${firstDay}T08:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T18:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room7)
       , SlotBuilder(ConferenceProposalTypes.TRN.id, "tuesday",
        new DateTime(s"${firstDay}T08:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${firstDay}T18:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room8)
    )

    val wednesdayScheduleMock: List[Slot] = List(
      // 09:30 - 09:50
      SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
        new DateTime(s"${secondDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T09:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 09:55 - 10:15
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
        new DateTime(s"${secondDay}T09:55:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T10:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 10:20 - 10:40
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
        new DateTime(s"${secondDay}T10:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T10:40:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 10:45 - 11:05
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
        new DateTime(s"${secondDay}T10:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T11:05:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 11:10 - 11:30
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
        new DateTime(s"${secondDay}T11:10:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)

      // 11:30 - 12:20
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 13:15 - 13:45
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "wednesday",
        new DateTime(s"${secondDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "wednesday",
        new DateTime(s"${secondDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "wednesday",
        new DateTime(s"${secondDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "wednesday",
        new DateTime(s"${secondDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "wednesday",
        new DateTime(s"${secondDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      // 14:00 - 14:50
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 15:00 - 15:50
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 16:30 - 17:20
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "wednesday",
        new DateTime(s"${secondDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 17:30 - 17:50
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "wednesday",
        new DateTime(s"${secondDay}T17:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T17:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 18:00 - 18:15
      , SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday",
        new DateTime(s"${secondDay}T18:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday",
        new DateTime(s"${secondDay}T18:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday",
        new DateTime(s"${secondDay}T18:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday",
        new DateTime(s"${secondDay}T18:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.QUICK.id, "wednesday",
        new DateTime(s"${secondDay}T18:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      // 18:15 - 19:15
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday",
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T19:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday",
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T19:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday",
        new DateTime(s"${secondDay}T18:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T19:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday",
        new DateTime(s"${secondDay}T18:15:00.00+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T19:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "wednesday",
        new DateTime(s"${secondDay}T18:15:00.00+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${secondDay}T19:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
    )

    val thursdayScheduleMock: List[Slot] = List(
      // 9:30 - 9:50
      SlotBuilder(ConferenceProposalTypes.KEY.id, "thursday",
        new DateTime(s"${thirdDay}T09:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T09:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 10:00 - 10:20
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "thursday",
        new DateTime(s"${thirdDay}T10:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T10:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 10:30 - 11:20
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T10:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T11:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T10:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T11:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T10:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T11:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T10:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T11:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T10:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T11:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T10:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T11:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 11:30 - 12:20
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T11:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T12:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      // 13:15 - 13:45
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "thursday",
        new DateTime(s"${thirdDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "thursday",
        new DateTime(s"${thirdDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "thursday",
        new DateTime(s"${thirdDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.TIA.id, "thursday",
        new DateTime(s"${thirdDay}T13:15:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T13:45:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      // 14:00 - 14:50
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T14:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T14:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room7)
      // 15:00 - 15:50
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T15:00:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T15:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room7)
      // 16:30 - 17:20
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room1)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room2)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room3)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room4)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room5)
      , SlotBuilder(ConferenceProposalTypes.BOF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      , SlotBuilder(ConferenceProposalTypes.CONF.id, "thursday",
        new DateTime(s"${thirdDay}T16:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:20:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room7)
      // 17:30 - 18:10
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "thursday",
        new DateTime(s"${thirdDay}T17:30:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T17:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
      , SlotBuilder(ConferenceProposalTypes.KEY.id, "thursday",
        new DateTime(s"${thirdDay}T17:50:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)),
        new DateTime(s"${thirdDay}T18:10:00.000+00:00").toDateTime(DateTimeZone.forID(timezone)), ConferenceRooms.Room6)
    )

    def mondaySchedule: List[Slot] = { List.empty/** alldb.filter(x=>x.day=="monday" ) **/ }

    def tuesdaySchedule: List[Slot] = { tuesdayScheduleMock/** alldb.filter(x=>x.day=="tuesday" ) **/ }

    def wednesdaySchedule: List[Slot] = { wednesdayScheduleMock/** alldb.filter(x=>x.day=="wednesday" ) **/ }

    def thursdaySchedule: List[Slot] = { thursdayScheduleMock/** alldb.filter(x=>x.day=="thursday" ) **/ }

    def fridaySchedule: List[Slot] = { List.empty/** alldb.filter(x=>x.day=="friday" ) **/ }

    def alldb:List[Slot]={
      var list :List[Slot] = List.empty[Slot]
      Slot.allSlot.map(x=>x._2 match {
        case Some(a)=>  list =a::list
        case None=>
      })
        list
    }

    def all: List[Slot] = {
      mondaySchedule ++ tuesdaySchedule ++ wednesdaySchedule ++ thursdaySchedule ++ fridaySchedule
    }
  }



  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  val fromDay = new DateTime().withYear(2017).withMonthOfYear(11).withDayOfMonth(5)
  val toDay = new DateTime().withYear(2017).withMonthOfYear(11).withDayOfMonth(7)

  def getFullRoutePath(route: String) = {
    val isHTTPSEnabled = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)
    val hostname = Play.current.configuration.getString("cfp.hostname").getOrElse("localhost:9000")
    val protocol = if(isHTTPSEnabled) "https" else "http";
    s"$protocol://$hostname$route"
  }

  // TODO You might want to start here and configure first, your various Conference Elements
  def current() = ConferenceDescriptor(
    eventCode = "DevoxxMA2017",
    // You will need to update conf/routes files with this code if modified
    confUrlCode = "devoxxma2017",
    frLangEnabled = true,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("program@devoxx.ma"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("program@devoxx.ma"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("nicolas.martignole@devoxx.ma"),
    conferenceUrls = ConferenceUrls(
      faq = "http://www.devoxx.ma/faq",
      registration = "https://reg.devoxx.ma",
      confWebsite = "http://www.devoxx.ma/",
      cfpHostname = {
        val h=Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.ma")
        if(h.endsWith("/")){
          h.substring(0,h.length - 1)
        }else{
          h
        }
      }
    ),
    timing = ConferenceTiming(
      datesI18nKey = "14 au 16 novembre 2017",
      speakersPassDuration = 3,
      preferredDayEnabled = true,
      firstDayFr = "14 novembre",
      firstDayEn = "november 14th",
      datesFr = "du 14 au 16 Novembre 2017",
      datesEn = "from 14th to 16th of November, 2017",
      cfpOpenedOn = DateTime.parse("2017-06-05T00:00:00+02:00"),
      cfpClosedOn = DateTime.parse("2017-07-19T23:59:00+02:00"),
      scheduleAnnouncedOn = DateTime.parse("2017-09-25T00:00:00+02:00"),
      days = dateRange(fromDay, toDay, new Period().withDays(1))
    ),

    hosterName = "xHub", hosterWebsite = "http://x-hub.io/",

    hashTag = "#DevoxxMA",
    conferenceSponsor = ConferenceSponsor(showSponsorProposalCheckbox = true, sponsorProposalType = ConferenceProposalTypes.CONF)
    , List(Locale.FRENCH)
    , "Palais des Congrès, Porte Maillot, Paris"
    , 1200 // French developers tends to be a bit verbose... we need extra space :-)
  )

  // It has to be a def, not a val, else it is not re-evaluated
  def verifyopeningcfp : Any = {
    if (DateTime.now().isBefore(ConferenceDescriptor.current().timing.cfpOpenedOn) || DateTime.now().isAfter(ConferenceDescriptor.current().timing.cfpClosedOn) ){
      CfpManager.updateCfpStatut(CfpManager.getCfpStatut("cfp").get , false) } else {
      CfpManager.updateCfpStatut(CfpManager.getCfpStatut("cfp").get , true)
    }
  }
  def isCFPOpen: Boolean = {
    //Play.current.configuration.getBoolean("cfp.isOpen").getOrElse(false)
    //cfpopen
    CfpManager.checkStatut("cfp")
  }


  def isGoldenTicketActive: Boolean = Play.current.configuration.getBoolean("goldenTicket.active").getOrElse(false)

  def isTagSystemActive: Boolean = Play.current.configuration.getBoolean("cfp.tags.active").getOrElse(false)

  def isFavoritesSystemActive: Boolean = Play.current.configuration.getBoolean("cfp.activateFavorites").getOrElse(false)

  def isHTTPSEnabled = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)

  // Reset all votes when a Proposal with state=SUBMITTED (or DRAFT) is updated
  // This is to reflect the fact that some speakers are eavluated, then they update the talk, and we should revote for it
  def isResetVotesForSubmitted = Play.current.configuration.getBoolean("cfp.resetVotesForSubmitted").getOrElse(false)

  // Set this to true temporarily
  // I will implement a new feature where each CFP member can decide to receive one digest email per day or a big email
  def notifyProposalSubmitted = Play.current.configuration.getBoolean("cfp.notifyProposalSubmitted").getOrElse(false)

  // For practical reason we want to hide the room and the time slot until the full agenda is published
  def isShowRoomAndTimeslot:Boolean = Play.current.configuration.getBoolean("cfp.showRoomAndTimeslot").getOrElse(false)

  def isShowRoom:Boolean = Play.current.configuration.getBoolean("cfp.showRoom").getOrElse(false)

  // My Devoxx is an OAuth provider on which a user can register
  def isMyDevoxxActive:Boolean = Play.current.configuration.getBoolean("mydevoxx.active").getOrElse(false)

  def myDevoxxURL():String = Play.current.configuration.getString("mydevoxx.url").getOrElse("https://my.devoxx.ma")

  // This is a JWT String shared secret that needs to be configured as a global environment variable
  def jwtSharedSecret() : String = Play.current.configuration.getString("mydevoxx.jwtSharedSecret").getOrElse("change me please")

  // Use Twilio (SMS service) to send notification to all speakers and to recieve also commands
  def isTwilioSMSActive():Boolean = Play.current.configuration.getBoolean("cfp.twilioSMS.active").getOrElse(false)

  def twilioAccountSid:String =  Play.current.configuration.getString("cfp.twilioSMS.accountSid").getOrElse("")

  def twilioAuthToken:String =  Play.current.configuration.getString("cfp.twilioSMS.authToken").getOrElse("")

  def twilioSenderNumber:String =  Play.current.configuration.getString("cfp.twilioSMS.senderNumber").getOrElse("")

  def twilioMockSMS:Boolean =  Play.current.configuration.getBoolean("cfp.twilioSMS.mock").getOrElse(true)
}
case class CfpManager (id: String = "cfp" ,cfpopen: Boolean = false){

}
object CfpManager {
  implicit val cfpManagerFormat: Format[CfpManager] = Json.format[CfpManager]

  def save(cfpManager: CfpManager) = Redis.pool.withClient {
    client =>

      val jsonCfpManager = Json.stringify(Json.toJson(cfpManager))
      client.hset("CfpManager", cfpManager.id, jsonCfpManager)

  }

  def checkStatut(id: String = "cfp"): Boolean = Redis.pool.withClient {
    client =>
      val maybecfp = getCfpStatut(id)
      if (maybecfp.exists(_.cfpopen.equals(true))) {
        true
      } else {
        false
      }

  }

  def cfpKeyIsExist(): Boolean = Redis.pool.withClient {
    client =>
      client.exists("CfpManager")
  }

  def getCfpStatut(id: String): Option[CfpManager] = Redis.pool.withClient {
    client =>
      client.hget("CfpManager", id).map {
        json: String =>
          Json.parse(json).as[CfpManager]
      }

  }

  def update(cfpManager: CfpManager) = Redis.pool.withClient {
    client =>

      val json = Json.stringify(Json.toJson(cfpManager.copy(id = "cfp")))
      client.hset("CfpManager", cfpManager.id, json)

  }

  def updateCfpStatut(cfpManager: CfpManager, closeOropen: Boolean) = Redis.pool.withClient {
    client =>
      val updatCfpStatut = cfpManager.copy(cfpopen = closeOropen)
      update(updatCfpStatut)

  }

  def cfpRedisExist(): Any = {
    if (!CfpManager.cfpKeyIsExist()) {
      var cfp = new CfpManager
      CfpManager.save(cfp)
    } else {
      None
    }
  }

  def testCfpImageBase(): Any = {
    Play.current.configuration.getString("cfp.imageBase") match {
      case Some(folder) =>

        if (Files.exists(Paths.get(folder)) == false) {
          val folderPath: Path = Paths.get(folder)
          var tmpDir: Path = Files.createDirectory(folderPath)
        }
      case None =>
    }

  }
  def testemailTemplate():Any= MailsManager.getEmailMode() match{

    case None=> MailsManager.saveEmailMode("disable")
    case Some(a)=>MailsManager.saveEmailMode(a)
  }

}
