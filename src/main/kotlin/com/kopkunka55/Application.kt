package com.kopkunka55

import com.kopkunka55.contoller.configureCommandRouter
import com.kopkunka55.contoller.configureQueryRouter
import com.kopkunka55.infrastructure.CommandRecordRepositoryImpl
import com.kopkunka55.infrastructure.QueryRecordRepositoryImpl
import io.ktor.server.application.*
import com.kopkunka55.plugins.*
import com.kopkunka55.repository.CommandRecordRepository
import com.kopkunka55.repository.QueryRecordRepository
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import java.util.TimeZone

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.queryModule() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val logger = LoggerFactory.getLogger("queryModule")
    configureSerialization()
    configureStatusPages()
    configureExposed()
    routing {
        configureQueryRouter()
    }
    install(Koin) {
        modules(
            module {
                factory<QueryRecordRepository> { QueryRecordRepositoryImpl() }
            }
        )
    }
    logger.info("Query Module is running")
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.commandModule() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val logger = LoggerFactory.getLogger("commandModule")
    configureSerialization()
    configureStatusPages()
    routing {
        configureCommandRouter()
    }
    install(Koin) {
        modules(
            module {
                factory<CommandRecordRepository> { CommandRecordRepositoryImpl(environment.config.property("ktor.aws.ddb_table").getString()) }
            }
        )
    }
    logger.info("Command Module is running")
}

fun Application.testModule() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val logger = LoggerFactory.getLogger("testModule")
    configureSerialization()
    configureStatusPages()
    configureExposed()
    routing {
        configureQueryRouter()
        configureCommandRouter()
    }
    install(Koin) {
        modules(
            module {
                factory<CommandRecordRepository> { CommandRecordRepositoryImpl(environment.config.property("ktor.aws.ddb_table").getString()) }
                factory<QueryRecordRepository> { QueryRecordRepositoryImpl() }
            }
        )
    }
    logger.info("Test Module is running")
}
