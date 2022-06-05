package com.kopkunka55

import com.kopkunka55.contoller.configureBaseRouter
import com.kopkunka55.contoller.configureCommandRouter
import com.kopkunka55.contoller.configureQueryRouter
import io.ktor.server.application.*
import com.kopkunka55.plugins.*
import io.ktor.server.routing.*
import java.util.TimeZone

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    configureSerialization()
    configureDI()
    val cqrsMode = environment.config.propertyOrNull("ktor.application.mode")!!.getString() ?: TODO("Error Handling")
    routing {
        configureBaseRouter()
    }
    if (cqrsMode == "COMMAND"){
        routing {
            configureBaseRouter()
            configureCommandRouter()
        }
        println("COMMAND MODE")
    } else if (cqrsMode == "QUERY"){
        configureExposed()
        routing {
            configureBaseRouter()
            configureQueryRouter()
        }
        println("QUERY MODE")
    } else {
        configureExposed()
        routing {
            configureBaseRouter()
            configureCommandRouter()
            configureQueryRouter()
        }
        println("TEST MODE")
    }
}
