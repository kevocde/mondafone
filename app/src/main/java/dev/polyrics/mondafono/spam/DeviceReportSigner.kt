package dev.polyrics.mondafono.spam

import dev.polyrics.mondafono.identity.Base64UrlEncoder
import dev.polyrics.mondafono.identity.DeviceIdentity

class DeviceReportSigner(
    private val identity: DeviceIdentity,
    private val encoder: Base64UrlEncoder = Base64UrlEncoder(),
) : ReportSigner {

    override fun sign(phoneHash: String, createdAt: Long, seqNum: Long): String? =
        identity.sign(ReportSignature.payload(phoneHash, createdAt, seqNum))?.let(encoder::encode)
}
