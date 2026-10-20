package dev.polyrics.mondafone.contacts

interface ContactsLookup {
    fun isContact(number: String): Boolean
}
