package com.kopkunka55.contoller

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Record(val datetime: String, val amount: Float)

fun Route.configureBaseRouter(){
    get("/health-check") {
        call.respondText("OK")
    }
}