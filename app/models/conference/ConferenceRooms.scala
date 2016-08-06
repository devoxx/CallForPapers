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

import models.Room

object ConferenceRooms {

  // Tip : I use the ID to sort-by on the view per day... So if the exhibition floor id is "aaa" it will be
  // the first column on the HTML Table
  val HALL_EXPO = Room("a_hall", "Exhibition floor", 1500, "special")

  // TODO Create rooms for San Jose Devoxx US conference
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

  val allRoomsTIA = List(ROOM4, ROOM5, ROOM8, ROOM9, ROOM10)

  val keynoteRoom = List(ROOM8, ROOM4, ROOM5, ROOM9)
  val eveningKeynoteRoom = List(ROOM8)

  val allRoomsConf = List(ROOM8, ROOM5, ROOM9, ROOM6, ROOM7, ROOM4, ROOM3, ROOM10)
  val fridayRoomsConf = List(ROOM4, ROOM5, ROOM8, ROOM9)

  val allRoomsQuick = List(ROOM8, ROOM5, ROOM9, ROOM6, ROOM7, ROOM4, ROOM3)

  val allRoomsLabs = List(BOF1, BOF2)
  val oneRoomLabs = List(BOF1)

  val allRoomsBOF = List(BOF1, BOF2)
  val oneRoomBOF = List(BOF1)

  val igniteRoom = List(BOF1)

  val allRooms = List(ROOM8, ROOM5, ROOM9, ROOM6, ROOM7, ROOM4, ROOM3, ROOM10, BOF1, BOF2, HALL_EXPO)
}