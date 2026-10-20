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

        /** The TV has a text field in focus and tells us the counters its next edit must carry (null: not given). */
        data class TextField(val imeCounter: Int?, val fieldCounter: Int?) : Incoming
        data object Other : Incoming
    }

    fun configure(): ByteArray = ProtoWriter().message(1) {
        int(1, FEATURES)
        message(2) {
            string(1, "Tact")
            string(2, "Tact")
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

    /** Sets the content of the focused text field; the counters are the ones the TV last sent. */
    fun text(imeCounter: Int, fieldCounter: Int, text: String): ByteArray = ProtoWriter().message(21) {
        int(1, imeCounter)
        int(2, fieldCounter)
        message(3) {
            int(1, 1)
            message(2) {
                int(1, text.length - 1)
                int(2, text.length - 1)
                string(3, text)
            }
        }
    }.toByteArray()

    private fun fieldCounterOf(message: ProtoField): Int? {
        val inner = ProtoReader.parse(message.bytes ?: return null)
        val status = inner.firstOrNull { it.number == 2 }?.bytes ?: return null
        return ProtoReader.parse(status).varint(1)?.toInt()
    }

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
            fields.has(21) -> {
                val inner = ProtoReader.parse(fields.first { it.number == 21 }.bytes ?: ByteArray(0))
                Incoming.TextField(inner.varint(1)?.toInt(), inner.varint(2)?.toInt())
            }
            // Key inject (20) and show request (22) carry the field counter inside their text field status (2).
            fields.has(20) -> Incoming.TextField(null, fieldCounterOf(fields.first { it.number == 20 }))
            fields.has(22) -> Incoming.TextField(null, fieldCounterOf(fields.first { it.number == 22 }))
            else -> Incoming.Other
        }
    }
}
