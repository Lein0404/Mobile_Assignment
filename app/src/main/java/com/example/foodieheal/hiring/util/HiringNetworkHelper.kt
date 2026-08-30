package com.example.foodieheal.hiring.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.foodieheal.MainActivity
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * I add this because when I use normal network monitor class to hiring module
 * sometime it cannot get my network connection no whether I turn from offline to active network
 *
 */

object HiringNetworkHelper {

    /**
     * Synchronously checks if the device currently has an active network with internet capability.
     */
    fun isDeviceOnline(context: Context? = MainActivity.appContext): Boolean {
        val ctx = context ?: return false
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Observes network changes safely for the hiring module.
     * When transitioning from offline to online, Android can fire momentary teardown events
     * (onLost on stale/inactive network interfaces). This helper debounces and verifies against
     * the active network to prevent false offline messages from popping up.
     */
    fun observeHiringNetwork(
        networkMonitor: NetworkMonitor?,
        coroutineScope: CoroutineScope,
        stateFlow: MutableStateFlow<Boolean>,
        onReconnected: () -> Unit = {}
    ) {
        val monitor = networkMonitor ?: MainActivity.appContext?.let { NetworkMonitor(it) }

        // Initialize state accurately from current system network status
        stateFlow.value = isDeviceOnline()

        monitor?.let { m ->
            coroutineScope.launch {
                m.isConnected.collectLatest { connected ->
                    if (connected) {
                        stateFlow.value = true
                        onReconnected()
                    } else {
                        // When false is emitted, wait briefly and verify whether the device
                        // actually lost internet or if it was just a transient interface teardown.
                        delay(400)
                        val actuallyOnline = isDeviceOnline()
                        stateFlow.value = actuallyOnline
                        if (actuallyOnline) {
                            onReconnected()
                        }
                    }
                }
            }
        }
    }
}
