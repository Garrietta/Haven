// Scriptable fake OkHttp for tools/connection-harness — NOT the real library.
// Only the API surface HaClient uses, with signatures matching OkHttp 4.12.
@file:Suppress("unused", "UNUSED_PARAMETER")
package okhttp3
import java.io.Closeable
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList

class ConnectionPool(maxIdleConnections: Int, keepAliveDuration: Long, timeUnit: TimeUnit)
interface Route
fun interface Authenticator { fun authenticate(route: Route?, response: Response): Request? }
class MediaType { companion object { fun String.toMediaTypeOrNull(): MediaType? = null } }
abstract class RequestBody { companion object { fun String.toRequestBody(contentType: MediaType? = null): RequestBody = object : RequestBody() {} } }
class FormBody : RequestBody() {
    class Builder { fun add(name: String, value: String): Builder = this; fun build(): FormBody = FormBody() }
}
class Request(val url: String, val headers: Map<String, String>) {
    fun header(name: String): String? = headers[name]
    fun newBuilder(): Builder = Builder().also { b -> b.u = url; b.h.putAll(headers) }
    class Builder {
        var u: String = ""
        val h = mutableMapOf<String, String>()
        fun url(url: String): Builder { require(url.startsWith("http") || url.startsWith("ws")) { "bad url" }; u = url; return this }
        fun header(name: String, value: String): Builder { h[name] = value; return this }
        fun post(body: RequestBody): Builder = this
        fun method(method: String, body: RequestBody?): Builder = this
        fun build(): Request = Request(u, h.toMap())
    }
}
abstract class ResponseBody : Closeable {
    abstract fun string(): String
    abstract fun bytes(): ByteArray
    abstract fun byteStream(): InputStream
}
class Response(val request: Request, val code: Int, private val text: String, val priorResponse: Response? = null) : Closeable {
    val isSuccessful: Boolean get() = code in 200..299
    val body: ResponseBody? get() = object : ResponseBody() {
        override fun string() = text
        override fun bytes() = text.toByteArray()
        override fun byteStream(): InputStream = text.byteInputStream()
        override fun close() {}
    }
    fun header(name: String): String? = null
    override fun close() {}
}
interface Call {
    fun execute(): Response
    fun cancel()
    fun isCanceled(): Boolean
    fun interface Factory { fun newCall(request: Request): Call }
}
interface WebSocket {
    fun send(text: String): Boolean
    fun close(code: Int, reason: String?): Boolean
    fun cancel()
}
abstract class WebSocketListener {
    open fun onOpen(webSocket: WebSocket, response: Response) {}
    open fun onMessage(webSocket: WebSocket, text: String) {}
    open fun onClosing(webSocket: WebSocket, code: Int, reason: String) {}
    open fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}
    open fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
}

/** Test-controllable fake socket. */
class FakeSocket(val url: String, val listener: WebSocketListener) : WebSocket {
    val sent = CopyOnWriteArrayList<String>()
    @Volatile var closed = false
    override fun send(text: String): Boolean { if (closed) return false; sent.add(text); return true }
    override fun close(code: Int, reason: String?): Boolean { if (closed) return false; closed = true; listener.onClosed(this, code, reason ?: ""); return true }
    override fun cancel() { if (closed) return; closed = true; listener.onFailure(this, java.io.IOException("Canceled"), null) }
    fun serverSays(text: String) = listener.onMessage(this, text)
    fun fail(msg: String) { if (closed) return; closed = true; listener.onFailure(this, java.io.IOException(msg), null) }
}
object FakeNet {
    val sockets = CopyOnWriteArrayList<FakeSocket>()
    /** url prefix -> http handler */
    var http: (Request) -> Response = { Response(it, 503, "") }
    val requests = CopyOnWriteArrayList<Request>()
    fun reset() { sockets.clear(); requests.clear(); http = { Response(it, 503, "") } }
}
open class OkHttpClient(private val authenticator: Authenticator? = null) : Call.Factory {
    override fun newCall(request: Request): Call = object : Call {
        @Volatile var c = false
        override fun execute(): Response {
            FakeNet.requests.add(request)
            val r = FakeNet.http(request)
            if (r.code == 401 && authenticator != null) {
                val retry = authenticator.authenticate(null, r) ?: return r
                FakeNet.requests.add(retry)
                val r2 = FakeNet.http(retry)
                return Response(r2.request, r2.code, r2.body!!.string(), priorResponse = r)
            }
            return r
        }
        override fun cancel() { c = true }
        override fun isCanceled() = c
    }
    fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
        val s = FakeSocket(request.url, listener)
        FakeNet.sockets.add(s)
        return s
    }
    fun newBuilder(): Builder = Builder(authenticator)
    class Builder(private var auth: Authenticator? = null) {
        fun connectionPool(p: ConnectionPool): Builder = this
        fun connectTimeout(timeout: Long, unit: TimeUnit): Builder = this
        fun readTimeout(timeout: Long, unit: TimeUnit): Builder = this
        fun writeTimeout(timeout: Long, unit: TimeUnit): Builder = this
        fun callTimeout(timeout: Long, unit: TimeUnit): Builder = this
        fun pingInterval(interval: Long, unit: TimeUnit): Builder = this
        fun retryOnConnectionFailure(b: Boolean): Builder = this
        fun authenticator(a: Authenticator): Builder { auth = a; return this }
        fun build(): OkHttpClient = OkHttpClient(auth)
    }
}
