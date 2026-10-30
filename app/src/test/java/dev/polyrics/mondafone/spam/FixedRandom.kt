package dev.polyrics.mondafone.spam

import kotlin.random.Random

class FixedRandom(private val value: Double) : Random() {
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextDouble(): Double = value
}
