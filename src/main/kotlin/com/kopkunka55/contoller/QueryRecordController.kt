package com.kopkunka55.contoller

import com.kopkunka55.repository.QueryRecordRepository
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class HistoryRequest(val startDateTime: String, val endDateTime: String)

@Serializable
data class QueryRecord(val datetime: String, val amount: Float)

fun Route.configureQueryRouter() {
    val logger = LoggerFactory.getLogger("QueryController")
    val queryRecordRepository: QueryRecordRepository by inject()

        post("/search"){
            val req = try { call.receive<HistoryRequest>() } catch (e: Exception){ throw BadRequestException("Request Data format is wrong") }
            if (req.startDateTime == null || req.endDateTime == null){
                logger.error("Missing required input attribute")
                throw BadRequestException("Request should include `startDatetime` and `endDatetime` at body parameter")
            }
            try {
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
                LocalDateTime.parse(req.startDateTime, dtf)
                LocalDateTime.parse(req.endDateTime, dtf)
            } catch (e: Exception){
                logger.info("Wrong datetime format")
                throw  BadRequestException("datetime format should be yyyy-MM-ddTHH:mm:ssXXX")
            }
            val records = transaction {
                queryRecordRepository.getRecordsBetweenDates(
                    req.startDateTime, req.endDateTime
                )
            }
            call.respond(records.map { QueryRecord(it.datetime, it.amount) })
        }

}