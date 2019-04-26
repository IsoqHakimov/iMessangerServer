package com.imes

import com.google.gson.JsonObject


data class BaseResponse<T>(val code: Int, val result: T?, val error: String?)
data class Response(val code: Int, val result: JsonObject?, val error: String?)
data class ChatSession(val id: String)
data class User(
    val id: Long? = null,
    val name: String? = null,
    val surname: String? = null,
    val phone: String? = null,
    val uid: String? = null,
    var image: String? = null,
    var active: Int? = null,
    var date: Long? = null
)

data class UserWrapper(
    val users: ArrayList<User>? = null,
    val count: Long? = null
)

data class Message(
    val id: Long? = null,
    val from: String? = null,
    val to: String? = null,
    val text: String? = null,
    val date: Long? = null
)

data class Contact(
    val id: Long? = null,
    val phone: String? = null,
    val name: String? = null,
    var flag: Int? = null,
    var uid: String? = null,
    val date: Long? = null,
    val image: String? = null
)

data class ContactsWrapper(
    val contacts: ArrayList<Contact>? = null
)

data class UsersStats(
    val time: Long? = null,
    val count: Long? = null
)

data class Spam(
    val id: Long? = null,
    val text: String? = null
)

data class SpamWrapper(
    val spams: ArrayList<Spam>? = null,
    val count: Long? = null
)