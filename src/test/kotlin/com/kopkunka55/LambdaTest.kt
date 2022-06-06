package com.kopkunka55

import com.amazonaws.services.lambda.runtime.tests.annotations.Event
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import javax.naming.Context

internal class LambdaTest {
    val context = mockk<com.amazonaws.services.lambda.runtime.Context>()

    @Test
    fun `Success Pattern`(){
        val main = Main()
        main.handleRequest(mapOf(), context)
    }
}