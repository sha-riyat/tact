package dev.remotectrl

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.remotectrl.ui.DevicesScreen
import dev.remotectrl.ui.PairingScreen
import dev.remotectrl.ui.RemoteScreen
import dev.remotectrl.ui.RemoteTheme
import dev.remotectrl.ui.plate

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The UI is always dark, so the system bars always get light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { App(vm) }
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val found by vm.found.collectAsStateWithLifecycle()
    val remoteState by vm.remoteState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        vm.startDiscovery()
        onDispose { vm.stopDiscovery() }
    }

    RemoteTheme {
        Surface(modifier = Modifier.fillMaxSize().plate().safeDrawingPadding(), color = androidx.compose.ui.graphics.Color.Transparent) {
            when (val screen = ui.screen) {
                Screen.Devices -> {
                    BackHandler(onBack = vm::back)
                    DevicesScreen(
                        saved = ui.saved,
                        found = found,
                        onOpen = vm::openTv,
                        onForget = vm::forget,
                        onManual = vm::addManual,
                    )
                }
                is Screen.Pairing -> {
                    BackHandler(onBack = vm::back)
                    PairingScreen(
                        tv = screen.tv,
                        state = ui.pairing,
                        onSubmit = vm::submitCode,
                        onRetry = { vm.beginPairing(screen.tv) },
                        onCancel = vm::back,
                    )
                }
                is Screen.Remote -> {
                    RemoteScreen(
                        tv = screen.tv,
                        state = remoteState,
                        saved = ui.saved,
                        found = found,
                        onOpen = vm::openTv,
                        onKey = vm::key,
                        onType = vm::type,
                        onReconnect = vm::reconnect,
                        onRepair = { screen.tv?.let(vm::beginPairing) },
                        onDevices = vm::showDevices,
                    )
                }
            }
        }
    }
}
