package com.kopkunka55.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureRouting() {
    install(StatusPages) {

    }
    routing {
        route("/health-check"){
            get {
                call.respondText("OK")
            }
        }
    }
}
