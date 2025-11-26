package com.yourcompany.friendlocator.routes

import com.yourcompany.friendlocator.config.CloudinaryConfig
import com.yourcompany.friendlocator.model.ImageUploadResponse
import com.yourcompany.friendlocator.model.PlaceCreateRequest
import com.yourcompany.friendlocator.repository.PlaceRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PlaceRoutes")

fun Route.placeRoutes() {

    route("/places") {

        /**
         * GET /api/places
         * 모든 장소 마커 조회 (지도 표시용)
         */
        get {
            val places = PlaceRepository.getAllPlaceMarkers()
            logger.info("Fetched ${places.size} place markers")
            call.respond(HttpStatusCode.OK, places)
        }

        /**
         * GET /api/places/nearby?lat={lat}&lng={lng}&radius={km}
         * 근처 장소 조회
         */
        get("/nearby") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 10.0

            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lat, lng 파라미터가 필요합니다"))
                return@get
            }

            val places = PlaceRepository.getPlacesNearby(lat, lng, radius)
            logger.info("Fetched ${places.size} nearby places at ($lat, $lng)")
            call.respond(HttpStatusCode.OK, places)
        }

        /**
         * GET /api/places/{id}
         * 장소 상세 조회
         */
        get("/{id}") {
            val placeId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "장소 ID가 필요합니다")
            )

            val place = PlaceRepository.getPlaceById(placeId)
            if (place != null) {
                call.respond(HttpStatusCode.OK, place)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "장소를 찾을 수 없습니다"))
            }
        }

        /**
         * POST /api/places
         * 장소 생성 (사진 없이 먼저 생성)
         */
        post {
            val request = call.receive<PlaceCreateRequest>()
            logger.info("Creating place: ${request.nickname} at (${request.latitude}, ${request.longitude})")

            val place = PlaceRepository.createPlace(
                userId = request.userId,
                nickname = request.nickname,
                latitude = request.latitude,
                longitude = request.longitude,
                memo = request.memo,
                photos = emptyList()
            )

            call.respond(HttpStatusCode.Created, place)
        }

        /**
         * POST /api/places/{id}/photos
         * 장소에 사진 업로드 (multipart form-data)
         */
        post("/{id}/photos") {
            val placeId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ImageUploadResponse(success = false, message = "장소 ID가 필요합니다")
            )

            // 장소 존재 확인
            val place = PlaceRepository.getPlaceById(placeId)
            if (place == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ImageUploadResponse(success = false, message = "장소를 찾을 수 없습니다")
                )
                return@post
            }

            // 사진 개수 제한 확인
            if (place.photos.size >= 5) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ImageUploadResponse(success = false, message = "사진은 최대 5장까지 업로드할 수 있습니다")
                )
                return@post
            }

            val multipart = call.receiveMultipart()
            var uploadedUrl: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val bytes = part.streamProvider().readBytes()
                        if (bytes.isNotEmpty()) {
                            uploadedUrl = CloudinaryConfig.uploadImage(bytes, "places/$placeId")
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (uploadedUrl != null) {
                PlaceRepository.addPhotoToPlace(placeId, uploadedUrl!!)
                logger.info("Photo uploaded for place $placeId: $uploadedUrl")
                call.respond(
                    HttpStatusCode.OK,
                    ImageUploadResponse(success = true, url = uploadedUrl)
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ImageUploadResponse(success = false, message = "이미지 파일이 필요합니다")
                )
            }
        }

        /**
         * DELETE /api/places/{id}?userId={userId}
         * 장소 삭제 (본인만 가능)
         */
        delete("/{id}") {
            val placeId = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "장소 ID가 필요합니다")
            )

            val userId = call.request.queryParameters["userId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "사용자 ID가 필요합니다")
            )

            val deleted = PlaceRepository.deletePlace(placeId, userId)
            if (deleted) {
                logger.info("Place deleted: $placeId by $userId")
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "삭제되었습니다"))
            } else {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "삭제 권한이 없거나 장소를 찾을 수 없습니다"))
            }
        }
    }
}
