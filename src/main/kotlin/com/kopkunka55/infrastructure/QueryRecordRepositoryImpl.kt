package com.kopkunka55.infrastructure

import com.kopkunka55.domain.QueryRecord
import com.kopkunka55.repository.QueryRecordRepository

class QueryRecordRepositoryImpl: QueryRecordRepository{
    override fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<QueryRecord> {
        TODO("Not yet implemented")
    }
}