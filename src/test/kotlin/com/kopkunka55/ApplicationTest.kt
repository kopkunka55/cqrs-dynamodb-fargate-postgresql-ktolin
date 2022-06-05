package com.kopkunka55

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import com.kopkunka55.plugins.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        client.get("/health-check").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("OK", bodyAsText())
        }
    }

    @Test
    fun saveRecord() = testApplication {
        client.post("/wallet"){
            headers {
                append("X-Request-Id", UUID.randomUUID().toString())
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            val datetime = ZonedDateTime.now()
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            setBody(
                Json.encodeToString( Record(datetime.format(dtf), 20.3.toFloat() ) )
            )
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            println(bodyAsText())
        }
    }

    @Test
    fun getHistoryOfWalletEachHour() = testApplication {
        client.post("/wallet/search") {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            setBody(
                Json.encodeToString(
                    HistoryRequest("2004-10-19T13:00:00+00:00", "2004-10-19T16:00:00+00:00")
                )
            )
        }.
        apply {
            assertEquals(HttpStatusCode.OK, status)
            println(bodyAsText())
        }
    }
}