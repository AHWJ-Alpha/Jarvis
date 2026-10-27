package com.jarvis.app.runtime

import android.content.Context
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/** Local, token-protected RPC endpoint for OpenClaw tools. */
class JarvisBridgeServer(
    context: Context,
    private val port: Int = 18791
) {
    private val token = BootstrapController(context).bridgeToken()
    private val bridge = DeviceCapabilityBridge(context)
    private val executor = Executors.newCachedThreadPool()
    @Volatile private var running = false
    private var server: ServerSocket? = null

    fun isRunning(): Boolean = running && server?.isClosed == false

    fun start(): Boolean {
        if (running) return true
        return runCatching {
            server = ServerSocket(port, 25, InetAddress.getByName("127.0.0.1"))
            running = true
            executor.execute {
                while (running) {
                    runCatching { server?.accept()?.let { socket -> executor.execute { handle(socket) } } }
                }
            }
            true
        }.getOrDefault(false)
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
        executor.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10_000
            val input = BufferedInputStream(client.getInputStream())
            val headerText = readHeader(input) ?: return
            val headerLines = headerText.split("\r\n")
            val first = headerLines.firstOrNull() ?: return
            val headers = mutableMapOf<String, String>()
            for (line in headerLines.drop(1)) {
                val separator = line.indexOf(':')
                if (separator > 0) headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
            }
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            if (contentLength < 0 || contentLength > 262_144) return
            val bodyBytes = ByteArray(contentLength)
            var offset = 0
            while (offset < bodyBytes.size) {
                val count = input.read(bodyBytes, offset, bodyBytes.size - offset)
                if (count <= 0) break
                offset += count
            }
            val body = bodyBytes.toString(StandardCharsets.UTF_8)
            val request = first.split(' ')
            val method = request.getOrNull(0).orEmpty()
            val path = request.getOrNull(1).orEmpty()
            val suppliedToken = headers["authorization"].orEmpty().removePrefix("Bearer ").trim()
            val result = when {
                token.length < 32 || suppliedToken != token -> JSONObject().put("ok", false).put("error", "unauthorized") to 401
                method == "GET" && path == "/health" -> JSONObject().put("ok", true).put("service", "jarvis-bridge") to 200
                method == "GET" && path == "/v1/capabilities" -> capabilities() to 200
                method == "POST" && path == "/v1/capabilities" -> execute(body) to 200
                else -> JSONObject().put("ok", false).put("error", "not_found") to 404
            }
            val payload = result.first.toString()
            val writer = OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)
            writer.write("HTTP/1.1 ${result.second} ${if (result.second == 200) "OK" else "Error"}\r\n")
            writer.write("Content-Type: application/json\r\nContent-Length: ${payload.toByteArray(StandardCharsets.UTF_8).size}\r\nConnection: close\r\n\r\n")
            writer.write(payload)
            writer.flush()
        }
    }

    private fun readHeader(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        var previous = 0
        var current: Int
        while (true) {
            current = input.read()
            if (current < 0) return null
            bytes.add(current.toByte())
            if (bytes.size >= 4 && bytes[bytes.size - 4] == '\r'.code.toByte() && bytes[bytes.size - 3] == '\n'.code.toByte() && bytes[bytes.size - 2] == '\r'.code.toByte() && bytes[bytes.size - 1] == '\n'.code.toByte()) break
            previous = current
            if (bytes.size > 32768) return null
        }
        return bytes.toByteArray().toString(StandardCharsets.US_ASCII).removeSuffix("\r\n\r\n")
    }

    private fun capabilities(): JSONObject = JSONObject().apply {
        put("ok", true)
        put("bridge", "127.0.0.1:$port")
        put("capabilities", DeviceCapabilityBridge.capabilityNames().joinToString(","))
    }

    private fun execute(raw: String): JSONObject {
        return runCatching {
            val input = JSONObject(raw)
            val capability = input.optString("capability")
            val risk = when (capability) {
                "device.info", "accessibility.inspect", "ocr.read" -> CapabilityRisk.LOW
                "root.exec", "accessibility.click" -> CapabilityRisk.HIGH
                else -> CapabilityRisk.MEDIUM
            }
            val result = bridge.execute(
                CapabilityRequest(capability, input.optJSONObject("arguments") ?: JSONObject(), risk, false),
                false
            )
            JSONObject().put("ok", result.ok).put("message", result.message).put("data", result.data)
        }.getOrElse { JSONObject().put("ok", false).put("error", "invalid_request: ${it.message}") }
    }
}
