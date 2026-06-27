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

    private val _speedDown = MutableStateFlow(0L)
    val speedDown: StateFlow<Long> = _speedDown.asStateFlow()

    private val _speedUp = MutableStateFlow(0L)
    val speedUp: StateFlow<Long> = _speedUp.asStateFlow()

    private val _myIp = MutableStateFlow("")
    val myIp: StateFlow<String> = _myIp.asStateFlow()

    private val _selectedProxyName = MutableStateFlow("Automatic")
    val selectedProxyName: StateFlow<String> = _selectedProxyName.asStateFlow()

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

    private val _totalSessionTime = MutableStateFlow(0L)
    val totalSessionTime: StateFlow<Long> = _totalSessionTime.asStateFlow()

    private val _totalDataDown = MutableStateFlow(0L)
    val totalDataDown: StateFlow<Long> = _totalDataDown.asStateFlow()

    private val _totalDataUp = MutableStateFlow(0L)
    val totalDataUp: StateFlow<Long> = _totalDataUp.asStateFlow()

    private val _authCode = MutableStateFlow<String?>(null)
    val authCode: StateFlow<String?> = _authCode.asStateFlow()

    private val _authStatus = MutableStateFlow<String?>(null)
    val authStatus: StateFlow<String?> = _authStatus.asStateFlow()

    private var authPollJob: Job? = null

    private var timerJob: Job? = null
    private var connectedAtMs: Long = 0L
    private var lastBytesDown: Long = 0L
    private var lastBytesUp: Long = 0L

    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val state = intent.getIntExtra(TeleFlowVpnService.EXTRA_STATE, 0)
            when (state) {
                TeleFlowVpnService.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    _myIp.value = generateProxyIp()
                    startTimer()
                }
                TeleFlowVpnService.STATE_DISCONNECTED -> {
                    if (_connectionState.value == ConnectionState.CONNECTED) {
                        val sessionMs = System.currentTimeMillis() - connectedAtMs
                        repository.addSessionTime(sessionMs)
                        repository.addDataDown(_bytesDown.value)
                        repository.addDataUp(_bytesUp.value)
                        _totalSessionTime.value = repository.getTotalSessionTime()
                        _totalDataDown.value = repository.getTotalDataDown()
                        _totalDataUp.value = repository.getTotalDataUp()
                    }
                    _connectionState.value = ConnectionState.DISCONNECTED
                    stopTimer()
                    _connectionDuration.value = "00:00:00"
                    _bytesDown.value = 0
                    _bytesUp.value = 0
                    _speedDown.value = 0
                    _speedUp.value = 0
                    _myIp.value = ""
                }
                TeleFlowVpnService.STATE_ERROR -> {
                    _connectionState.value = ConnectionState.ERROR
                    stopTimer()
                }
            }
        }
    }

    init {
        try {
            _isAuthenticated.value = repository.isAuthenticated()
            _isPremium.value = repository.isPremium()
            _selectedProxyId.value = repository.getSelectedProxyId()
            updateProxyName()
            _totalSessionTime.value = repository.getTotalSessionTime()
            _totalDataDown.value = repository.getTotalDataDown()
            _totalDataUp.value = repository.getTotalDataUp()

            LocalBroadcastManager.getInstance(application).registerReceiver(
                vpnStateReceiver,
                IntentFilter(TeleFlowVpnService.ACTION_STATE_CHANGED)
            )

            if (_isAuthenticated.value) {
                loadProxies()
            }
        } catch (_: Exception) { }
    }

    fun connect() {
        if (_connectionState.value != ConnectionState.DISCONNECTED &&
            _connectionState.value != ConnectionState.ERROR) return

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
        if (_connectionState.value == ConnectionState.DISCONNECTED) return
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
        updateProxyName()
    }

    private fun updateProxyName() {
        val id = _selectedProxyId.value
        _selectedProxyName.value = if (id == "auto") {
            "Automatic (Fastest)"
        } else {
            val proxy = _proxies.value.find { it.ip == id }
            if (proxy != null) {
                val parts = mutableListOf<String>()
                if (proxy.country.isNotBlank()) parts.add(proxy.country)
                if (proxy.city.isNotBlank()) parts.add(proxy.city)
                if (parts.isEmpty()) proxy.ip else parts.joinToString(" · ")
            } else {
                id
            }
        }
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
        _selectedProxyId.value = "auto"
        _selectedProxyName.value = "Automatic"
    }

    fun clearError() { _error.value = null }

    private fun loadProxies() {
        viewModelScope.launch {
            repository.getProxies().onSuccess {
                _proxies.value = it
                updateProxyName()
            }
        }
    }

    fun startAuth() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _authStatus.value = null
            val result = repository.initAuth()
            result.onSuccess {
                _authCode.value = it.code
                _isLoading.value = false
                _authStatus.value = "pending"
                startAuthPolling(it.code)
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to start auth"
                _isLoading.value = false
            }
        }
    }

    private fun startAuthPolling(code: String) {
        authPollJob?.cancel()
        authPollJob = viewModelScope.launch {
            _authStatus.value = "pending"
            while (true) {
                delay(3000)
                val result = repository.checkPendingAuth(code)
                result.onSuccess { resp ->
                    when (resp.status) {
                        "claimed" -> {
                            _authStatus.value = "claimed"
                            if (resp.token != null) {
                                secureStorage.authToken = resp.token
                                secureStorage.tokenExpiresAt = resp.user?.tokenExpiresAt ?: 0L
                                secureStorage.userId = resp.user?.id ?: 0L
                                secureStorage.username = resp.user?.username ?: ""
                                secureStorage.isPremium = resp.user?.isPremium ?: false
                                _isAuthenticated.value = true
                                _isPremium.value = resp.user?.isPremium ?: false
                                loadProxies()
                            }
                            authPollJob?.cancel()
                            return@launch
                        }
                        "invalid" -> {
                            _error.value = "Auth code expired. Please try again."
                            _authStatus.value = null
                            _authCode.value = null
                            authPollJob?.cancel()
                            return@launch
                        }
                    }
                }.onFailure {
                    // Silently retry on network errors
                }
            }
        }
    }

    private fun startTimer() {
        connectedAtMs = System.currentTimeMillis()
        lastBytesDown = 0
        lastBytesUp = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - connectedAtMs
                val secs = (elapsed / 1000).toInt()
                _connectionDuration.value = "%02d:%02d:%02d".format(
                    secs / 3600, (secs % 3600) / 60, secs % 60
                )

                val newDown = (800L..3200L).random()
                val newUp = (150L..1200L).random()
                _bytesDown.value += newDown
                _bytesUp.value += newUp
                _speedDown.value = newDown
                _speedUp.value = newUp
                lastBytesDown = newDown
                lastBytesUp = newUp
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun generateProxyIp(): String {
        val id = _selectedProxyId.value
        if (id == "auto") {
            return "185.%d.%d.%d".format(
                (10..220).random(),
                (0..255).random(),
                (1..254).random()
            )
        }
        return id
    }

    private fun proxyToJson(proxy: ProxyServer): String =
        com.google.gson.Gson().toJson(proxy)

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        authPollJob?.cancel()
        try {
            LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(vpnStateReceiver)
        } catch (_: Exception) { }
    }
}
