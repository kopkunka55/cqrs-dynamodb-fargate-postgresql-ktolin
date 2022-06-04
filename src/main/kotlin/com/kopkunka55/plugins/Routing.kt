package com.kopkunka55.plugins

import com.kopkunka55.repository.QueryRecordRepository
import com.kopkunka55.repository.RecordRepository
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

@Serializable
data class Record(val datetime: String, val amount: Float)

@Serializable
data class HistoryRequest(val startDateTime: String, val endDateTime: String)

fun Application.configureQueryRouting(){
    val queryRecordRepository: QueryRecordRepository by inject()

    routing {
        route("/health-check"){
            get {
                call.respondText("OK")
            }
        }
        route("/wallet"){
            post("/search") {
                val req = call.receive<HistoryRequest>()
                val records = transaction {
                    queryRecordRepository.getRecordsBetweenDates(
                        req.startDateTime, req.endDateTime
                    )
                }
                call.respond(records.map { Record(it.datetime, it.amount) })
            }
        }
    }
}

fun Application.configureRouting() {
    val recordRepository: RecordRepository by inject()

    routing {
        route("/health-check"){
            get {
                call.respondText("OK")
            }
        }
        route("/wallet"){
            post {
                val request = call.receive<Record>()
                val requestId = call.request.headers["X-Request-Id"].toString()
                val record = recordRepository.saveRecord(requestId, request.datetime, request.amount)
                call.respond(Record(record.datetime, record.amount))
            }
        }
    }
}
