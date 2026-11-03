package dev.polyrics.mondafone.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSpamProbabilityTest {

    @Test
    fun noForeignReportsMeansNoProbability() {
        assertEquals(0.0, GlobalSpamProbability().probabilityOfSpam(0), DELTA)
    }

    @Test
    fun probabilityHalvesTheChanceOfBeingLegitimatePerReport() {
        val probability = GlobalSpamProbability()
        assertEquals(0.5, probability.probabilityOfSpam(1), DELTA)
        assertEquals(0.75, probability.probabilityOfSpam(2), DELTA)
        assertEquals(0.875, probability.probabilityOfSpam(3), DELTA)
    }

    @Test
    fun negativeReportsBehaveLikeNoReports() {
        assertEquals(0.0, GlobalSpamProbability().probabilityOfSpam(-3), DELTA)
    }

    @Test
    fun neverFlagsAsSpamWithoutForeignReports() {
        assertFalse(GlobalSpamProbability(FixedRandom(0.0)).isSpamByChance(0))
    }

    @Test
    fun flagsAsSpamWhenDrawIsBelowProbability() {
        assertTrue(GlobalSpamProbability(FixedRandom(0.49)).isSpamByChance(1))
    }

    @Test
    fun doesNotFlagAsSpamWhenDrawReachesProbability() {
        assertFalse(GlobalSpamProbability(FixedRandom(0.5)).isSpamByChance(1))
        assertFalse(GlobalSpamProbability(FixedRandom(0.75)).isSpamByChance(2))
    }

    private companion object {
        const val DELTA = 0.000001
    }
}
