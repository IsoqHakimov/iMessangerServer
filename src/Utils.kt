package com.imes

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import io.ktor.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.future.await
import java.util.concurrent.atomic.AtomicInteger

suspend fun Connection.sendPreparedStatementAwait(query: String, values: ArrayList<String?>): QueryResult =
    sendPreparedStatement(query, values).await()

class ChatClient(val session: DefaultWebSocketServerSession, UID: String?) {
    companion object {
        var lastId = AtomicInteger(0)
    }

    val id = lastId.getAndIncrement()
    var uid = UID
}