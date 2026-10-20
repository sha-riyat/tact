package dev.remotectrl.atv

import java.math.BigInteger
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

object PairingSecret {
    /**
     * Hash the TV expects: SHA-256 over both public keys and the last bytes of the on-screen code.
     * Returns null when the code is malformed or its first byte does not match the hash (typo).
     */
    fun compute(client: RSAPublicKey, server: RSAPublicKey, code: String): ByteArray? {
        val codeBytes = hexToBytes(code.trim()) ?: return null
        if (codeBytes.size < 2) return null
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(unsigned(client.modulus))
            update(unsigned(client.publicExponent))
            update(unsigned(server.modulus))
            update(unsigned(server.publicExponent))
            update(codeBytes, 1, codeBytes.size - 1)
        }.digest()
        return if (digest[0] == codeBytes[0]) digest else null
    }

    private fun unsigned(n: BigInteger): ByteArray {
        val b = n.toByteArray()
        return if (b.size > 1 && b[0] == 0.toByte()) b.copyOfRange(1, b.size) else b
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        return runCatching {
            ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }.getOrNull()
    }
}
