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
  // val HALL_EXPO = Room("expo", "Exhibition floor", 3000, "exhibition")

  val ROOM1 = Room("room1", "Grand Ballroom 220A", 1200, "theatre")
  val ROOM2 = Room("room2", "Grand Ballroom 220B", 288, "2/3 classroom")
  val ROOM3 = Room("room3", "Grand Ballroom 220 C", 288, "2/3 classroom")
  val ROOM4 = Room("room4", "LL 20 A", 160, "2/3 classroom")
  val ROOM5 = Room("room5", "LL 20 B", 84, "2/3 classroom")
  val ROOM6 = Room("room6", "LL 20 C", 84, "2/3 classroom")
  val ROOM7 = Room("room7", "LL 20 D", 160, "2/3 classroom")
  val ROOM8 = Room("room8", "LL 21 A", 87, "2/3 classroom")
  val ROOM9 = Room("room9", "LL 21 B", 108, "2/3 classroom")
  val ROOM10 = Room("room10", "LL 21 C", 108, "2/3 classroom")

  val allRoomsTIA = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM7, ROOM9, ROOM10)
  val allRoomsTIAThree = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM7, ROOM9)

  val keynoteRoom = List(ROOM1)
  val eveningKeynoteRoom = List(ROOM1)

  val morningConfRooms = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM5, ROOM6, ROOM7, ROOM8, ROOM9, ROOM10)

  val afternoonConfRoomsDayOneTwo = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM7, ROOM9, ROOM10)
  val afternoonConfRoomsDayThree = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM7, ROOM9)

  val allRoomsQuick = List(ROOM5, ROOM6, ROOM7, ROOM8, ROOM9, ROOM10)

  val LabsDayOneTwo = List(ROOM5, ROOM6, ROOM8)
  val LabsDayThree = List(ROOM5, ROOM6, ROOM8, ROOM10)

  val allRoomsBOF = List(ROOM5, ROOM6, ROOM7, ROOM8, ROOM9, ROOM10)

  val igniteRoom = List(ROOM4)

  val allRooms = List(ROOM1, ROOM2, ROOM3, ROOM4, ROOM5, ROOM6, ROOM7, ROOM8, ROOM9, ROOM10)
}