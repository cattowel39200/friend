package com.yourcompany.friendlocator

import com.yourcompany.friendlocator.config.DatabaseConfig
import com.yourcompany.friendlocator.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    // 데이터베이스 초기화
    DatabaseConfig.init()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        Netty,
        port = port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureHTTP()
    configureRouting()
}
