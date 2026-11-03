package dev.polyrics.mondafone.spam

class PhoneNumberNormalizer(private val regionProvider: RegionProvider) {

    fun normalize(rawNumber: String): String? = normalize(rawNumber, regionProvider.regionIso())

    fun normalize(rawNumber: String, regionIso: String): String? {
        val digits = rawNumber.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        val fullDigits = withCallingCode(rawNumber, digits, regionIso) ?: return null
        if (fullDigits.length !in MIN_DIGITS..MAX_DIGITS) return null
        val callingCode = CountryCallingCodes.longestPrefixOf(fullDigits) ?: return null
        val nationalNumber = fullDigits.removePrefix(callingCode)
        if (nationalNumber.isEmpty()) return null
        return callingCode.padStart(CODE_DIGITS, '0') + nationalNumber
    }

    private fun withCallingCode(rawNumber: String, digits: String, regionIso: String): String? {
        val international = rawNumber.trimStart().startsWith('+') || digits.startsWith(INTERNATIONAL_PREFIX)
        if (international) return digits.removePrefix(INTERNATIONAL_PREFIX)
        val callingCode = CountryCallingCodes.callingCodeOf(regionIso) ?: return null
        return "$callingCode$digits"
    }

    private companion object {
        const val CODE_DIGITS = 4
        const val MIN_DIGITS = 2
        const val MAX_DIGITS = 15
        const val INTERNATIONAL_PREFIX = "00"
    }
}
