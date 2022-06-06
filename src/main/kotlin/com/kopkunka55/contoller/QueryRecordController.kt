package com.kopkunka55.contoller

import com.kopkunka55.repository.QueryRecordRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

@Serializable
data class HistoryRequest(val startDateTime: String, val endDateTime: String)

@Serializable
data class QueryRecord(val datetime: String, val amount: Float)

fun Route.configureQueryRouter() {
    val queryRecordRepository: QueryRecordRepository by inject()

    route("/query"){
        get("/health-check") {
            call.respondText("OK")
        }
        post("/search") {
            val req = call.receive<HistoryRequest>()
            val records = transaction {
                queryRecordRepository.getRecordsBetweenDates(
                    req.startDateTime, req.endDateTime
                )
            }
            call.respond(records.map { QueryRecord(it.datetime, it.amount) })
        }
    }

}