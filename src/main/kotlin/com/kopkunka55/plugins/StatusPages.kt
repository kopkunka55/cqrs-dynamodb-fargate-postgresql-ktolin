package com.kopkunka55.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause ->
            when(cause){
                is NotFoundException -> {
                    call.respondText("$cause", status = HttpStatusCode.NotFound)
                }
                is BadRequestException -> {
                    call.respondText("$cause", status = HttpStatusCode.BadRequest)
                }
                else -> {
                    call.respondText("$cause", status = HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}