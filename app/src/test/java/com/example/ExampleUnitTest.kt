package com.example

import com.example.data.model.SwarUserIdentity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for core SwarRoom utilities.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun numericIdForUid_isAlwaysTenDigitsAndUniquePerUser() {
    val ids = (1..200).map { i -> SwarUserIdentity.numericIdForUid("uid_user_$i") }
    assertTrue(ids.all { it.length == 10 && it.all(Char::isDigit) && it.first() != '0' })
    assertEquals(ids.size, ids.toSet().size)
    assertEquals(
      SwarUserIdentity.numericIdForUid("uid_alpha"),
      SwarUserIdentity.numericIdForUid("uid_alpha"),
    )
  }

  @Test
  fun levelForCoins_calculatesLevelAndProgress() {
    val lvl1 = SwarUserIdentity.levelForCoins(500L)
    assertEquals(1, lvl1.level)
    assertEquals(0.5f, lvl1.progress, 0.01f)
    assertEquals(500L, lvl1.nextLevelRemainingCoins)

    val lvl15 = SwarUserIdentity.levelForCoins(14_250L)
    assertEquals(15, lvl15.level)
    assertEquals("गोल्डन वॉइस", lvl15.title)
  }
}
