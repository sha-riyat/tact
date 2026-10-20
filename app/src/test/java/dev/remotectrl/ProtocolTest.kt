package dev.remotectrl

import dev.remotectrl.atv.KeyCodes
import dev.remotectrl.atv.PairingMessages
import dev.remotectrl.atv.PairingSecret
import dev.remotectrl.atv.RemoteMessages
import dev.remotectrl.proto.Frames
import dev.remotectrl.proto.ProtoReader
import dev.remotectrl.proto.ProtoWriter
import dev.remotectrl.proto.bytes
import dev.remotectrl.proto.string
import dev.remotectrl.proto.varint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

class ProtocolTest {
    @Test
    fun protoRoundTrip() {
        val data = ProtoWriter().int(1, 300).string(2, "héllo").message(3) { int(1, 7) }.toByteArray()
        val fields = ProtoReader.parse(data)
        assertEquals(300L, fields.varint(1))
        assertEquals("héllo", fields.string(2))
        assertEquals(7L, ProtoReader.parse(fields.bytes(3)!!).varint(1))
    }

    @Test
    fun varintEncodesKnownBytes() {
        // Field 1, value 150 is the classic protobuf example: 08 96 01.
        assertArrayEquals(byteArrayOf(0x08, 0x96.toByte(), 0x01), ProtoWriter().int(1, 150).toByteArray())
    }

    @Test
    fun framesRoundTrip() {
        val payload = ByteArray(300) { it.toByte() }
        val out = ByteArrayOutputStream().also { Frames.write(it, payload) }
        assertArrayEquals(payload, Frames.read(ByteArrayInputStream(out.toByteArray())))
    }

    @Test
    fun pairingRequestHasEnvelope() {
        val fields = ProtoReader.parse(PairingMessages.request("atvremote", "x"))
        assertEquals(2L, fields.varint(1))
        assertEquals(PairingMessages.STATUS_OK, fields.varint(2))
        assertEquals("atvremote", ProtoReader.parse(fields.bytes(10)!!).string(1))
    }

    @Test
    fun keyMessageCarriesCodeAndDirection() {
        val inner = ProtoReader.parse(ProtoReader.parse(RemoteMessages.key(KeyCodes.HOME)).bytes(10)!!)
        assertEquals(3L, inner.varint(1))
        assertEquals(3L, inner.varint(2))
    }

    @Test
    fun pingIsParsed() {
        val frame = ProtoWriter().message(8) { int(1, 42) }.toByteArray()
        assertEquals(RemoteMessages.Incoming.Ping(42), RemoteMessages.parse(frame))
    }

    @Test
    fun charsMapToKeyCodes() {
        assertEquals(29, KeyCodes.forChar('a'))
        assertEquals(29, KeyCodes.forChar('A'))
        assertEquals(7, KeyCodes.forChar('0'))
        assertEquals(62, KeyCodes.forChar(' '))
        assertNull(KeyCodes.forChar('é'))
    }

    @Test
    fun selfSignedCertIsValidAndSelfVerifying() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val cert = dev.remotectrl.atv.SelfSignedCert.create(pair, "remotectrl")
        cert.verify(pair.public)
        cert.checkValidity()
        assertEquals("CN=remotectrl", cert.subjectX500Principal.name)
        assertEquals(pair.public, cert.publicKey)
    }

    @Test
    fun pairingSecretAcceptsMatchingCodeOnly() {
        val gen = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val client = gen.generateKeyPair().public as RSAPublicKey
        val server = gen.generateKeyPair().public as RSAPublicKey

        fun unsigned(n: java.math.BigInteger) = n.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        val tail = byteArrayOf(0x12, 0x34)
        val hash = MessageDigest.getInstance("SHA-256").apply {
            update(unsigned(client.modulus)); update(unsigned(client.publicExponent))
            update(unsigned(server.modulus)); update(unsigned(server.publicExponent))
            update(tail)
        }.digest()
        val code = "%02X1234".format(hash[0].toInt() and 0xFF)

        assertArrayEquals(hash, PairingSecret.compute(client, server, code))
        val wrong = "%02X1234".format((hash[0].toInt() + 1) and 0xFF)
        assertNull(PairingSecret.compute(client, server, wrong))
        assertNull(PairingSecret.compute(client, server, "ZZ"))
        assertNotNull(hash)
    }
}
