package dev.remotectrl.proto

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/** Minimal protobuf writer: just the wire types the TV protocols use. */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun int(field: Int, value: Int): ProtoWriter {
        tag(field, WIRE_VARINT)
        rawVarint(value.toLong())
        return this
    }

    fun bool(field: Int, value: Boolean): ProtoWriter = int(field, if (value) 1 else 0)

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun bytes(field: Int, value: ByteArray): ProtoWriter {
        tag(field, WIRE_LEN)
        rawVarint(value.size.toLong())
        out.write(value)
        return this
    }

    fun message(field: Int, build: ProtoWriter.() -> Unit): ProtoWriter =
        bytes(field, ProtoWriter().apply(build).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun tag(field: Int, wire: Int) = rawVarint(((field shl 3) or wire).toLong())

    private fun rawVarint(value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }

    companion object {
        const val WIRE_VARINT = 0
        const val WIRE_LEN = 2
    }
}

/** One decoded field. [varint] is set for wire type 0, [bytes] for length-delimited fields. */
class ProtoField(val number: Int, val varint: Long, val bytes: ByteArray?)

object ProtoReader {
    fun parse(data: ByteArray): List<ProtoField> {
        val fields = mutableListOf<ProtoField>()
        var pos = 0

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                require(pos < data.size) { "truncated varint" }
                val b = data[pos++].toInt()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
                require(shift < 70) { "varint too long" }
            }
        }

        while (pos < data.size) {
            val tag = varint()
            val number = (tag ushr 3).toInt()
            when (val wire = (tag and 7).toInt()) {
                0 -> fields += ProtoField(number, varint(), null)
                1 -> pos += 8
                2 -> {
                    val len = varint().toInt()
                    require(len >= 0 && pos + len <= data.size) { "bad length" }
                    fields += ProtoField(number, 0, data.copyOfRange(pos, pos + len))
                    pos += len
                }
                5 -> pos += 4
                else -> throw IllegalArgumentException("unsupported wire type $wire")
            }
        }
        return fields
    }
}

fun List<ProtoField>.varint(number: Int): Long? = firstOrNull { it.number == number && it.bytes == null }?.varint

fun List<ProtoField>.bytes(number: Int): ByteArray? = firstOrNull { it.number == number }?.bytes

fun List<ProtoField>.string(number: Int): String? = bytes(number)?.toString(Charsets.UTF_8)

fun List<ProtoField>.has(number: Int): Boolean = any { it.number == number }

/** Messages on the TLS sockets are prefixed by their length as a varint. */
object Frames {
    private const val MAX_FRAME = 1 shl 20

    fun write(out: OutputStream, payload: ByteArray) {
        val header = ByteArrayOutputStream()
        var v = payload.size
        while (v and 0x7F.inv() != 0) {
            header.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        header.write(v)
        out.write(header.toByteArray() + payload)
        out.flush()
    }

    fun read(input: InputStream): ByteArray {
        var length = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b < 0) throw EOFException("connection closed")
            length = length or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
            if (shift > 28) throw java.io.IOException("bad frame header")
        }
        if (length < 0 || length > MAX_FRAME) throw java.io.IOException("frame too large: $length")
        val buf = ByteArray(length)
        var off = 0
        while (off < length) {
            val n = input.read(buf, off, length - off)
            if (n < 0) throw EOFException("connection closed")
            off += n
        }
        return buf
    }
}
