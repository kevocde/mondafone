package dev.polyrics.mondafone.spam

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberDisplayTest {

    @Test
    fun formatsTwoDigitCallingCode() {
        assertEquals("+57 3001234567", PhoneNumberDisplay.format("00573001234567"))
    }

    @Test
    fun formatsSingleDigitCallingCode() {
        assertEquals("+1 2125551234", PhoneNumberDisplay.format("00012125551234"))
    }

    @Test
    fun formatsThreeDigitCallingCode() {
        assertEquals("+355 691234567", PhoneNumberDisplay.format("0355691234567"))
    }

    @Test
    fun returnsInputWhenNotCanonical() {
        assertEquals("", PhoneNumberDisplay.format(""))
        assertEquals("123", PhoneNumberDisplay.format("123"))
        assertEquals("+57abc", PhoneNumberDisplay.format("+57abc"))
    }
}
