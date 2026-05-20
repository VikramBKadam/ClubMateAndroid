package com.clubmates.app.data.network

import android.util.Log
import com.clubmates.app.data.local.TokenStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

sealed class WsEvent {
    data class MessageNew(val matchId: String, val message: ApiMessage) : WsEvent()
    data class MessageAck(val tempId: String, val serverId: String, val createdAt: String) : WsEvent()
    data class MessageRead(val matchId: String, val upToMessageId: String) : WsEvent()
    data class TypingStart(val matchId: String, val userId: String) : WsEvent()
    data class TypingStop(val matchId: String, val userId: String) : WsEvent()
    data class MatchCreated(val match: ApiMatch) : WsEvent()
    data class PresenceJoin(val venueId: String, val userId: String, val name: String?, val photoUrl: String?) : WsEvent()
    data class PresenceLeave(val venueId: String, val userId: String) : WsEvent()
    object Connected : WsEvent()
    object Disconnected : WsEvent()
}

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenStore: TokenStore,
    private val baseUrl: String
) {
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var retryDelayMs = 1_000L

    private val json = Json { ignoreUnknownKeys = true }

    fun connect() {
        if (webSocket != null) return
        val token = tokenStore.accessToken ?: return
        val wsUrl = baseUrl.replace("http", "ws") + "/v1/ws?token=$token"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        retryDelayMs = 1_000L
    }

    fun send(envelope: WsSend) {
        val text = json.encodeToString(WsSend.serializer(), envelope)
        webSocket?.send(text)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            retryDelayMs = 1_000L
            scope.launch { _events.emit(WsEvent.Connected) }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            scope.launch { handleMessage(text) }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.w("WS", "WebSocket failure: ${t.message}")
            webSocket = null
            scope.launch { _events.emit(WsEvent.Disconnected) }
            scheduleReconnect()
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            webSocket = null
            scope.launch { _events.emit(WsEvent.Disconnected) }
            if (code != 1000) scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(retryDelayMs)
            retryDelayMs = min(retryDelayMs * 2, 60_000L)
            connect()
        }
    }

    private suspend fun handleMessage(text: String) {
        val type = runCatching {
            json.decodeFromString(WsEnvelope.serializer(), text).type
        }.getOrNull() ?: return

        val event: WsEvent = when (type) {
            "message.new" -> {
                val msg = json.decodeFromString(WsMessageNew.serializer(), text)
                WsEvent.MessageNew(msg.matchId, msg.message)
            }
            "message.ack" -> {
                val ack = json.decodeFromString(WsMessageAck.serializer(), text)
                WsEvent.MessageAck(ack.tempId, ack.serverId, ack.createdAt)
            }
            "message.read" -> {
                val parsed = json.parseToJsonElement(text)
                val matchId = parsed.toString() // parse properly below
                val envelope = json.decodeFromString(kotlinx.serialization.json.JsonObject.serializer(), text)
                WsEvent.MessageRead(
                    envelope["matchId"].toString().trim('"'),
                    envelope["upToMessageId"].toString().trim('"')
                )
            }
            "typing.start" -> {
                val t = json.decodeFromString(WsTyping.serializer(), text)
                WsEvent.TypingStart(t.matchId, t.userId)
            }
            "typing.stop" -> {
                val t = json.decodeFromString(WsTyping.serializer(), text)
                WsEvent.TypingStop(t.matchId, t.userId)
            }
            "match.created" -> {
                val m = json.decodeFromString(WsMatchCreated.serializer(), text)
                WsEvent.MatchCreated(m.match)
            }
            "presence.join" -> {
                val p = json.decodeFromString(WsPresence.serializer(), text)
                WsEvent.PresenceJoin(p.venueId, p.userId, p.name, p.photoUrl)
            }
            "presence.leave" -> {
                val p = json.decodeFromString(WsPresence.serializer(), text)
                WsEvent.PresenceLeave(p.venueId, p.userId)
            }
            else -> return
        }
        _events.emit(event)
    }
}
