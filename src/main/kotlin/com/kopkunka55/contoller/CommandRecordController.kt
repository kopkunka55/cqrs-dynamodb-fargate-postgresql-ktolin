package com.kopkunka55.contoller
import com.kopkunka55.repository.CommandRecordRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureCommandRouter() {
    val commandRecordRepository: CommandRecordRepository by inject()
    post {
        val request = call.receive<Record>()
        val requestId = call.request.headers["X-Request-Id"].toString()
        val record = commandRecordRepository.saveRecord(requestId, request.datetime, request.amount)
        call.respond(Record(record.datetime, record.amount))
    }
}
