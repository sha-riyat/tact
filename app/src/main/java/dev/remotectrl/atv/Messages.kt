package dev.remotectrl.atv

import dev.remotectrl.proto.ProtoField
import dev.remotectrl.proto.ProtoReader
import dev.remotectrl.proto.ProtoWriter
import dev.remotectrl.proto.has
import dev.remotectrl.proto.varint

/** Pairing protocol (TCP 6467): every message is wrapped in a version/status envelope. */
object PairingMessages {
    private const val PROTOCOL_VERSION = 2
    const val STATUS_OK = 200L
    private const val ENCODING_HEX = 3
    private const val SYMBOL_LENGTH = 6
    private const val ROLE_INPUT = 1

    private fun envelope(build: ProtoWriter.() -> Unit): ByteArray =
        ProtoWriter().int(1, PROTOCOL_VERSION).int(2, STATUS_OK.toInt()).apply(build).toByteArray()

    fun request(serviceName: String, clientName: String): ByteArray = envelope {
        message(10) {
            string(1, serviceName)
            string(2, clientName)
        }
    }

    fun option(): ByteArray = envelope {
        message(20) {
            message(1) {
                int(1, ENCODING_HEX)
                int(2, SYMBOL_LENGTH)
            }
            int(3, ROLE_INPUT)
        }
    }

    fun configuration(): ByteArray = envelope {
        message(30) {
            message(1) {
                int(1, ENCODING_HEX)
                int(2, SYMBOL_LENGTH)
            }
            int(2, ROLE_INPUT)
        }
    }

    fun secret(hash: ByteArray): ByteArray = envelope {
        message(40) { bytes(1, hash) }
    }

    /** Returns the status of a reply from the TV, or null when the frame has none. */
    fun status(frame: ByteArray): Long? = ProtoReader.parse(frame).varint(2)

    fun hasSecretAck(frame: ByteArray): Boolean = ProtoReader.parse(frame).has(41)
}

/** Remote control protocol (TCP 6466). */
object RemoteMessages {
    /** Feature bitmask sent in the handshake, as expected by Android TV. */
    private const val FEATURES = 622
    private const val DIRECTION_SHORT = 3

    sealed interface Incoming {
        data object Configure : Incoming
        data object SetActive : Incoming
        data class Ping(val value: Int) : Incoming
        data class Error(val code: Int) : Incoming
        data object Other : Incoming
    }

    fun configure(): ByteArray = ProtoWriter().message(1) {
        int(1, FEATURES)
        message(2) {
            string(1, "Remote CTRL")
            string(2, "RemoteCtrl")
            int(3, 1)
            string(4, "1")
            string(5, "dev.remotectrl")
            string(6, "1.0.0")
        }
    }.toByteArray()

    fun setActive(): ByteArray = ProtoWriter().message(2) { int(1, FEATURES) }.toByteArray()

    fun pingResponse(value: Int): ByteArray = ProtoWriter().message(9) { int(1, value) }.toByteArray()

    fun key(code: Int): ByteArray = ProtoWriter().message(10) {
        int(1, code)
        int(2, DIRECTION_SHORT)
    }.toByteArray()

    fun appLink(url: String): ByteArray = ProtoWriter().message(90) { string(1, url) }.toByteArray()

    fun parse(frame: ByteArray): Incoming {
        val fields: List<ProtoField> = ProtoReader.parse(frame)
        return when {
            fields.has(1) -> Incoming.Configure
            fields.has(2) -> Incoming.SetActive
            fields.has(8) -> {
                val inner = ProtoReader.parse(fields.first { it.number == 8 }.bytes ?: ByteArray(0))
                Incoming.Ping((inner.varint(1) ?: 0L).toInt())
            }
            fields.has(3) -> Incoming.Error(0)
            else -> Incoming.Other
        }
    }
}
