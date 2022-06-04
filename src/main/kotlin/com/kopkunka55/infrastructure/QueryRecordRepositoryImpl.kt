package com.kopkunka55.infrastructure

import com.kopkunka55.domain.QueryRecord
import com.kopkunka55.repository.QueryRecordRepository
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

object QueryRecordTable: IntIdTable("record"){
    var datetime = datetime("datetime").uniqueIndex()
    val amount = float("amount")
}

class QueryRecordEntity(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<QueryRecordEntity>(QueryRecordTable)
    var datetime by QueryRecordTable.datetime
    var amount by QueryRecordTable.amount
}

class QueryRecordRepositoryImpl: QueryRecordRepository{
    private fun String.toDateTimeAtUTC(): LocalDateTime{
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        return LocalDateTime.parse(this, dtf)
    }
    private fun LocalDateTime.toUTCDateTimeString(): String{
        val datetime = LocalDateTime.parse(this.toString())
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        return datetime.format(dtf) + "+00:00"
    }

    override fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<QueryRecord> {
        val queryRecordEntity = QueryRecordEntity.find {
            (QueryRecordTable.datetime greaterEq  startDateTime.toDateTimeAtUTC()) and
                    (QueryRecordTable.datetime lessEq endDateTime.toDateTimeAtUTC())
        }
        return queryRecordEntity.map { QueryRecord(it.datetime.toUTCDateTimeString(), it.amount) }
    }
}