package com.kopkunka55.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable

@Serializable
data class Record(val datetime: String, val amount: Float)

@Serializable
data class HistoryRequest(val startDateTime: String, val endDateTime: String)

fun Application.configureRouting() {
    install(StatusPages) {

    }
    routing {
        route("/health-check"){
            get {
                call.respondText("OK")
            }
        }
        route("/wallet"){
            post {
                val request = call.receive<Record>()
                val requestId = call.request.headers["X-Request-Id"].toString()
                call.respond(Record(request.datetime, request.amount))
            }
            post("/search") {
               val req = call.receive<HistoryRequest>()
                call.respond(listOf(
                    Record(req.startDateTime, 0.3.toFloat()) ,
                    Record(req.startDateTime, 0.3.toFloat()) ,
                    Record(req.startDateTime, 0.3.toFloat()) ,
                ))
            }

        }
    }
}
