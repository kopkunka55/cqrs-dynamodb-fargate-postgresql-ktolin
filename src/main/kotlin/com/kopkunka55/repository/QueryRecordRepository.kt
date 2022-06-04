package com.kopkunka55.repository

import com.kopkunka55.domain.QueryRecord

interface QueryRecordRepository {
    fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<QueryRecord>
}