package dev.remotectrl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.remotectrl.atv.Discovery
import dev.remotectrl.atv.PairingClient
import dev.remotectrl.atv.PairingException
import dev.remotectrl.atv.RemoteClient
import dev.remotectrl.atv.RemoteState
import dev.remotectrl.atv.TlsIdentity
import dev.remotectrl.data.Tv
import dev.remotectrl.data.TvStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Devices : Screen
    data class Pairing(val tv: Tv) : Screen
    /** The home screen; [tv] is null until a TV has been added. */
    data class Remote(val tv: Tv?) : Screen
}

sealed interface PairingUi {
    data object Starting : PairingUi
    data object WaitingCode : PairingUi
    data object Verifying : PairingUi
    data class Error(val message: String, val retryable: Boolean) : PairingUi
}

data class UiState(
    val screen: Screen = Screen.Remote(null),
    val saved: List<Tv> = emptyList(),
    val pairing: PairingUi = PairingUi.Starting,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    init {
        TlsIdentity.init(app)
    }

    private val store = TvStore(app)
    private val discovery = Discovery(app)

    private var pairing: PairingClient? = null
    private var remote: RemoteClient? = null
    private var remoteJob: Job? = null

    /** The TV the remote is (or was last) pointed at; survives a visit to the TV list or to pairing. */
    private var current: Tv? = null

    private val _ui = MutableStateFlow(UiState(saved = store.load()))
    val ui: StateFlow<UiState> = _ui

    private val _remoteState = MutableStateFlow<RemoteState>(RemoteState.Idle)
    val remoteState: StateFlow<RemoteState> = _remoteState

    val found = discovery.found

    init {
        // Open straight on the remote, pointed at the TV used last.
        store.load().lastOrNull()?.let(::openRemote)
    }

    fun showDevices() {
        _ui.update { it.copy(screen = Screen.Devices) }
    }

    fun startDiscovery() = discovery.start()

    fun stopDiscovery() = discovery.stop()

    fun openTv(tv: Tv) {
        if (store.load().any { it.host == tv.host }) openRemote(tv) else beginPairing(tv)
    }

    fun addManual(host: String) = openTv(Tv(host.trim(), host.trim()))

    fun forget(tv: Tv) {
        store.remove(tv)
        if (current?.host == tv.host) {
            closeRemote()
            current = null
        }
        _ui.update { it.copy(saved = store.load()) }
    }

    fun beginPairing(tv: Tv) {
        closeRemote()
        pairing?.close()
        val client = PairingClient(tv.host)
        pairing = client
        _ui.update { it.copy(screen = Screen.Pairing(tv), pairing = PairingUi.Starting) }
        viewModelScope.launch {
            try {
                client.start()
                _ui.update { it.copy(pairing = PairingUi.WaitingCode) }
            } catch (e: Exception) {
                _ui.update { it.copy(pairing = PairingUi.Error(describe(e), retryable = true)) }
            }
        }
    }

    fun submitCode(code: String) {
        val screen = _ui.value.screen as? Screen.Pairing ?: return
        val client = pairing ?: return
        _ui.update { it.copy(pairing = PairingUi.Verifying) }
        viewModelScope.launch {
            try {
                client.finish(code)
                store.save(screen.tv)
                _ui.update { it.copy(saved = store.load()) }
                openRemote(screen.tv)
            } catch (e: PairingException) {
                // A wrong code keeps the session open so the user can type it again.
                val retryable = e.message != WRONG_CODE
                _ui.update {
                    it.copy(pairing = if (retryable) PairingUi.Error(describe(e), true) else PairingUi.WaitingCode)
                }
            } catch (e: Exception) {
                _ui.update { it.copy(pairing = PairingUi.Error(describe(e), retryable = true)) }
            }
        }
    }

    fun reconnect() {
        (_ui.value.screen as? Screen.Remote)?.tv?.let(::openRemote)
    }

    /** Leaves the TV list or pairing and returns to the remote, reconnecting if pairing had dropped it. */
    fun back() {
        pairing?.close()
        val tv = current
        when {
            tv == null -> _ui.update { it.copy(screen = Screen.Remote(null)) }
            remote != null -> _ui.update { it.copy(screen = Screen.Remote(tv)) }
            else -> openRemote(tv)
        }
    }

    fun key(code: Int) {
        remote?.key(code)
    }

    fun type(text: String) {
        remote?.type(text)
    }


    private fun openRemote(tv: Tv) {
        closeRemote()
        current = tv
        store.save(tv)
        _ui.update { it.copy(saved = store.load()) }
        val client = RemoteClient(tv.host, viewModelScope)
        remote = client
        _ui.update { it.copy(screen = Screen.Remote(tv)) }
        remoteJob = viewModelScope.launch { client.state.collect { _remoteState.value = it } }
        client.connect()
    }

    private fun closeRemote() {
        remoteJob?.cancel()
        remote?.close()
        remote = null
        _remoteState.value = RemoteState.Idle
    }

    private fun describe(e: Exception): String = e.message?.takeIf { it.isNotBlank() } ?: "Erreur réseau"

    override fun onCleared() {
        discovery.stop()
        pairing?.close()
        closeRemote()
    }

    private companion object {
        const val WRONG_CODE = "Code incorrect"
    }
}
