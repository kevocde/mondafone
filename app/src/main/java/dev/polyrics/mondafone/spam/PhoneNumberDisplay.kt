package dev.polyrics.mondafone.spam

object PhoneNumberDisplay {

    fun format(canonicalNumber: String): String {
        if (!canonicalNumber.isCanonical()) return canonicalNumber
        val callingCode = canonicalNumber.take(CODE_DIGITS).toInt()
        return "+$callingCode ${canonicalNumber.drop(CODE_DIGITS)}"
    }

    private fun String.isCanonical(): Boolean =
        length > CODE_DIGITS && all(Char::isDigit)

    private const val CODE_DIGITS = 4
}
