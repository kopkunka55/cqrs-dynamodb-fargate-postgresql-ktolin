package com.kopkunka55.infrastructure

import com.kopkunka55.domain.Record
import com.kopkunka55.repository.RecordRepository
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.ZoneOffset
import java.time.ZonedDateTime

class RecordRepositoryImpl(override val tableName: String): RecordRepository {
    private val ddbClient = DynamoDbClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build()

    private fun String.toUTCDateTime(): String{
        return ZonedDateTime.parse(this).withZoneSameInstant(ZoneOffset.UTC).toString()
    }

    override fun saveRecord(requestId: String, datetime: String, amount: Float): Record {
        val dateTimeAtUTC = datetime.toUTCDateTime()
        val itemValues = mutableMapOf<String, AttributeValue>()
        itemValues["request_id"] = AttributeValue.builder().s(requestId).build()
        itemValues["datetime"] = AttributeValue.builder().s(dateTimeAtUTC).build()
        itemValues["amount"] = AttributeValue.builder().n(amount.toString()).build()

        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(itemValues)
            .build()

        ddbClient.putItem(request)
        return Record(dateTimeAtUTC, amount)
    }

}