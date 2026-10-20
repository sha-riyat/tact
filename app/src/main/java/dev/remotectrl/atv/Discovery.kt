package dev.remotectrl.atv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dev.remotectrl.data.Tv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Finds Android TVs on the local Wi-Fi through mDNS (`_androidtvremote2._tcp`). */
class Discovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _found = MutableStateFlow<List<Tv>>(emptyList())
    val found: StateFlow<List<Tv>> = _found

    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var listener: NsdManager.DiscoveryListener? = null

    @Synchronized
    fun start() {
        if (listener != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = stop()
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(info: NsdServiceInfo) = enqueue(info)
            override fun onServiceLost(info: NsdServiceInfo) {
                _found.value = _found.value.filterNot { it.name == info.serviceName }
            }
        }
        listener = l
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l)
    }

    @Synchronized
    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
        pending.clear()
        resolving = false
    }

    // Older Android versions can only resolve one service at a time.
    @Synchronized
    private fun enqueue(info: NsdServiceInfo) {
        pending.addLast(info)
        resolveNext()
    }

    @Synchronized
    private fun resolveNext() {
        if (resolving) return
        val next = pending.removeFirstOrNull() ?: return
        resolving = true
        @Suppress("DEPRECATION")
        nsd.resolveService(next, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = done()

            override fun onServiceResolved(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val host = info.host?.hostAddress
                if (host != null) {
                    val tv = Tv(info.serviceName, host)
                    _found.value = _found.value.filterNot { it.host == host } + tv
                }
                done()
            }
        })
    }

    @Synchronized
    private fun done() {
        resolving = false
        resolveNext()
    }

    private companion object {
        const val SERVICE_TYPE = "_androidtvremote2._tcp."
    }
}
