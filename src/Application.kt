package com.imes

import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.gson.*
import java.util.concurrent.TimeUnit
import com.google.firebase.FirebaseApp
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseOptions
import com.google.gson.Gson
import com.imes.controllers.ContactsController
import com.imes.controllers.MessageController
import com.imes.controllers.UserController
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    val gson = Gson()

    val connection = MySQLConnectionBuilder.createConnectionPool {
        username = "root"
        host = "localhost"
        port = 3306
        password = "samuray"
        database = "iMes"
        maxActiveConnections = 100
        maxIdleTime = TimeUnit.MINUTES.toMillis(15)
        maxPendingQueries = 10_000
        connectionValidationInterval = TimeUnit.SECONDS.toMillis(30)
    }
    connection.connect().get()

    val serviceAccount = FileInputStream("imes-adminsdk.json")

    val options = FirebaseOptions.Builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl("https://imes-34abb.firebaseio.com")
        .build()

    FirebaseApp.initializeApp(options)

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(PartialContent) {
        maxRangeCount = 8
    }

    install(ContentNegotiation) {
        gson {

        }
    }

    routing {


        get("test"){
            call.respond(Message(text = "salom \"  \"\" "+"salom \"  \"\" ".replace('"','\"')+ "salom \"  \"\" ".replace("\"","\\\"")))
        }

        val clients = Collections.synchronizedSet(LinkedHashSet<ChatClient>())

        webSocket("/ws") {

            val uid = call.request.headers["uid"]
            val old = clients.filter {
                it.uid == uid
            }
            old.forEach {
                it.session.close()
            }
            clients.removeAll {
                it.uid == uid
            }

            val client = ChatClient(this, uid)
            clients += client

            try {
                while (true) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {

                        val baseResponse = gson.fromJson(frame.readText(), BaseResponse::class.java)

                        if(baseResponse.code == 0) {
                         val message = baseResponse.result as Message
                            try {
                                MessageController.setMessage(connection,message)
                                val a = clients.filter {
                                    it.uid.equals(message.to)
                                }
                                if (a.isNotEmpty()) {
                                    a[0].session.outgoing.send(Frame.Text(gson.toJson(message)))
                                }
                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("onClose ${closeReason.await()}")
            } catch (e: Throwable) {
                println("onError ${closeReason.await()}")
                e.printStackTrace()
            } finally {
                clients -= client
            }

        }

        post("api/reg") {

            var user = User()
            //val gson = Gson()

            var format: String?
            var filename: String? = ""

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->

                if (part is PartData.FileItem) {

                    val name = part.originalFileName!!

                    format = name.substringAfterLast(".")
                    filename = "${System.currentTimeMillis()}.$format"

                    val file = File("/var/www/html/imes/images/$filename")

                    part.streamProvider().use { its ->

                        file.outputStream().buffered().use {

                            its.copyTo(it)
                        }
                    }
                }
                if (part is PartData.FormItem) {
                    user = gson.fromJson(part.value, User::class.java)
                }
                part.dispose()
            }
            user.image = filename

            call.respond(UserController.setUser(connection, user))

        }

        get("api/auth") {
            val uid = call.request.headers["uid"]
            if(uid == null) call.respond(BaseResponse(1, null, "invalid params"))
            else call.respond(UserController.getUser(connection, uid))
        }

        post("api/contacts") {
            val contacts = call.receive<ContactsWrapper>()
            val uid = call.request.headers["uid"]
            if(uid == null) call.respond(BaseResponse(1, null, "invalid params"))
            else call.respond(ContactsController.checkContacts(connection, contacts.contacts, uid))
        }

    }
}

