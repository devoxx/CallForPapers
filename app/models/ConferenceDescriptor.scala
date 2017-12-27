package models

import java.util.Locale

import controllers.Backoffice
import java.nio.file.{Files, Path, Paths}

import library.Redis
import models.ConferenceDescriptor.ConferenceProposalConfigurations.{CLOSING_KEY, OPENING_KEY}
import models.ConferenceDescriptor.ConferenceSlotBreaks._
import org.joda.time.{DateTime, DateTimeZone, Period}
import play.api.Play
import play.api.libs.json.{Format, Json}
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
                                notifyProposalSubmitted: Boolean,
                                maxProposalSummaryCharacters: Int=1200,
                                isHTTPSEnabled: Boolean = false
                               )

object ConferenceDescriptor {
  var cfpopen = true

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

    val OTHER = ProposalType(id = "other", label = "other.label")

    val ALL = List(CONF, LAB, DEEP_DIVE, QUICK, BOF, OPENING_KEY, CLOSING_KEY, IGNITE, OTHER)

    def valueOf(id: String): ProposalType = id match {
      case "conf" => CONF
      case "lab" => LAB
      case "deep_dive" => DEEP_DIVE
      case "quick" => QUICK
      case "bof" => BOF
      case "opening_key" => OPENING_KEY
      case "closing_key" => CLOSING_KEY
      case "ignite" => IGNITE
      case "other" => OTHER
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
    val LAB = ProposalConfiguration(id = "lab", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.LAB.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-beaker",
      chosablePreferredDay = true)
    val DEEP_DIVE = ProposalConfiguration(id = "deep_dive", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.DEEP_DIVE.id)), givesSpeakerFreeEntrance = true, freeEntranceDisplayed = true, htmlClass = "icon-anchor",
      chosablePreferredDay = true)
    val QUICK = ProposalConfiguration(id = "quick", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.QUICK.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-fast-forward",
      chosablePreferredDay = true)
    val BOF = ProposalConfiguration(id = "bof", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.BOF.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-group",
      chosablePreferredDay = false)
    val OPENING_KEY = ProposalConfiguration(id = "opening_key", slotsCount = 1, givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val CLOSING_KEY = ProposalConfiguration(id = "closing_key", slotsCount = 1, givesSpeakerFreeEntrance = true, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = true)
    val IGNITE = ProposalConfiguration(id = "ignite", slotsCount = ConferenceSlots.all.count(_.name.equals(ConferenceProposalTypes.IGNITE.id)), givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      chosablePreferredDay = false)
    val OTHER = ProposalConfiguration(id = "other", slotsCount = 1, givesSpeakerFreeEntrance = false, freeEntranceDisplayed = false, htmlClass = "icon-microphone",
      hiddenInCombo = true, chosablePreferredDay = false)

    val ALL = List(CONF, LAB, QUICK, BOF, OPENING_KEY, CLOSING_KEY, IGNITE, OTHER)

    def doesItGivesSpeakerFreeEntrance(proposalType: ProposalType): Boolean = {
      ALL.filter(_.id == proposalType.id).exists(_.givesSpeakerFreeEntrance)
    }
  }

  // TODO Configure here your Conference's tracks.
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

  val DEVOXX_CONF_URL_CODE = "devoxxuk"

  // TODO configure the description for each Track
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
    val GALLERY_HALL = Room("a_gallery_hall", "Gallery Hall", 1180, "special", "")

    val HALL_EXPO = Room("z_hall", "Exhibition floor", 1500, "special", "")

    val AUDIT = Room("aud_room", "Auditorium", 300, "theatre", "")

    val EXEC_CENTRE = Room("exec_centre", "Exec Centre", 120, "classroom", "")

    val ROOM_A = Room("room1", "Room A", 190, "classroom", "")
    val ROOM_BC = Room("room2", "Room B/C", 120, "classroom", "")
    val ROOM_DEFG = Room("room3", "Room D/E/F/G", 250, "classroom", "")

    val LAB_ROOM_A = Room("x_lab_room1", "Lab Room A", 50, "classroom", "")
    val LAB_ROOM_B = Room("x_lab_room2", "Lab Room B", 50, "classroom", "")
    val LAB_ROOM_C = Room("x_lab_room3", "Lab Room C", 50, "classroom", "")
    val LAB_ROOM_D = Room("x_lab_room4", "Lab Room D", 50, "classroom", "")

    val keynoteRoom = List(GALLERY_HALL)

    val conferenceRooms = List(GALLERY_HALL, AUDIT, ROOM_A, ROOM_BC, ROOM_DEFG, EXEC_CENTRE)

    val deepDiveWed = List(AUDIT, LAB_ROOM_A)
    val labsWed = List(LAB_ROOM_A, LAB_ROOM_B, LAB_ROOM_C, LAB_ROOM_D)

    val bofThu = List(AUDIT, ROOM_A, ROOM_DEFG, EXEC_CENTRE)
    val igniteThu = List(ROOM_BC)
    val quickieThu = List(GALLERY_HALL, AUDIT, ROOM_A, ROOM_BC, ROOM_DEFG, EXEC_CENTRE)

    val quickieFri = List(GALLERY_HALL, AUDIT, ROOM_A, ROOM_BC, ROOM_DEFG, EXEC_CENTRE)
    
    def allRooms:List[Room]={
      var list :List[Room] = List.empty[Room]
      Room.allRoom.map(x=>x._2 match {
        case Some(a)=>  list = a::list
        case None=>

      })
      list
    }
  }

  // TODO if you want to use the Scheduler, you can configure the breaks
  object ConferenceSlotBreaks {
    val registrationAndCoffee = SlotBreak("reg", "Registration & Coffee", "Accueil", ConferenceRooms.HALL_EXPO)
    val breakfast = SlotBreak("dej", "Breakfast", "Accueil et petit-déjeuner", ConferenceRooms.HALL_EXPO)
    val coffee = SlotBreak("coffee", "Coffee Break", "Pause café", ConferenceRooms.HALL_EXPO)
    val lunch = SlotBreak("lunch", "Lunch", "Pause déjeuner", ConferenceRooms.HALL_EXPO)
    val eveningReception = SlotBreak("reception", "Evening Reception", "Evening Reception", ConferenceRooms.HALL_EXPO)
    val closingKeynote = SlotBreak("closeKey", "Closing Keynote", "Keynote", ConferenceRooms.AUDIT)
    val allSlotBreak=List(registrationAndCoffee, breakfast, coffee, lunch, eveningReception, closingKeynote)
  }

  // TODO The idea here is to describe in term of Agenda, for each rooms, the slots. This is required only for the Scheduler
  object ConferenceSlots {

    // VARIABLE CONSTANTS

    private val WEDNESDAY: String = "wednesday"
    private val THURSDAY: String = "thursday"
    private val FRIDAY: String = "friday"

    private val WED_DATE = "2018-05-09T"
    private val THU_DATE = "2018-05-10T"
    private val FRI_DATE = "2018-05-11T"

    val firstDay = WED_DATE
    val secondDay = THU_DATE
    val thirdDay = FRI_DATE

    val conferenceday=Seq(("tuesday","tuesday"),("wednesday","wednesday"),("thursday","thursday"))

    def mondaySchedule: List[Slot] = { alldb.filter(x=>x.day=="monday" )  }

    def tuesdaySchedule: List[Slot] = {  alldb.filter(x=>x.day=="tuesday" ) }

    def wednesdaySchedule: List[Slot] = { alldb.filter(x=>x.day=="wednesday" ) }

    def thursdaySchedule: List[Slot] = { alldb.filter(x=>x.day=="thursday" ) }

    def fridaySchedule: List[Slot] = { alldb.filter(x=>x.day=="friday" ) }

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

  val fromDay = new DateTime().withYear(2018).withMonthOfYear(5).withDayOfMonth(9)
  val toDay = new DateTime().withYear(2018).withMonthOfYear(5).withDayOfMonth(11)

  val MAXIMUM_SUMMARY_CHARACTERS = 1200

  val DEVOXX_EVENT_CODE = "DV18"
  val DEVOXX_HASH_TAG = "#DevoxxUK"
  val DEVOXX_VENUE_ADDRESS = "Business Design Centre, 52 Upper St, London N1 0QH, United Kingdom"
  val DEVOXX_FIRST_DAY_IN_FRENCH = "9 mai"
  val DEVOXX_FIRST_DAY_IN_ENGLISH = "May 9th"
  val DEVOXX_DAY_RANGE_IN_FRENCH = "mer 9 au 11 Mai 2018"
  val DEVOXX_DAY_RANGE_IN_ENGLISH = "from 9th to 11th of May, 2018"
  val DEVOXX_CONF_SITE_URL = "https://www.devoxx.co.uk/"
  val DEVOXX_INFO_SITE_URL = DEVOXX_CONF_SITE_URL + "#info"
  val DEVOXX_SPONSORS_SITE_URL = DEVOXX_CONF_SITE_URL + "#sponsors"
  val DEVOXX_TICKETS_SITE_URL = "https://www.eventbrite.co.uk/e/devoxx-uk-2018-tickets-39324055308"
  val DEVOXX_CFP_OPENED_ON_DATE = "2017-11-08T00:00:00+01:00"
  val DEVOXX_CFP_CLOSED_ON_DATE = "2018-01-09T23:59:59+01:00"
  val DEVOXX_SCHEDULE_ANNOUNCEMENT_DATE = "2018-01-23T00:00:00+01:00"

  def getFullRoutePath(route: String) = {
    val isHTTPSEnabled = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)
    val hostname = Play.current.configuration.getString("cfp.hostname").getOrElse("localhost:9000")
    val protocol = if(isHTTPSEnabled) "https" else "http";
    s"$protocol://$hostname$route"
  }

  // TODO You might want to start here and configure first, your various Conference Elements
  def current(): ConferenceDescriptor = new ConferenceDescriptor(
    eventCode = DEVOXX_EVENT_CODE,
    // You will need to update conf/routes files with this code if modified
    confUrlCode = DEVOXX_CONF_URL_CODE,
    frLangEnabled = false,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("info@devoxx.co.uk"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("program@devoxx.co.uk"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("info@devoxx.co.uk"),
    conferenceUrls = ConferenceUrls(
      info = DEVOXX_INFO_SITE_URL,
      sponsors = DEVOXX_SPONSORS_SITE_URL,
      registration = DEVOXX_TICKETS_SITE_URL,
      confWebsite = DEVOXX_CONF_SITE_URL,
      cfpHostname = {
        val h=Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.co.uk")
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
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("info@devoxx.co.uk"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("program@devoxx.co.uk"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("info@devoxx.co.uk"),
    conferenceUrls = ConferenceUrls(
      info = DEVOXX_INFO_SITE_URL,
      sponsors = DEVOXX_SPONSORS_SITE_URL,
      registration = DEVOXX_TICKETS_SITE_URL,
      confWebsite = DEVOXX_CONF_SITE_URL,
      cfpHostname = Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.co.uk")
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

  // It has to be a def, not a val, else it is not re-evaluated
  def verifyopeningcfp : Any = {
    if (DateTime.now().isBefore(ConferenceDescriptor.current().timing.cfpOpenedOn) || DateTime.now().isAfter(ConferenceDescriptor.current().timing.cfpClosedOn) ){
      CfpManager.updateCfpStatut(CfpManager.getCfpStatut("cfp").get , false) } else {
      CfpManager.updateCfpStatut(CfpManager.getCfpStatut("cfp").get , true)
    }
  }
  def isCFPOpen: Boolean = {
    Backoffice.isCFPOpen()
    //cfpopen
    //CfpManager.checkStatut("cfp")
  }

  // All timezone sensitive methods are using this constant variable.
  // Defaults to "Europe/London" if not set in the Clever Cloud env. variables page.
  def timeZone: String = Play.current.configuration.getString("conference.timezone").getOrElse("Europe/London")

  def isGoldenTicketActive: Boolean = Play.current.configuration.getBoolean("goldenTicket.active").getOrElse(false)

  def isFavoritesSystemActive: Boolean = Play.current.configuration.getBoolean("cfp.activateFavorites").getOrElse(false)
  def isTagSystemActive: Boolean = Play.current.configuration.getBoolean("cfp.tags.active").getOrElse(false)
  
  def isHTTPSEnabled: Boolean = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)

  // Reset all votes when a Proposal with state=SUBMITTED (or DRAFT) is updated
  // This is to reflect the fact that some speakers are eavluated, then they update the talk, and we should revote for it
  def isResetVotesForSubmitted = Play.current.configuration.getBoolean("cfp.resetVotesForSubmitted").getOrElse(false)

  // Set this to true temporarily
  // I will implement a new feature where each CFP member can decide to receive one digest email per day or a big email
  def notifyProposalSubmitted = Play.current.configuration.getBoolean("cfp.notifyProposalSubmitted").getOrElse(false)

  def gluonAuthorization(): String = Play.current.configuration.getString("gluon.authorization").getOrElse("")
  def gluonUsername(): String = Play.current.configuration.getString("gluon.username").getOrElse("")
  def gluonPassword(): String = Play.current.configuration.getString("gluon.password").getOrElse("")

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

  def capgeminiUsername(): String = {
    Play.current.configuration.getString("capgemini.username").getOrElse("")
  }

  def capgeminiPassword(): String = {
    Play.current.configuration.getString("capgemini.password").getOrElse("")
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
