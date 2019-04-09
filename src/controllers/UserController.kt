package com.imes.controllers

import com.github.jasync.sql.db.Connection
import com.imes.BaseResponse
import com.imes.User
import com.imes.sendPreparedStatementAwait

object UserController {

    suspend fun setUser(connection: Connection, user: User): BaseResponse<User> {

        val c = if (!connection.isConnected())
            connection.connect().get() else connection

        val query = "insert into users (name, surname, phone, uid, image, active)\n" +
                "values (\"${user.name}\",\"${user.surname}\",'${user.phone}','${user.uid}','${user.image}',1);"

        c.sendPreparedStatementAwait(query, ArrayList())

        return BaseResponse(0, user, "")

    }

    suspend fun getUser(connection: Connection, uid: String): BaseResponse<User>  {

        val c = if (!connection.isConnected())
            connection.connect().get() else connection

        val query = "select *\n" +
                "                from users where uid = '$uid';"

        c.sendPreparedStatementAwait(query, ArrayList()).rows.let {
            if(it.size > 0) {
                val user = User(
                    id = it[0]["id"].toString().toLongOrNull(),
                    name = it[0]["name"].toString(),
                    surname = it[0]["surname"].toString(),
                    phone = it[0]["phone"].toString(),
                    image = it[0]["image"].toString(),
                    uid = it[0]["uid"].toString(),
                    active = it[0]["active"].toString().toIntOrNull(),
                    date = it[0]["date"].toString().toLongOrNull()
                )

                return BaseResponse(0, user, "")
            }
           else{
                    return BaseResponse(2, null, "user not found")
            }
        }

    }



}