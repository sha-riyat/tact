package dev.remotectrl.atv

import dev.remotectrl.proto.Frames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.net.ssl.SSLSocket

sealed interface RemoteState {
    data object Idle : RemoteState
    data object Connecting : RemoteState
    data object Ready : RemoteState

    /** [needsPairing] is true when the TV closed the connection before the handshake finished. */
    data class Failed(val message: String, val needsPairing: Boolean) : RemoteState
}

/** Live connection to one TV on port 6466: handshake, keep-alive replies and key injection. */
class RemoteClient(private val host: String, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<RemoteState>(RemoteState.Idle)
    val state: StateFlow<RemoteState> = _state

    private val writeLock = Any()
    private var socket: SSLSocket? = null
    private var job: Job? = null
    private var sender: Job? = null
    private val outbox = Channel<ByteArray>(Channel.UNLIMITED)

    @Volatile
    private var imeCounter = 0

    @Volatile
    private var fieldCounter = 0

    fun connect() {
        if (_state.value is RemoteState.Connecting || _state.value is RemoteState.Ready) return
        _state.value = RemoteState.Connecting
        job = scope.launch(Dispatchers.IO) { run() }
        sender = scope.launch(Dispatchers.IO) { drainOutbox() }
    }

    fun close() {
        job?.cancel()
        sender?.cancel()
        runCatching { socket?.close() }
        socket = null
        _state.value = RemoteState.Idle
    }

    fun key(code: Int) = send(RemoteMessages.key(code))

    fun launchApp(url: String) = send(RemoteMessages.appLink(url))

    /** Inserts [text] at the cursor of the field the TV has focused, using the counters it last sent (0 until then). */
    fun type(text: String) {
        if (text.isEmpty()) return
        send(RemoteMessages.text(imeCounter, fieldCounter, text))
    }

    /** Frames leave in the order they were queued, which matters when typing letter by letter. */
    private fun send(frame: ByteArray) {
        outbox.trySend(frame)
    }

    private suspend fun drainOutbox() {
        for (frame in outbox) {
            val s = socket ?: continue
            try {
                write(s, frame)
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun run() {
        var ready = false
        try {
            val s = TlsIdentity.connect(host, PORT)
            socket = s
            val input = s.inputStream
            while (true) {
                when (val msg = RemoteMessages.parse(Frames.read(input))) {
                    RemoteMessages.Incoming.Configure -> write(s, RemoteMessages.configure())
                    RemoteMessages.Incoming.SetActive -> {
                        write(s, RemoteMessages.setActive())
                        ready = true
                        _state.value = RemoteState.Ready
                    }
                    is RemoteMessages.Incoming.Ping -> write(s, RemoteMessages.pingResponse(msg.value))
                    is RemoteMessages.Incoming.TextField -> {
                        msg.imeCounter?.let { imeCounter = it }
                        msg.fieldCounter?.let { fieldCounter = it }
                    }
                    is RemoteMessages.Incoming.Error -> throw PairingException("Erreur du téléviseur")
                    RemoteMessages.Incoming.Other -> Unit
                }
            }
        } catch (e: Exception) {
            if (job?.isActive == true) _state.value = failure(e, needsPairing = !ready)
        }
    }

    private fun write(s: SSLSocket, frame: ByteArray) {
        synchronized(writeLock) { Frames.write(s.outputStream, frame) }
    }

    private fun fail(e: Exception) {
        runCatching { socket?.close() }
        socket = null
        _state.value = failure(e, needsPairing = false)
    }

    private fun failure(e: Exception, needsPairing: Boolean) =
        RemoteState.Failed(e.message ?: "Connexion perdue", needsPairing)

    private companion object {
        const val PORT = 6466
    }
}
