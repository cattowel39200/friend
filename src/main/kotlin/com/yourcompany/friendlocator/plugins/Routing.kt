package com.yourcompany.friendlocator.plugins

import com.yourcompany.friendlocator.routes.placeRoutes
import com.yourcompany.friendlocator.routes.restaurantRoutes
import com.yourcompany.friendlocator.routes.roomRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Health check
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "running",
                    "service" to "Friend Locator API",
                    "version" to "1.0.0"
                )
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }

        // API routes
        route("/api") {
            roomRoutes()
            placeRoutes()
            restaurantRoutes()
        }
    }
}
