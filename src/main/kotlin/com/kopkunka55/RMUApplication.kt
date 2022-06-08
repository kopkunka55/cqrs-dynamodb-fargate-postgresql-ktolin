package com.kopkunka55

import com.kopkunka55.infrastructure.QueryRecordRepositoryImpl
import com.kopkunka55.repository.QueryRecordRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun main(){
    val readModelUpdater = ReadModelUpdater()
    readModelUpdater.update()
}

class ReadModelUpdater {
    private val ddbClient = DynamoDbClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(ProfileCredentialsProvider.create())
        .build()

    private val queryRecordRepository: QueryRecordRepository = QueryRecordRepositoryImpl()

    init {
        Database.connect(
            url = "jdbc:postgresql://${System.getenv("DATABASE_ENDPOINT")}/anywallet",
            user = System.getenv("DATABASE_USER"),
            password = System.getenv("DATABASE_PASSWORD"),
            driver = "org.postgresql.Driver"
        )
    }
    fun update() {
        val currentTime = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
        val dtf = DateTimeFormatter.ofPattern("yyyyMMddHH")
        val dtfWithTZ = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        val key = HashMap<String, AttributeValue>()
        key["PK"] = AttributeValue.fromS(currentTime.format(dtf))
        key["SK"] = AttributeValue.fromS("amount")

        val request = GetItemRequest.builder()
            .tableName("anywallet-dev")
            .key(key)
            .build()
        val summaryPerHour = ddbClient.getItem(request)

        if (!summaryPerHour.hasItem()) {
            println("NO ITEM")
            return
        }

        val record = summaryPerHour.item()
        val amount = record["amount"]?.n()?.toFloat()
        println(amount)

        transaction {
            queryRecordRepository.updateRecordPerHour(
                currentTime.withMinute(0).withSecond(0).format(dtfWithTZ),
                amount!!)
        }
    }
}
