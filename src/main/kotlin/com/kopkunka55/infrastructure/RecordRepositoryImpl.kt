package com.kopkunka55.infrastructure

import com.kopkunka55.domain.Record
import com.kopkunka55.repository.RecordRepository

class RecordRepositoryImpl(override val tableName: String): RecordRepository {
    override fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<Record> {
        TODO("Not yet implemented")
    }

    override fun saveRecord(requestId: String, datetime: String, amount: Float) {
        TODO("Not yet implemented")
    }

}