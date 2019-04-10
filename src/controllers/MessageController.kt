package com.imes.controllers

import com.github.jasync.sql.db.Connection
import com.imes.Message
import com.imes.sendPreparedStatementAwait

object MessageController {

    suspend fun setMessage(connection: Connection,message: Message){
        val c = if (!connection.isConnected())
            connection.connect().get() else connection
        val query = "insert into message (`from`, `to`, text, date, status)\n" +
                "values ('${message.from}', '${message.to}', ?, 1);"

        c.sendPreparedStatementAwait(query, arrayListOf(message.text))

    }


}