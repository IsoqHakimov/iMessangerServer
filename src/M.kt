package com.imes


data class BaseResponse<T>(val code: Int, val result: T?, val error: String?)
data class Response(val code: Int, val result: Any?, val error: String?)
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