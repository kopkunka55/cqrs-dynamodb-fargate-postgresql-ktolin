package com.kopkunka55.contoller
import com.kopkunka55.repository.CommandRecordRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class CommandRecord(val datetime: String, val amount: Float)

fun Route.configureCommandRouter() {
    val commandRecordRepository: CommandRecordRepository by inject()
    route("/command"){
        get("/health-check") {
            call.respondText("OK")
        }
        post {
            val request = call.receive<CommandRecord>()
            val requestId = call.request.headers["X-Request-Id"].toString()
            val record = commandRecordRepository.saveRecord(requestId, request.datetime, request.amount)
            call.respond(CommandRecord(record.datetime, record.amount))
        }
    }
}
