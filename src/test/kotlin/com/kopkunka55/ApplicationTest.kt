package com.kopkunka55

import com.kopkunka55.contoller.CommandRecord
import com.kopkunka55.contoller.HistoryRequest
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ApplicationTest {
    @Test
    fun commandHealthCheck() = testApplication  {
        client.get("/command/health-check").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("OK", bodyAsText())
        }
    }

    @Test
    fun queryHealthCheck() = testApplication  {
        client.get("/query/health-check").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("OK", bodyAsText())
        }
    }

    @Test
    fun saveRecord() = testApplication {
        client.post("/command"){
            headers {
                append("X-Request-Id", UUID.randomUUID().toString())
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            val datetime = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            setBody(
                Json.encodeToString( CommandRecord(datetime.format(dtf), 20.3.toFloat() ) )
            )
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            println(bodyAsText())
        }
    }

    @Test
    fun getHistoryOfWalletEachHour() = testApplication {
        client.post("/query/search") {
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