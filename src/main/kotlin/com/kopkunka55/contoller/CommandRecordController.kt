package com.kopkunka55.contoller
import com.kopkunka55.repository.CommandRecordRepository
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class CommandRecord(val datetime: String, val amount: Float)

fun Route.configureCommandRouter() {
    val logger = LoggerFactory.getLogger("CommandController")
    val commandRecordRepository: CommandRecordRepository by inject()
    route("/"){
        get("/health-check"){
           call.respondText("OK\n")
        }
        post {
            val request = try {call.receive<CommandRecord>()} catch (e: Exception) {throw BadRequestException("Input data format is wrong")}
            if (request.datetime == null || request.amount ==null){
                logger.error("Missing request attribute")
                throw  BadRequestException("Input data should include `datetime` and `amount`")
            }
            try {
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                LocalDateTime.parse(request.datetime, dtf)
            } catch (e: Exception){
                logger.error("Wrong datetime format")
                throw  BadRequestException("datetime format should be yyyy-MM-ddTHH:mm:ssXXX")
            }
            try {
                val requestId = call.request.headers["X-Amzn-Trace-Id"].toString()
                val record = commandRecordRepository.saveRecord(requestId, request.datetime, request.amount)
                call.respond(CommandRecord(record.datetime, record.amount))
            } catch (e: Exception){
                throw Throwable("Internal Server Error")
            }
        }
    }
}
