package com.kopkunka55.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureExposed(){
    Database.connect(
        url = "jdbc:postgresql://${environment.config.property("ktor.database.url").getString()}/anywallet",
        user = environment.config.property("ktor.database.user").getString(),
        password = environment.config.property("ktor.database.password").getString(),
        driver = "org.postgresql.Driver"
    )
}