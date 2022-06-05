package com.kopkunka55.infrastructure

import com.kopkunka55.domain.CommandRecord
import com.kopkunka55.repository.CommandRecordRepository
import kotlinx.serialization.builtins.serializer
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CommandRecordRepositoryImpl(override val tableName: String): CommandRecordRepository {
    private val ddbClient = DynamoDbClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build()

    private fun String.toUTCDateTime(): ZonedDateTime{
        return ZonedDateTime.parse(this).withZoneSameInstant(ZoneOffset.UTC)
    }

    override fun saveRecord(requestId: String, datetime: String, amount: Float): CommandRecord {
        val dateTimeAtUTC = datetime.toUTCDateTime()
        val dtf = DateTimeFormatter.ofPattern("yyyyMMddHH")

        // Optimistic locking
        // Check if record in the certain hour range is exist

        // Partition Key (name: PK): datetime(yyyyMMddHH)
        // Sort Key (name: SK): "amount" -> Not number of amount itself, but the string
        // This is a key to get head item including only sum and version
        val key = HashMap<String, AttributeValue>()
        val pk = dateTimeAtUTC.format(dtf)
        key["PK"] = AttributeValue.fromS(pk)
        key["SK"] = AttributeValue.fromS("amount")
        val getItemRequest = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()
        val getItemResult = ddbClient.getItem(getItemRequest)

        // ArrayList for actions which is going to be executed within TransactionWriteItems API
        var actions = arrayListOf<TransactWriteItem>()

        // If record in the certain hour range is exist already
        if (getItemResult.hasItem()){
            val beforeVersion = getItemResult.item()["version"]
            val one = AttributeValue.fromN("1")
            val updateSum = Update.builder()
                .tableName(tableName)
                    // Optimistic locking to avoid update conflict
                .conditionExpression("#version = :beforeVersion")
                    // if the version is not changed after previous GetItem step, finally able to update
                .updateExpression("SET #amount = #amount + :amount, #version = #version + :one")
                .key(key)
                .expressionAttributeNames(
                    mapOf<String, String>(
                        "#amount" to "amount",
                        "#version" to "version"
                    )
                )
                .expressionAttributeValues(
                    mapOf<String, AttributeValue>(
                        ":amount" to AttributeValue.fromN(amount.toString()),
                        ":beforeVersion" to beforeVersion!!,
                        ":one" to one
                    )
                ).build()
            actions.add(TransactWriteItem.builder().update(updateSum).build())
        } else {
            // If there is no record in given time range (yyyyMMddHH)
            val headItemsValue = mutableMapOf<String, AttributeValue>()
            headItemsValue["PK"] = AttributeValue.fromS(pk)
            headItemsValue["SK"] = AttributeValue.fromS("amount")
            headItemsValue["amount"] = AttributeValue.fromN(amount.toString())
            headItemsValue["version"] = AttributeValue.builder().n("1").build()
            val createHeadItem = Put.builder()
                .tableName(tableName)
                .item(headItemsValue)
                .build()
            actions.add(TransactWriteItem.builder().put(createHeadItem).build())
        }

        // Whether record exists in given time range or not, event record should be just inserted
        val itemValues = mutableMapOf<String, AttributeValue>()
        itemValues["PK"] = AttributeValue.fromS(pk)
        itemValues["SK"] = AttributeValue.fromS(dateTimeAtUTC.format(dtf))
        itemValues["amount"] = AttributeValue.fromN(amount.toString())

        val createRecord = Put.builder().tableName(tableName).item(itemValues).build()
        actions.add(TransactWriteItem.builder().put(createRecord).build())

        val transactionWriteRequest = TransactWriteItemsRequest.builder()
            .transactItems(actions)
            .build()

        ddbClient.transactWriteItems(transactionWriteRequest)
        return CommandRecord(dateTimeAtUTC.toString(), amount)
    }

    override fun getRecord(datetime: String): CommandRecord {
        TODO("Not yet implemented")
    }

}