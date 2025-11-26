package com.yourcompany.friendlocator.routes

import com.yourcompany.friendlocator.model.*
import com.yourcompany.friendlocator.repository.LocationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RoomRoutes")

fun Route.roomRoutes() {

    route("/rooms") {

        /**
         * POST /api/rooms/join
         * 방 참여 요청
         */
        post("/join") {
            val request = call.receive<RoomJoinRequest>()
            logger.info("Room join request: roomCode=${request.roomCode}, userId=${request.userId}, nickname=${request.nickname}")

            val roomId = LocationRepository.joinOrCreateRoom(request.roomCode)

            call.respond(
                HttpStatusCode.OK,
                RoomJoinResponse(
                    success = true,
                    roomId = roomId,
                    message = "방에 참여했습니다"
                )
            )
        }

        /**
         * POST /api/rooms/{roomId}/locations
         * 내 위치 전송
         */
        post("/{roomId}/locations") {
            val roomId = call.parameters["roomId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "roomId가 필요합니다")
            )

            val request = call.receive<LocationRequest>()
            logger.info("Location update: roomId=$roomId, userId=${request.userId}, lat=${request.lat}, lng=${request.lng}")

            val location = LocationResponse(
                userId = request.userId,
                nickname = request.nickname,
                lat = request.lat,
                lng = request.lng,
                accuracy = request.accuracy,
                timestamp = request.timestamp
            )

            LocationRepository.updateLocation(roomId, location)

            call.respond(
                HttpStatusCode.OK,
                mapOf("success" to true, "message" to "위치가 업데이트되었습니다")
            )
        }

        /**
         * GET /api/rooms/{roomId}/locations?userId={userId}
         * 친구 위치 조회 (본인 제외)
         */
        get("/{roomId}/locations") {
            val roomId = call.parameters["roomId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "roomId가 필요합니다")
            )

            val userId = call.request.queryParameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "userId가 필요합니다")
            )

            val friendsLocations = LocationRepository.getFriendsLocations(roomId, userId)
            logger.debug("Friends locations request: roomId=$roomId, userId=$userId, found=${friendsLocations.size}")

            call.respond(HttpStatusCode.OK, friendsLocations)
        }

        /**
         * DELETE /api/rooms/{roomId}/locations/{userId}
         * 위치 공유 중단 (사용자 위치 삭제)
         */
        delete("/{roomId}/locations/{userId}") {
            val roomId = call.parameters["roomId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "roomId가 필요합니다")
            )

            val userId = call.parameters["userId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("success" to false, "message" to "userId가 필요합니다")
            )

            logger.info("Location sharing stopped: roomId=$roomId, userId=$userId")
            LocationRepository.removeUserLocation(roomId, userId)

            call.respond(
                HttpStatusCode.OK,
                mapOf("success" to true, "message" to "위치 공유가 중단되었습니다")
            )
        }
    }
}
