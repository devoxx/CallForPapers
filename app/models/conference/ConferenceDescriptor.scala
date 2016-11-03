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

import java.util.Locale

import models._
import org.joda.time.{DateTime, Period}
import play.api.Play


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
/**
  * TODO customize this file for Devoxx US
  */
object ConferenceDescriptor {

  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  val fromDay: DateTime = new DateTime().withYear(2017).withMonthOfYear(2).withDayOfMonth(21)
  val toDay: DateTime = new DateTime().withYear(2017).withMonthOfYear(2).withDayOfMonth(23)

  def current(): ConferenceDescriptor = new ConferenceDescriptor(
    eventCode = "DVUS17",
    // You will need to update conf/routes files with this code if modified
    confUrlCode = "devoxxus2017",
    frLangEnabled = false,
    fromEmail = Play.current.configuration.getString("mail.from").getOrElse("info@devoxx.us"),
    committeeEmail = Play.current.configuration.getString("mail.committee.email").getOrElse("program@devoxx.us"),
    bccEmail = Play.current.configuration.getString("mail.bcc"),
    bugReportRecipient = Play.current.configuration.getString("mail.bugreport.recipient").getOrElse("nicolas.martignole@devoxx.fr"),
    conferenceUrls = ConferenceUrls(
      faq = "https://devoxx.us/faq/",
      registration = "https://www.regonline.com/register/checkin.aspx?MethodId=0&EventId=1845425",
      confWebsite = "https://devoxx.us/",
      cfpHostname = Play.current.configuration.getString("cfp.hostname").getOrElse("cfp.devoxx.us")
    ),
    timing = ConferenceTiming(
      datesI18nKey = "March 21 - 23, 2017",
      speakersPassDuration = 3,
      preferredDayEnabled = true,
      firstDayFr = "21 March",
      firstDayEn = "March, 21st",
      datesFr = "du 21 au 23 mars 2017",
      datesEn = "March 21 - 23, 2017",
      cfpOpenedOn = DateTime.parse("2016-09-01T00:00:00+09:00"),
      cfpClosedOn = DateTime.parse("2016-10-11T23:59:59+09:00"),
      scheduleAnnouncedOn = DateTime.parse("2016-11-16T00:00:00+09:00"),
      days = dateRange(fromDay, toDay, new Period().withDays(1))
    ),
    hosterName = "Clever-cloud", hosterWebsite = "http://www.clever-cloud.com/#DevoxxUS",
    hashTag = "#DevoxxUS",
    conferenceSponsor = ConferenceSponsor(showSponsorProposalCheckbox = true, sponsorProposalType = ConferenceProposalTypes.CONF)
    , List(Locale.ENGLISH)
    , "San Jose, USA"
    // Do we want to send an email for each talk submitted
    , notifyProposalSubmitted = Play.current.configuration.getBoolean("email.on.new.proposal").getOrElse(false)
    , 1200
  )

  def isCFPOpen: Boolean = {
    Play.current.configuration.getBoolean("cfp.isOpen").getOrElse(false)
  }

  def isGoldenTicketActive: Boolean = Play.current.configuration.getBoolean("goldenTicket.active").getOrElse(false)

  def isFavoritesSystemActive: Boolean = Play.current.configuration.getBoolean("cfp.activateFavorites").getOrElse(false)

  def isHTTPSEnabled: Boolean = Play.current.configuration.getBoolean("cfp.activateHTTPS").getOrElse(false)

  // Reset all votes when a Proposal with state=SUBMITTED (or DRAFT) is updated
  // This is to reflect the fact that some speakers are eavluated, then they update the talk, and we should revote for it
  def isResetVotesForSubmitted = Play.current.configuration.getBoolean("cfp.resetVotesForSubmitted").getOrElse(false)

}