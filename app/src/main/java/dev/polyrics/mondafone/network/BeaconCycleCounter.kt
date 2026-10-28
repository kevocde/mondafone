package dev.polyrics.mondafone.network

class BeaconCycleCounter {

    private var received = 0

    @Synchronized
    fun record() {
        received++
    }

    @Synchronized
    fun reset() {
        received = 0
    }

    @Synchronized
    fun count(): Int = received
}
