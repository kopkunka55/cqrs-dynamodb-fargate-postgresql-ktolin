package com.kopkunka55.plugins

import com.kopkunka55.infrastructure.RecordRepositoryImpl
import com.kopkunka55.repository.RecordRepository
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDI(){
   install(Koin) {
       modules(
           module {
               factory<RecordRepository> { RecordRepositoryImpl(environment.config.property("ktor.aws.ddb_table").toString()) }
           }
       )
   }
}