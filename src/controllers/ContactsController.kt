package com.imes.controllers

import com.github.jasync.sql.db.Connection
import com.imes.BaseResponse
import com.imes.Contact
import com.imes.sendPreparedStatementAwait


object ContactsController {

    suspend fun checkContacts(connection: Connection, contacts: ArrayList<Contact>?, uid: String): BaseResponse<ArrayList<Contact>> {
        val c = if (!connection.isConnected())
            connection.connect().get() else connection

        contacts?.forEach {
            var query = "select * from users where phone = '${it.phone}'"
            c.sendPreparedStatementAwait(query, ArrayList()).rows.let { i ->
                if (i.size > 0) {
                    it.flag = 1
                    it.uid = i[0]["uid"].toString()
                }
            }
            query = "insert ignore into contacts (phone, flag, uid, userId)\n" +
                    "values ( '${it.phone}', ${it.flag}, '${it.uid}', (select id from users where users.uid = '$uid'));"
            c.sendPreparedStatementAwait(query, ArrayList())
        }
        return BaseResponse(0, contacts, "")

    }

}