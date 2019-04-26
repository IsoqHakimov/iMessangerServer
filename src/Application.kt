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
import com.imes.controllers.AdminController
import com.imes.controllers.ContactsController
import com.imes.controllers.UserController
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.sessions.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.generateNonce
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.event.Level
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
private val server = ChatServer()
@ObsoleteCoroutinesApi
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

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        header(HttpHeaders.XForwardedProto)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.Allow)
        anyHost()
        allowCredentials = true
        maxAge = Duration.ofDays(7)
    }

    install(CallLogging) {
        level = Level.INFO
       // filter { call -> call.request.path().startsWith("/") }
    }

    install(Sessions) {
        cookie<ChatSession>("SESSION")
    }

    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<ChatSession>() == null) {
            call.sessions.set(ChatSession(generateNonce()))
        }
    }

    routing {

        val clients = Collections.synchronizedSet(LinkedHashSet<ChatClient>())

        webSocket("/ws") {

            val uid = call.request.headers["uid"]

            val session = call.sessions.get<ChatSession>()

            if (uid == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }

            server.memberJoin(uid, this)

            try {
                incoming.consumeEach { frame ->

                    if (frame is Frame.Text) {
                        server.message(connection, frame.readText())
                    }
                }
            }
            finally {
                server.memberLeft(uid, this)
            }

        }

        post("api/reg") {

            var user = User()

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

        options("api/admin/dashboard") {
            call.respond(HttpStatusCode.OK)
        }

        get("api/admin/dashboard"){
            call.respond(AdminController.getDashboard(connection))
        }

        options("api/admin/profile") {
            call.respond(HttpStatusCode.OK)
        }

        get("api/admin/profile"){

        }

        options("api/admin/users") {
            call.respond(HttpStatusCode.OK)
        }

        get("api/admin/users"){
            val page = call.parameters["page"]?.toIntOrNull()?:1
            call.respond(AdminController.getUsers(connection, page))
        }

        options("api/admin/spam") {
            call.respond(HttpStatusCode.OK)
        }

        put("api/admin/spam"){
            call.respond(AdminController.editSpam(connection, call.receive()))
        }

        post("api/admin/spam"){
                call.respond(AdminController.addSpam(connection, call.receive()))
        }

        delete("api/admin/spam"){
            call.respond(AdminController.deleteSpam(connection, call.receive()))
        }

        options("api/admin/spams") {
            call.respond(HttpStatusCode.OK)
        }

        get("api/admin/spams"){
            val page = call.parameters["page"]?.toIntOrNull()?:1
            call.respond(AdminController.getSpams(connection, page))
        }

        options("api/admin/notifications") {
            call.respond(HttpStatusCode.OK)
        }

        get("api/admin/notifications"){

        }

        options("api/admin/notification") {
            call.respond(HttpStatusCode.OK)
        }

        post("api/admin/notification"){

        }

        post("api/upload"){
            var format: String?
            var filename: String? = null

            val multipart = call.receiveMultipart()

            multipart.forEachPart { part ->

                if (part is PartData.FileItem) {

                    val name = part.originalFileName!!

                    format = name.substringAfterLast(".")
                    filename = "${System.currentTimeMillis()}.$format"

                    val file = File("/var/www/html/api/temp/images/$filename")

                    part.streamProvider().use { its ->

                        file.outputStream().buffered().use {

                            its.copyTo(it)
                        }
                    }
                }

                part.dispose()

            }

            call.respond(BaseResponse(0, filename, ""))

        }



    }
}

