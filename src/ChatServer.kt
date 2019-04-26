package com.imes

import com.github.jasync.sql.db.Connection
import com.google.gson.Gson
import com.imes.controllers.MessageController
import io.ktor.gson.GsonConverter
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class ChatServer {

    val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    val lastMessages = LinkedList<String>()

    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }
    }

    suspend fun memberLeft(member: String, socket: WebSocketSession) {
        val connections = members[member]
        connections?.remove(socket)

    }

    private suspend fun sendTo(baseResponse: String, receiving: String) {
        members[receiving]?.send(Frame.Text(baseResponse))
//      val connection =
//        if(connection?.size!! > 0){
//            connection[0].send(Frame.Text(""))
//        }
    }

    suspend fun message(connection: Connection,baseResponse: String) {
        val response = Gson().fromJson(baseResponse, Response::class.java)
        if (response.code == 0) {
            val message = Gson().fromJson(response.result.toString(), Message::class.java)
            MessageController.setMessage(connection,message)
            sendTo(baseResponse, message.to?:"")
        }
    }

    private suspend fun List<WebSocketSession>.send(frame: Frame) {
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
                } catch (ignore: ClosedSendChannelException) {

                }
            }
        }
    }
}