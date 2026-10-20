package dev.remotectrl.atv

import dev.remotectrl.proto.Frames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.SSLSocket

class PairingException(message: String) : Exception(message)

/** Pairs this phone with a TV: [start] makes the TV show a 6-character code, [finish] checks it. */
class PairingClient(private val host: String) {
    private var socket: SSLSocket? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        val s = TlsIdentity.connect(host, PORT)
        socket = s
        try {
            exchange(s, PairingMessages.request("atvremote", "Remote CTRL"))
            exchange(s, PairingMessages.option())
            exchange(s, PairingMessages.configuration())
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    suspend fun finish(code: String) = withContext(Dispatchers.IO) {
        val s = socket ?: throw PairingException("Appairage non démarré")
        val secret = PairingSecret.compute(TlsIdentity.clientPublicKey(), TlsIdentity.serverPublicKey(s), code)
            ?: throw PairingException("Code incorrect")
        try {
            val reply = exchange(s, PairingMessages.secret(secret))
            if (!PairingMessages.hasSecretAck(reply)) throw PairingException("Le téléviseur a refusé le code")
        } finally {
            close()
        }
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun exchange(s: SSLSocket, message: ByteArray): ByteArray {
        Frames.write(s.outputStream, message)
        val reply = Frames.read(s.inputStream)
        if (PairingMessages.status(reply) != PairingMessages.STATUS_OK) {
            throw PairingException("Le téléviseur a refusé l'appairage")
        }
        return reply
    }

    private companion object {
        const val PORT = 6467
    }
}
