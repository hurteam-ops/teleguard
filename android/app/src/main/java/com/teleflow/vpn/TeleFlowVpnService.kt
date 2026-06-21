package com.teleflow.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.teleflow.ui.MainActivity
import com.teleflow.data.model.ProxyServer
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class TeleFlowVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var forwardJob: Job? = null
    private var proxySocket: Socket? = null
    private var connected = false

    companion object {
        const val ACTION_CONNECT = "com.teleflow.action.CONNECT"
        const val ACTION_DISCONNECT = "com.teleflow.action.DISCONNECT"
        const val EXTRA_PROXY = "extra_proxy_config"
        const val EXTRA_SOCKS_USER = "extra_socks_user"
        const val EXTRA_SOCKS_PASS = "extra_socks_pass"
        const val ACTION_STATE_CHANGED = "com.teleflow.action.STATE_CHANGED"
        const val EXTRA_STATE = "extra_state"
        const val STATE_CONNECTED = 1
        const val STATE_DISCONNECTED = 0
        const val STATE_ERROR = -1
        private const val CHANNEL_ID = "teleflow_vpn"
        private const val NOTIFICATION_ID = 1
        private const val MTU = 1500
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Ready"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val proxyJson = intent.getStringExtra(EXTRA_PROXY)
                if (proxyJson != null) {
                    val proxy = kotlinx.serialization.json.Json
                        .decodeFromString<ProxyServer>(proxyJson)
                    val user = intent.getStringExtra(EXTRA_SOCKS_USER) ?: ""
                    val pass = intent.getStringExtra(EXTRA_SOCKS_PASS) ?: ""
                    connect(proxy, user, pass)
                }
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        disconnect()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() = disconnect()

    private fun connect(proxy: ProxyServer, socksUser: String, socksPass: String) {
        try {
            updateNotification("Connecting")

            val tun = Builder()
                .setName("teleflow")
                .setMtu(MTU)
                .addAddress("10.88.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .setSession("TeleFlow")
                .setBlocking(true)
                .setConfigureIntent(
                    PendingIntent.getActivity(
                        this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .establish()

            if (tun == null) {
                updateNotification("Permission denied")
                broadcastState(STATE_ERROR)
                return
            }

            vpnInterface = tun
            forwardJob = scope.launch {
                try {
                    pipe(tun, proxy, socksUser, socksPass)
                    connected = true
                    withContext(Dispatchers.Main) {
                        updateNotification("Connected")
                        broadcastState(STATE_CONNECTED)
                    }
                } catch (e: Exception) {
                    connected = false
                    withContext(Dispatchers.Main) {
                        updateNotification("Error: ${e.message ?: "Unknown"}")
                        broadcastState(STATE_ERROR)
                    }
                }
            }
        } catch (e: Exception) {
            broadcastState(STATE_ERROR)
            updateNotification("Error")
        }
    }

    private suspend fun pipe(
        tun: ParcelFileDescriptor,
        proxy: ProxyServer,
        socksUser: String,
        socksPass: String
    ) = withContext(Dispatchers.IO) {
        val tunIn = tun.fileDescriptor.inputStream()
        val tunOut = tun.fileDescriptor.outputStream()

        val sock = createTlsSocket(proxy.ip, proxy.port)
        proxySocket = sock

        socks5Handshake(sock, proxy.ip, proxy.port, socksUser, socksPass)

        val sockIn = sock.getInputStream()
        val sockOut = sock.getOutputStream()

        val a = launch { pump(tunIn, sockOut) }
        val b = launch { pump(sockIn, tunOut) }

        a.join()
        b.join()
    }

    private fun createTlsSocket(host: String, port: Int): Socket {
        return try {
            val sslCtx = SSLContext.getInstance("TLSv1.3")
            sslCtx.init(null, arrayOf(trustAllCerts), java.security.SecureRandom())
            val factory = sslCtx.socketFactory
            val tlsSocket = factory.createSocket() as SSLSocket
            tlsSocket.connect(InetSocketAddress(host, port), 10_000)
            tlsSocket.startHandshake()
            tlsSocket
        } catch (_: Exception) {
            Socket().also {
                it.connect(InetSocketAddress(host, port), 10_000)
            }
        }
    }

    private fun pump(input: InputStream, output: OutputStream) {
        val buf = ByteArray(MTU)
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            output.write(buf, 0, n)
            output.flush()
        }
    }

    private fun socks5Handshake(
        sock: Socket,
        targetHost: String,
        targetPort: Int,
        username: String,
        password: String
    ) {
        val out = sock.getOutputStream()
        val inp = sock.getInputStream()

        out.write(byteArrayOf(0x05, 0x01, 0x02))
        out.flush()

        val greetResp = readExact(inp, 2)
        if (greetResp[0] != 0x05.toByte() || greetResp[1] != 0x02.toByte()) {
            throw Exception("SOCKS5 server rejected auth method")
        }

        val uBytes = username.toByteArray(Charsets.UTF_8)
        val pBytes = password.toByteArray(Charsets.UTF_8)
        val authPacket = ByteArray(3 + uBytes.size + pBytes.size)
        authPacket[0] = 0x01
        authPacket[1] = uBytes.size.toByte()
        System.arraycopy(uBytes, 0, authPacket, 2, uBytes.size)
        authPacket[2 + uBytes.size] = pBytes.size.toByte()
        System.arraycopy(pBytes, 0, authPacket, 3 + uBytes.size, pBytes.size)
        out.write(authPacket)
        out.flush()

        val authResp = readExact(inp, 2)
        if (authResp[1] != 0x00.toByte()) {
            throw Exception("SOCKS5 authentication failed")
        }

        val hostBytes = targetHost.toByteArray(Charsets.UTF_8)
        val connectPacket = ByteArray(7 + hostBytes.size)
        connectPacket[0] = 0x05
        connectPacket[1] = 0x01
        connectPacket[2] = 0x00
        connectPacket[3] = 0x03
        connectPacket[4] = hostBytes.size.toByte()
        System.arraycopy(hostBytes, 0, connectPacket, 5, hostBytes.size)
        connectPacket[5 + hostBytes.size] = (targetPort shr 8).toByte()
        connectPacket[6 + hostBytes.size] = (targetPort and 0xFF).toByte()
        out.write(connectPacket)
        out.flush()

        val header = readExact(inp, 4)
        if (header[1] != 0x00.toByte()) {
            throw Exception("SOCKS5 CONNECT failed: code ${header[1]}")
        }

        when (header[3].toInt() and 0xFF) {
            0x01 -> readExact(inp, 4)
            0x03 -> {
                val len = readExact(inp, 1)
                readExact(inp, len[0].toInt() and 0xFF)
            }
            0x04 -> readExact(inp, 16)
        }
        readExact(inp, 2)
    }

    private fun readExact(inp: InputStream, count: Int): ByteArray {
        val buf = ByteArray(count)
        var off = 0
        while (off < count) {
            val n = inp.read(buf, off, count - off)
            if (n < 0) throw java.io.EOFException("Connection closed")
            off += n
        }
        return buf
    }

    private fun disconnect() {
        forwardJob?.cancel()
        forwardJob = null
        connected = false
        try { proxySocket?.close() } catch (_: Exception) {}
        proxySocket = null
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        updateNotification("Disconnected")
        broadcastState(STATE_DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastState(state: Int) {
        val intent = Intent(ACTION_STATE_CHANGED).putExtra(EXTRA_STATE, state)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "TeleFlow VPN", NotificationManager.IMPORTANCE_LOW)
        ch.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun notification(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TeleFlowVpnService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("TeleFlow")
            .setContentText(text)
            .setContentIntent(tap)
            .addAction(Notification.Action.Builder(null, "Disconnect", stop).build())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private val trustAllCerts = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}
