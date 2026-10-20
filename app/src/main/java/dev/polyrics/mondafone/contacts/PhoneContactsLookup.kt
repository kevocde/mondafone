package dev.polyrics.mondafone.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract

class PhoneContactsLookup(private val resolver: ContentResolver) : ContactsLookup {

    override fun isContact(number: String): Boolean {
        val filterUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        return resolver.query(filterUri, projection, null, null, null)?.use { it.moveToFirst() }
            ?: false
    }
}
