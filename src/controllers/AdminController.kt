package com.imes.controllers

import com.github.jasync.sql.db.Connection
import com.imes.*
import java.util.*
import kotlin.collections.ArrayList

object AdminController {

    suspend fun getDashboard(connection: Connection): BaseResponse<Any> {

        val c = if (!connection.isConnected())
            connection.connect().get() else connection

        val cal = Calendar.getInstance()

        var current = cal.timeInMillis
        val month = ArrayList<Long>()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
        month.add(cal.timeInMillis - cal.timeInMillis % 86400000)
        for (i in 0..10) {
            cal.add(Calendar.MONTH, -1)
            month.add(cal.timeInMillis - cal.timeInMillis % 86400000)
        }

        val usersStats = ArrayList<UsersStats>()

        month.forEach { time ->
            val query = "select count(id) as count from users where regDate between $time and $current"
            c.sendPreparedStatementAwait(query, ArrayList()).rows.let {
                if (it.size > 0)
                    usersStats.add(UsersStats(time = time, count = it[0]["count"].toString().toLongOrNull() ?: 0L))
                else {
                    usersStats.add(UsersStats(time = time, count = 0L))
                }

            }
            current = time
        }
        return BaseResponse(0, usersStats, "")
        //  val query = "select count(id) from users where regDate between  and "
    }

    suspend fun getUsers(connection: Connection, page: Int): BaseResponse<Any> {

        val c = if (!connection.isConnected())
            connection.connect().get() else connection

        val users = ArrayList<User>()

            var query = "select * from users order by id desc  limit ${(page - 1) * 10},10"
            c.sendPreparedStatementAwait(query, ArrayList()).rows.forEach {
               val user = User(
                   id = it["id"].toString().toLong(),
                   name = it["name"].toString(),
                   image = it["image"].toString(),
                   phone = it["phone"].toString()
               )
                users.add(user)
            }

        query =
            "select count(id) as count from users"
        var count: Long? = 0L
        c.sendPreparedStatementAwait(query, ArrayList()).rows.let {
            if (it.size > 0)
                count = it[0]["count"].toString().toLongOrNull()
        }
        return BaseResponse(0, UserWrapper(users, count), "")
        //  val query = "select count(id) from users where regDate between  and "
    }

    suspend fun getSpams(connection: Connection, page: Int): BaseResponse<Any> {

        val c = if (!connection.isConnected())
            connection.connect().get() else connection

        val spams = ArrayList<Spam>()

        var query = "select * from spams order by id desc  limit ${(page - 1) * 10},10"
        c.sendPreparedStatementAwait(query, ArrayList()).rows.forEach {
            val spam = Spam(
                id = it["id"].toString().toLong(),
                text = it["text"].toString()
            )
            spams.add(spam)
        }

        query =
            "select count(id) as count from spams"
        var count: Long? = 0L
        c.sendPreparedStatementAwait(query, ArrayList()).rows.let {
            if (it.size > 0)
                count = it[0]["count"].toString().toLongOrNull()
        }
        return BaseResponse(0, SpamWrapper(spams, count), "")
        //  val query = "select count(id) from users where regDate between  and "
    }

    suspend fun addSpam(connection: Connection, spam: Spam?): BaseResponse<Any>{

        if(spam == null) return BaseResponse(1, null, "invalid params")
        val c = if (!connection.isConnected())
            connection.connect().get() else connection
        val query = "insert into spams (text)\n" +
                "values (?);"
        c.sendPreparedStatementAwait(query, arrayListOf(spam.text))
        return BaseResponse(0, null, "")
    }

    suspend fun editSpam(connection: Connection, spam: Spam?): BaseResponse<Any>{

        if(spam == null) return BaseResponse(1, null, "invalid params")
        val c = if (!connection.isConnected())
            connection.connect().get() else connection
        val query = "update spams\n" +
                "set text = ?\n" +
                "where id = ${spam.id};"
        c.sendPreparedStatementAwait(query, arrayListOf(spam.text))
        return BaseResponse(0, null, "")
    }

    suspend fun deleteSpam(connection: Connection, spam: Spam?): BaseResponse<Any>{

        if(spam == null) return BaseResponse(1, null, "invalid params")
        val c = if (!connection.isConnected())
            connection.connect().get() else connection
        val query = "delete from spams where id = ${spam.id}"
        c.sendPreparedStatementAwait(query, ArrayList())
        return BaseResponse(0, null, "")
    }

}