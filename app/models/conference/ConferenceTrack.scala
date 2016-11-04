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

import models.{Track, TrackDesc}

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
  val METHOD_ARCHI = TrackDesc(ConferenceTracks.METHOD_ARCHI.id, "/assets/devoxxus2017/images/icon_methodology.png", ConferenceTracks.METHOD_ARCHI.label, "track.method_archi.desc")
  val JAVA = TrackDesc(ConferenceTracks.JAVA.id, "/assets/devoxxus2017/images/icon_javase.png", ConferenceTracks.JAVA.label, "track.java.desc")
  val CLOUD = TrackDesc(ConferenceTracks.CLOUD.id, "/assets/devoxxus2017/images/icon_cloud.png", ConferenceTracks.CLOUD.label, "track.cloud.desc")
  val SSJ = TrackDesc(ConferenceTracks.SSJ.id, "/assets/devoxxus2017/images/icon_javaee.png", ConferenceTracks.SSJ.label, "track.ssj.desc")
  val LANG = TrackDesc(ConferenceTracks.LANG.id, "/assets/devoxxus2017/images/icon_alternative.png", ConferenceTracks.LANG.label, "track.lang.desc")
  val BIGDATA = TrackDesc(ConferenceTracks.BIGDATA.id, "/assets/devoxxus2017/images/icon_architecture.png", ConferenceTracks.BIGDATA.label, "track.bigdata.desc")
  val WEB = TrackDesc(ConferenceTracks.WEB.id, "/assets/devoxxus2017/images/icon_web.png", ConferenceTracks.WEB.label, "track.web.desc")
  val FUTURE = TrackDesc(ConferenceTracks.FUTURE.id, "/assets/devoxxus2017/images/icon_future.png", ConferenceTracks.FUTURE.label, "track.future.desc")
  val MOBILE = TrackDesc(ConferenceTracks.MOBILE.id, "/assets/devoxxus2017/images/icon_mobile.png", ConferenceTracks.MOBILE.label, "track.mobile.desc")

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