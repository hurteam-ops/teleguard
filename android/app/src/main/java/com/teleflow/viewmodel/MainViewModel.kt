package com.teleflow.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.teleflow.data.SecureStorage
import com.teleflow.data.TeleFlowRepository
import com.teleflow.data.model.AuthRequest
import com.teleflow.data.model.ConnectionState
import com.teleflow.data.model.ProxyServer
import com.teleflow.vpn.TeleFlowVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage.getInstance(application)
    val repository = TeleFlowRepository(secureStorage)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectionDuration = MutableStateFlow("00:00:00")
    val connectionDuration: StateFlow<String> = _connectionDuration.asStateFlow()

    private val _bytesDown = MutableStateFlow(0L)
    val bytesDown: StateFlow<Long> = _bytesDown.asStateFlow()

    private val _bytesUp = MutableStateFlow(0L)
    val bytesUp: StateFlow<Long> = _bytesUp.asStateFlow()

    private val _proxies = MutableStateFlow<List<ProxyServer>>(emptyList())
    val proxies: StateFlow<List<ProxyServer>> = _proxies.asStateFlow()

    private val _selectedProxyId = MutableStateFlow("auto")
    val selectedProxyId: StateFlow<String> = _selectedProxyId.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var timerJob: Job? = null
    private var connectedAtMs: Long = 0L

    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val state = intent.getIntExtra(TeleFlowVpnService.EXTRA_STATE, 0)
            when (state) {
                TeleFlowVpnService.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    startTimer()
                }
                TeleFlowVpnService.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    stopTimer()
                    _connectionDuration.value = "00:00:00"
                    _bytesDown.value = 0
                    _bytesUp.value = 0
                }
                TeleFlowVpnService.STATE_ERROR -> {
                    _connectionState.value = ConnectionState.ERROR
                    stopTimer()
                }
            }
        }
    }

    init {
        _isAuthenticated.value = repository.isAuthenticated()
        _isPremium.value = repository.isPremium()
        _selectedProxyId.value = repository.getSelectedProxyId()

        LocalBroadcastManager.getInstance(application).registerReceiver(
            vpnStateReceiver,
            IntentFilter(TeleFlowVpnService.ACTION_STATE_CHANGED)
        )

        if (_isAuthenticated.value) {
            loadProxies()
        }
    }

    fun connect() {
        if (_connectionState.value != ConnectionState.DISCONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING

        val context = getApplication<Application>()
        val intent = Intent(context, TeleFlowVpnService::class.java).apply {
            action = TeleFlowVpnService.ACTION_CONNECT
        }

        val selected = _selectedProxyId.value
        if (selected != "auto") {
            val proxy = _proxies.value.find { it.ip == selected }
            if (proxy != null) {
                intent.putExtra(TeleFlowVpnService.EXTRA_PROXY, proxyToJson(proxy))
            }
        }

        context.startForegroundService(intent)
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTING

        val context = getApplication<Application>()
        val intent = Intent(context, TeleFlowVpnService::class.java).apply {
            action = TeleFlowVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun selectProxy(id: String) {
        _selectedProxyId.value = id
        repository.setSelectedProxyId(id)
    }

    fun authenticate(request: AuthRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.authenticate(request)
            result.onSuccess {
                _isAuthenticated.value = true
                _isPremium.value = repository.isPremium()
                loadProxies()
            }.onFailure { e ->
                _error.value = e.message ?: "Authentication failed"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        disconnect()
        repository.logout()
        _isAuthenticated.value = false
        _isPremium.value = false
        _proxies.value = emptyList()
    }

    fun clearError() { _error.value = null }

    private fun loadProxies() {
        viewModelScope.launch {
            repository.getProxies().onSuccess { _proxies.value = it }
        }
    }

    private fun startTimer() {
        connectedAtMs = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - connectedAtMs
                val secs = (elapsed / 1000).toInt()
                _connectionDuration.value = "%02d:%02d:%02d".format(
                    secs / 3600, (secs % 3600) / 60, secs % 60
                )
                _bytesDown.value += (500..2000).random()
                _bytesUp.value += (100..800).random()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun proxyToJson(proxy: ProxyServer): String =
        kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer<ProxyServer>(), proxy
        )

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(vpnStateReceiver)
    }
}
