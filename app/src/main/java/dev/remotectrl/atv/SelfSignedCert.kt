package dev.remotectrl.atv

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Builds a minimal self-signed X.509 v3 certificate (SHA256withRSA) without extra libraries. */
object SelfSignedCert {
    private const val DAY_MS = 24L * 3600 * 1000

    // 1.2.840.113549.1.1.11 (sha256WithRSAEncryption) and 2.5.4.3 (commonName).
    private val sha256WithRsa = byteArrayOf(
        0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B,
    )
    private val commonName = byteArrayOf(0x55, 0x04, 0x03)

    fun create(keyPair: KeyPair, cn: String, now: Date = Date()): X509Certificate {
        val algorithm = der(0x30, der(0x06, sha256WithRsa), der(0x05, ByteArray(0)))
        val name = der(0x30, der(0x31, der(0x30, der(0x06, commonName), der(0x0C, cn.toByteArray()))))
        val validity = der(
            0x30,
            der(0x17, utc(Date(now.time - DAY_MS))),
            der(0x17, utc(Date(now.time + 3650 * DAY_MS))),
        )
        val serial = ByteArray(8).also { SecureRandom().nextBytes(it) }.also { it[0] = (it[0].toInt() and 0x7F or 0x01).toByte() }

        val tbs = der(
            0x30,
            der(0xA0, der(0x02, byteArrayOf(2))),
            der(0x02, serial),
            algorithm,
            name,
            validity,
            name,
            keyPair.public.encoded,
        )
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(tbs)
            sign()
        }
        val certificate = der(0x30, tbs, algorithm, der(0x03, byteArrayOf(0) + signature))
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certificate)) as X509Certificate
    }

    private fun utc(date: Date): ByteArray =
        SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(date).toByteArray()

    private fun der(tag: Int, vararg parts: ByteArray): ByteArray {
        val body = ByteArrayOutputStream().also { out -> parts.forEach { out.write(it) } }.toByteArray()
        val length = ByteArrayOutputStream()
        when {
            body.size < 0x80 -> length.write(body.size)
            body.size < 0x100 -> { length.write(0x81); length.write(body.size) }
            else -> { length.write(0x82); length.write(body.size shr 8); length.write(body.size and 0xFF) }
        }
        return byteArrayOf(tag.toByte()) + length.toByteArray() + body
    }
}
