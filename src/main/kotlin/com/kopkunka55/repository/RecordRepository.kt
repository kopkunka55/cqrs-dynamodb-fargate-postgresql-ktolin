package com.kopkunka55.repository

import com.kopkunka55.domain.Record

interface RecordRepository {
    val tableName: String
    fun saveRecord(requestId: String, datetime: String, amount: Float): Record
    fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<Record>
}