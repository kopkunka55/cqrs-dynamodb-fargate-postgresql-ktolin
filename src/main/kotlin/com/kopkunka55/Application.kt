package com.kopkunka55

import com.kopkunka55.contoller.configureCommandRouter
import com.kopkunka55.contoller.configureQueryRouter
import io.ktor.server.application.*
import com.kopkunka55.plugins.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.TimeZone

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    configureSerialization()
    configureDI()
    configureStatusPages()

    val logger = LoggerFactory.getLogger("Application")

    when (environment.config.property("ktor.application.mode").getString()) {
        "COMMAND" -> {
            routing {
                configureCommandRouter()
            }
            logger.info("Application is running with COMMAND MODE")
        }
        "QUERY" -> {
            configureExposed()
            routing {
                configureQueryRouter()
            }
            logger.info("Application is running with QUERY MODE")
        }
        else -> {
            configureExposed()
            routing {
                configureCommandRouter()
                configureQueryRouter()
            }
            logger.info("Application is running with TEST MODE")
        }
    }
}
