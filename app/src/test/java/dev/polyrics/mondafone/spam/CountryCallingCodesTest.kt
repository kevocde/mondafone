package dev.polyrics.mondafone.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryCallingCodesTest {

    @Test
    fun returnsCallingCodeForKnownRegion() {
        assertEquals(57, CountryCallingCodes.callingCodeOf("CO"))
        assertEquals(1, CountryCallingCodes.callingCodeOf("US"))
        assertEquals(34, CountryCallingCodes.callingCodeOf("ES"))
    }

    @Test
    fun acceptsLowercaseRegion() {
        assertEquals(57, CountryCallingCodes.callingCodeOf("co"))
    }

    @Test
    fun returnsNullForUnknownRegion() {
        assertNull(CountryCallingCodes.callingCodeOf("XX"))
    }

    @Test
    fun matchesLongestValidCallingCodePrefix() {
        assertEquals("57", CountryCallingCodes.longestPrefixOf("573001234567"))
        assertEquals("1", CountryCallingCodes.longestPrefixOf("12125551234"))
        assertEquals("355", CountryCallingCodes.longestPrefixOf("355691234567"))
    }

    @Test
    fun returnsNullWhenNoCallingCodeMatches() {
        assertNull(CountryCallingCodes.longestPrefixOf("0"))
        assertNull(CountryCallingCodes.longestPrefixOf("999999"))
    }
}
