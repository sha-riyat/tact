package dev.remotectrl.atv

import android.content.Context
import java.io.File
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * The phone's client certificate: a software RSA key kept in a PKCS12 file private to the app.
 * (Android Keystore keys fail in Conscrypt's TLS handshake with some TVs.)
 */
object TlsIdentity {
    private const val ALIAS = "remotectrl-client"
    private const val CONNECT_TIMEOUT_MS = 5000

    // The file sits in the app's private storage, so this password is only a format requirement.
    private val password = "remotectrl".toCharArray()

    private lateinit var file: File
    private var cached: KeyStore? = null

    fun init(context: Context) {
        file = File(context.filesDir, "identity.p12")
    }

    // The TV presents a self-signed certificate: trust is established by the pairing code, not a CA.
    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    @Synchronized
    private fun keyStore(): KeyStore {
        cached?.let { return it }
        val ks = KeyStore.getInstance("PKCS12")
        if (file.exists()) {
            file.inputStream().use { ks.load(it, password) }
        } else {
            ks.load(null, null)
            val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val cert = SelfSignedCert.create(pair, "remotectrl")
            ks.setKeyEntry(ALIAS, pair.private, password, arrayOf(cert))
            file.outputStream().use { ks.store(it, password) }
        }
        cached = ks
        return ks
    }

    fun clientPublicKey(): RSAPublicKey = keyStore().getCertificate(ALIAS).publicKey as RSAPublicKey

    /** Opens a TLS socket that presents our client certificate to [host]:[port]. */
    fun connect(host: String, port: Int): SSLSocket {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keyStore(), password) }
        val context = SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, arrayOf(trustAll), null) }
        val socket = context.socketFactory.createSocket() as SSLSocket
        try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket.keepAlive = true
            socket.startHandshake()
        } catch (e: Exception) {
            socket.close()
            throw e
        }
        return socket
    }

    fun serverPublicKey(socket: SSLSocket): RSAPublicKey =
        socket.session.peerCertificates[0].publicKey as RSAPublicKey
}
