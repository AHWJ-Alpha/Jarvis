package com.jarvis.app.runtime

import android.content.Context
import android.util.Base64
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.SecureRandom

data class RemotePairingInfo(val code: String, val expiresAt: Long)

/** Pairing and bearer-token storage for the opt-in remote web console. */
class RemoteAccessManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("jarvis_remote_access", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var remoteDeviceControl: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_CONTROL, false)
        set(value) { prefs.edit().putBoolean(KEY_DEVICE_CONTROL, value).apply() }

    val isPaired: Boolean get() = !prefs.getString(KEY_TOKEN, null).isNullOrBlank()

    fun newPairingCode(): RemotePairingInfo {
        val value = (10000000 + random.nextInt(90000000)).toString()
        val expiry = System.currentTimeMillis() + PAIR_TTL_MS
        prefs.edit().putString(KEY_PAIR_CODE, value).putLong(KEY_PAIR_EXPIRY, expiry)
            .putInt(KEY_PAIR_ATTEMPTS, 0).putLong(KEY_LOCKOUT, 0).apply()
        return RemotePairingInfo(value, expiry)
    }

    fun currentPairingInfo(): RemotePairingInfo? {
        val code = prefs.getString(KEY_PAIR_CODE, null) ?: return null
        val expiry = prefs.getLong(KEY_PAIR_EXPIRY, 0)
        if (expiry <= System.currentTimeMillis()) return null
        return RemotePairingInfo(code, expiry)
    }

    fun pair(code: String): String? {
        if (!enabled || System.currentTimeMillis() < prefs.getLong(KEY_LOCKOUT, 0)) return null
        val pairing = currentPairingInfo() ?: return null
        val supplied = code.trim().toByteArray(Charsets.UTF_8)
        val expected = pairing.code.toByteArray(Charsets.UTF_8)
        if (!MessageDigest.isEqual(supplied, expected)) {
            val attempts = prefs.getInt(KEY_PAIR_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_PAIR_ATTEMPTS, attempts)
            if (attempts >= MAX_PAIR_ATTEMPTS) editor.putLong(KEY_LOCKOUT, System.currentTimeMillis() + LOCKOUT_MS)
            editor.apply()
            return null
        }
        val tokenBytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.encodeToString(tokenBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        prefs.edit().putString(KEY_TOKEN, token).remove(KEY_PAIR_CODE).remove(KEY_PAIR_EXPIRY)
            .remove(KEY_PAIR_ATTEMPTS).remove(KEY_LOCKOUT).apply()
        return token
    }

    fun authenticates(token: String): Boolean {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return false
        return MessageDigest.isEqual(stored.toByteArray(Charsets.UTF_8), token.trim().toByteArray(Charsets.UTF_8))
    }

    fun revoke() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_PAIR_CODE).remove(KEY_PAIR_EXPIRY)
            .remove(KEY_PAIR_ATTEMPTS).remove(KEY_LOCKOUT).apply()
    }

    fun localUrls(port: Int = RemoteAccessServer.PORT): List<String> = runCatching {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        interfaces.toList().filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filter { address ->
                val bytes = address.address.map { it.toInt() and 0xff }
                val tailscale = bytes.size == 4 && bytes[0] == 100 && bytes[1] in 64..127
                !address.isLoopbackAddress && (address.isSiteLocalAddress || tailscale)
            }
            .distinctBy { it.hostAddress }
            .map { "http://${it.hostAddress}:$port" }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_DEVICE_CONTROL = "remoteDeviceControl"
        private const val KEY_TOKEN = "bearerToken"
        private const val KEY_PAIR_CODE = "pairCode"
        private const val KEY_PAIR_EXPIRY = "pairExpiry"
        private const val KEY_PAIR_ATTEMPTS = "pairAttempts"
        private const val KEY_LOCKOUT = "pairLockout"
        const val PAIR_TTL_MS = 5 * 60 * 1000L
        const val LOCKOUT_MS = 5 * 60 * 1000L
        const val MAX_PAIR_ATTEMPTS = 5
    }
}
