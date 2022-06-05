package com.kopkunka55

import io.ktor.server.application.*
import com.kopkunka55.plugins.*
import java.util.TimeZone

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    configureSerialization()
    val cqrsMode = environment.config.propertyOrNull("ktor.application.mode")!!.getString() ?: TODO("Error Handling")
    if (cqrsMode == "COMMAND"){
        configureRouting()
        configureDI()
        println("COMMAND MODE")
    } else if (cqrsMode == "QUERY"){
        configureQueryRouting()
        configureQueryDI()
        configureExposed()
        println("QUERY MODE")
    }
}
