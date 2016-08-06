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

import models.ProposalType

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

