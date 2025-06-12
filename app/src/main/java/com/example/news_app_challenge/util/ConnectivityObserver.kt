package com.example.news_app_challenge.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// Enum to represent different states of network connection.
enum class ConnectionStatus {
    AVAILABLE,    // Network is connected and has internet access
    LOSING,       // Network is losing connectivity
    LOST,         // Network connection is lost
    UNAVAILABLE   // Network is not available (e.g., no active networks)
}

// Interface for observing Connectivity Manager
interface ConnectivityObserver {
    fun observe(): Flow<ConnectionStatus>
}


class ConnectivityObserverImpl(
    private val context: Context
) : ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectionStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                launch { send(ConnectionStatus.AVAILABLE) }
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                launch { send(ConnectionStatus.LOSING) }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                launch { send(ConnectionStatus.LOST) }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                launch { send(ConnectionStatus.UNAVAILABLE) }
            }
        }

        // Request for networks that have internet capability
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Register the network callback to receive updates
        connectivityManager.registerNetworkCallback(networkRequest, callback)

        // Send the initial network status immediately
        val initialStatus = if (isNetworkConnected()) ConnectionStatus.AVAILABLE else ConnectionStatus.UNAVAILABLE
        send(initialStatus)

        // Keep the flow active until the collector cancels
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() 

// Determines if there is network connectivity
    private fun isNetworkConnected(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}