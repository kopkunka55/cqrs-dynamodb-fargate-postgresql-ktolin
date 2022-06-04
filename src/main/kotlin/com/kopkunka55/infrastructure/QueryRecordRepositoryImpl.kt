package com.kopkunka55.infrastructure

import com.kopkunka55.domain.QueryRecord
import com.kopkunka55.repository.QueryRecordRepository
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.CustomDateTimeFunction
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object QueryRecordTable: IntIdTable("record"){
    val datetime = datetime("datetime")
    val amount = float("amount")
}

class QueryRecordEntity(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<QueryRecordEntity>(QueryRecordTable)
    var datetime by QueryRecordTable.datetime
    var amount by QueryRecordTable.amount
}

class QueryRecordRepositoryImpl: QueryRecordRepository{
    private fun String.toDateTimeAtUTC(): DateTime{
        return DateTime(this).withZone(DateTimeZone.UTC)
    }
    private fun DateTime.formatWithTimeZone(): String{
        val datetime = ZonedDateTime.parse(this.toString())
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ssXXX")
        return datetime.format(dtf)
    }
    override fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<QueryRecord> {
        val queryRecordEntity = QueryRecordEntity.find {
            (QueryRecordTable.datetime greaterEq startDateTime.toDateTimeAtUTC()) and
                    (QueryRecordTable.datetime lessEq endDateTime.toDateTimeAtUTC())
        }
        return queryRecordEntity.map { QueryRecord(it.datetime.formatWithTimeZone(), it.amount) }
    }
}