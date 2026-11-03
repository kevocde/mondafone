package dev.polyrics.mondafone.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {

    private class FixedRegion(private val iso: String) : RegionProvider {
        override fun regionIso(): String = iso
    }

    @Test
    fun keepsInternationalNumberWithPadding() {
        assertEquals("00573001234567", normalizer().normalize("+57 300 123 4567"))
    }

    @Test
    fun addsDefaultRegionToNationalNumber() {
        assertEquals("00573001234567", normalizer().normalize("3001234567"))
    }

    @Test
    fun acceptsPaddedInternationalPrefix() {
        assertEquals("00573001234567", normalizer().normalize("00573001234567"))
    }

    @Test
    fun padsSingleDigitCallingCode() {
        assertEquals("00012125551234", normalizer().normalize("+1 (212) 555-1234"))
    }

    @Test
    fun padsThreeDigitCallingCode() {
        assertEquals("0355691234567", normalizer().normalize("+355 69 123 4567"))
    }

    @Test
    fun internationalNumberIgnoresDefaultRegion() {
        assertEquals(
            normalizer("CO").normalize("+34 987654321"),
            normalizer("US").normalize("+34 987654321"),
        )
    }

    @Test
    fun usesRegionProviderForNationalNumber() {
        assertEquals("00573001234567", PhoneNumberNormalizer(FixedRegion("CO")).normalize("3001234567"))
    }

    @Test
    fun returnsNullForUnknownRegionOnNationalNumber() {
        assertNull(normalizer("XX").normalize("3001234567"))
    }

    @Test
    fun returnsNullForBlankInput() {
        assertNull(normalizer().normalize(""))
        assertNull(normalizer().normalize("   "))
        assertNull(normalizer().normalize("+"))
    }

    @Test
    fun returnsNullForTooLongNumber() {
        assertNull(normalizer().normalize("+1234567890123456"))
    }

    @Test
    fun stripsNonDigitCharacters() {
        assertEquals("00573001234567", normalizer().normalize("+57-300.123.4567"))
    }

    private fun normalizer(region: String = "CO"): PhoneNumberNormalizer =
        PhoneNumberNormalizer(FixedRegion(region))
}
