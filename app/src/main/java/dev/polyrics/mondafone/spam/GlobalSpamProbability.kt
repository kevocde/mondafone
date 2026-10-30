package dev.polyrics.mondafone.spam

import kotlin.math.pow
import kotlin.random.Random

class GlobalSpamProbability(private val random: Random = Random.Default) {

    fun probabilityOfSpam(foreignReports: Int): Double =
        if (foreignReports <= 0) 0.0 else 1.0 - HALF.pow(foreignReports)

    fun isSpamByChance(foreignReports: Int): Boolean =
        foreignReports > 0 && random.nextDouble() < probabilityOfSpam(foreignReports)

    private companion object {
        const val HALF = 0.5
    }
}
