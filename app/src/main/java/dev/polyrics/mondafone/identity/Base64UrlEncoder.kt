package dev.polyrics.mondafone.identity

import java.util.Base64

class Base64UrlEncoder {

    fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun decode(text: String): ByteArray =
        Base64.getUrlDecoder().decode(text)
}
