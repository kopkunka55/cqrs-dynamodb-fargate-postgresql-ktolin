package com.kopkunka55.repository

import com.kopkunka55.domain.CommandRecord

interface CommandRecordRepository {
    val tableName: String
    fun saveRecord(requestId: String, datetime: String, amount: Float): CommandRecord
    fun getRecord(datetime: String): CommandRecord
}