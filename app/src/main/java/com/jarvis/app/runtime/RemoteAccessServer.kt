package com.jarvis.app.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/** Optional paired web console. It never mirrors the screen or issues remote touch events. */
class RemoteAccessServer(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val access = RemoteAccessManager(appContext)
    private val tasks = RemoteTaskStore(appContext)
    private val executor = Executors.newFixedThreadPool(4)
    @Volatile private var running = false
    @Volatile private var server: ServerSocket? = null

    fun isRunning(): Boolean = running && server?.isClosed == false

    fun start(): Boolean {
        if (isRunning()) return true
        if (!access.enabled) return false
        tasks.recoverInterrupted()
        return runCatching {
            server = ServerSocket(PORT, 24, InetAddress.getByName("0.0.0.0"))
            running = true
            executor.execute {
                while (running) {
                    runCatching { server?.accept()?.let { socket -> executor.execute { handle(socket) } } }
                        .onFailure { if (running) Log.w(TAG, "Remote request accept failed", it) }
                }
            }
            tasks.queued().forEach { schedule(it.id) }
            true
        }.getOrElse {
            running = false
            runCatching { server?.close() }
            server = null
            Log.e(TAG, "Could not start remote web console", it)
            false
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
        executor.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = SOCKET_TIMEOUT_MS
            val input = BufferedInputStream(client.getInputStream())
            val headerText = readHeader(input) ?: return
            val lines = headerText.split("\r\n")
            val requestLine = lines.firstOrNull().orEmpty().split(' ')
            val method = requestLine.getOrNull(0).orEmpty().uppercase()
            val target = requestLine.getOrNull(1).orEmpty()
            if (method !in setOf("GET", "POST")) {
                respond(client, 405, "application/json; charset=utf-8", jsonError("method_not_allowed"))
                return
            }
            val headers = lines.drop(1).mapNotNull { line ->
                val colon = line.indexOf(':')
                if (colon <= 0) null else line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
            }.toMap()
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            if (length < 0 || length > MAX_BODY_BYTES) {
                respond(client, 413, "application/json; charset=utf-8", jsonError("request_too_large"))
                return
            }
            val bodyBytes = ByteArray(length)
            var offset = 0
            while (offset < bodyBytes.size) {
                val count = input.read(bodyBytes, offset, bodyBytes.size - offset)
                if (count <= 0) {
                    respond(client, 400, "application/json; charset=utf-8", jsonError("incomplete_body"))
                    return
                }
                offset += count
            }
            val body = bodyBytes.toString(StandardCharsets.UTF_8)
            val splitTarget = target.split('?', limit = 2)
            val path = splitTarget.firstOrNull().orEmpty()
            val query = parseQuery(splitTarget.getOrNull(1).orEmpty())
            val result = route(client, method, path, query, headers["authorization"].orEmpty(), body)
            if (result != null) respond(client, result.first, result.second, result.third)
        }
    }

    private fun route(
        client: Socket,
        method: String,
        path: String,
        query: Map<String, String>,
        authorization: String,
        body: String
    ): Triple<Int, String, String>? {
        if (method == "GET" && (path == "/" || path == "/index.html")) {
            val html = runCatching {
                appContext.assets.open("remote/index.html").bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            }.getOrElse { return Triple(500, "application/json; charset=utf-8", jsonError("web_console_unavailable")) }
            return Triple(200, "text/html; charset=utf-8", html)
        }
        if (method == "POST" && path == "/api/pair") {
            if (!running || !access.enabled) return Triple(503, "application/json; charset=utf-8", jsonError("remote_access_disabled"))
            val code = runCatching { JSONObject(body).optString("code") }.getOrDefault("")
            val token = access.pair(code)
            return if (token == null) Triple(401, "application/json; charset=utf-8", jsonError("pairing_failed"))
            else Triple(200, "application/json; charset=utf-8", JSONObject().put("ok", true).put("token", token).toString())
        }
        if (!running || !access.enabled) return Triple(503, "application/json; charset=utf-8", jsonError("remote_access_disabled"))
        val token = authorization.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(' ')
            ?.trim()
            .orEmpty()
        if (!access.authenticates(token)) return Triple(401, "application/json; charset=utf-8", jsonError("unauthorized"))

        return when {
            method == "GET" && path == "/api/status" -> {
                Triple(200, "application/json; charset=utf-8", JSONObject()
                    .put("ok", true)
                    .put("deviceControl", access.remoteDeviceControl)
                    .put("activeTask", DeviceTaskCoordinator.activeTask().orEmpty())
                    .put("busy", DeviceTaskCoordinator.activeTask() != null)
                    .put("app", "Jarvis")
                    .toString())
            }
            method == "GET" && path == "/api/tasks" -> {
                val sessionId = query["session"].orEmpty()
                val items = tasks.list(sessionId).filter { sessionId.isNotBlank() }
                Triple(200, "application/json; charset=utf-8", JSONObject().put("ok", true)
                    .put("tasks", JSONArray().also { array -> items.forEach { array.put(it.toJson()) } }).toString())
            }
            method == "POST" && path == "/api/tasks" -> {
                val task = runCatching {
                    val json = JSONObject(body)
                    tasks.enqueue(json.optString("sessionId"), json.optString("prompt"))
                }.getOrElse { error ->
                    return Triple(400, "application/json; charset=utf-8", jsonError(error.message ?: "invalid_task"))
                }
                schedule(task.id)
                Triple(202, "application/json; charset=utf-8", JSONObject().put("ok", true).put("task", task.toJson()).toString())
            }
            else -> Triple(404, "application/json; charset=utf-8", jsonError("not_found"))
        }
    }

    private fun schedule(id: String) {
        scope.launch {
            val store = RemoteTaskStore(appContext)
            try {
                DeviceTaskCoordinator.run("远程任务") {
                    val task = store.claim(id) ?: return@run
                    if (!access.enabled) {
                        store.complete(task.id, "远程访问已关闭，任务未执行。", RemoteTaskState.FAILED)
                        return@run
                    }
                    val allowed = access.remoteDeviceControl
                    try {
                        val result = RemoteExecutionGuard.run(allowed) {
                            RemoteTaskAgent(appContext).run(task, allowed)
                        }
                        store.complete(task.id, result)
                    } catch (error: Exception) {
                        store.complete(task.id, "远程任务失败：${error.message ?: error.javaClass.simpleName}", RemoteTaskState.FAILED)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Remote task failed before execution", error)
                store.complete(id, "远程任务无法启动：${error.message ?: error.javaClass.simpleName}", RemoteTaskState.FAILED)
            }
        }
    }

    private fun readHeader(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        while (true) {
            val current = input.read()
            if (current < 0) return null
            bytes.add(current.toByte())
            val count = bytes.size
            if (count >= 4 && bytes[count - 4] == 13.toByte() && bytes[count - 3] == 10.toByte() &&
                bytes[count - 2] == 13.toByte() && bytes[count - 1] == 10.toByte()) break
            if (count > MAX_HEADER_BYTES) return null
        }
        return bytes.toByteArray().toString(StandardCharsets.US_ASCII).removeSuffix("\r\n\r\n")
    }

    private fun parseQuery(raw: String): Map<String, String> = raw.split('&').mapNotNull { entry ->
        val parts = entry.split('=', limit = 2)
        if (parts.firstOrNull().isNullOrBlank()) null else runCatching {
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }.getOrNull()
    }.toMap()

    private fun respond(client: Socket, code: Int, contentType: String, payload: String) {
        runCatching {
            val bytes = payload.toByteArray(StandardCharsets.UTF_8)
            val reason = when (code) {
                200 -> "OK"
                202 -> "Accepted"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                413 -> "Content Too Large"
                500 -> "Internal Server Error"
                503 -> "Service Unavailable"
                else -> "Error"
            }
            val header = "HTTP/1.1 $code $reason\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "Cache-Control: no-store\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "Referrer-Policy: no-referrer\r\n" +
                "Content-Security-Policy: default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'\r\n\r\n"
            client.getOutputStream().apply {
                write(header.toByteArray(StandardCharsets.US_ASCII))
                write(bytes)
                flush()
            }
        }
    }

    private fun jsonError(message: String) = JSONObject().put("ok", false).put("error", message.take(200)).toString()

    companion object {
        const val PORT = 18792
        private const val TAG = "JarvisRemote"
        private const val SOCKET_TIMEOUT_MS = 10_000
        private const val MAX_HEADER_BYTES = 16_384
        private const val MAX_BODY_BYTES = 64 * 1024
    }
}
