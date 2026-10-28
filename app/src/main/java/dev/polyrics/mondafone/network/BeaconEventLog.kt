package dev.polyrics.mondafone.network

interface BeaconEventLog {

    fun log(event: String)

    fun lines(): List<String>

    fun setListener(listener: ((List<String>) -> Unit)?)
}
