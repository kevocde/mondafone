package dev.polyrics.mondafone.network

class BeaconCycleCounter {

    private val ids = mutableSetOf<String>()

    @Synchronized
    fun record(id: String): Boolean = ids.add(id)

    @Synchronized
    fun count(): Int = ids.size
}
