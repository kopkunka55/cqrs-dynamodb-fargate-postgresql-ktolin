package com.kopkunka55.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception { call: ApplicationCall, cause: NotFoundException ->
            call.respondText("$cause", status = HttpStatusCode.NotFound)
        }
        exception { call: ApplicationCall, cause: BadRequestException ->
            call.respondText("$cause", status = HttpStatusCode.BadRequest)
        }
        exception { call: ApplicationCall, cause: Throwable ->
            call.respondText("$cause", status = HttpStatusCode.InternalServerError)
        }
    }
}