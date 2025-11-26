package com.yourcompany.friendlocator.routes

import com.yourcompany.friendlocator.config.CloudinaryConfig
import com.yourcompany.friendlocator.model.ImageUploadResponse
import com.yourcompany.friendlocator.model.RestaurantCreateRequest
import com.yourcompany.friendlocator.model.ReviewCreateRequest
import com.yourcompany.friendlocator.repository.RestaurantRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RestaurantRoutes")

fun Route.restaurantRoutes() {

    route("/restaurants") {

        /**
         * GET /api/restaurants
         * 모든 맛집 마커 조회
         */
        get {
            val restaurants = RestaurantRepository.getAllRestaurants()
            logger.info("Fetched ${restaurants.size} restaurants")
            call.respond(HttpStatusCode.OK, restaurants)
        }

        /**
         * GET /api/restaurants/nearby?lat={lat}&lng={lng}&radius={km}&category={category}
         * 주변 맛집 검색
         */
        get("/nearby") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 10.0
            val category = call.request.queryParameters["category"]

            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lat, lng 파라미터가 필요합니다"))
                return@get
            }

            val restaurants = RestaurantRepository.getNearbyRestaurants(lat, lng, radius, category)
            logger.info("Fetched ${restaurants.size} nearby restaurants at ($lat, $lng)")
            call.respond(HttpStatusCode.OK, restaurants)
        }

        /**
         * GET /api/restaurants/search?q={query}
         * 맛집 검색 (이름)
         */
        get("/search") {
            val query = call.request.queryParameters["q"] ?: ""

            if (query.length < 2) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "검색어는 2자 이상이어야 합니다"))
                return@get
            }

            val restaurants = RestaurantRepository.searchByName(query)
            logger.info("Search '$query': found ${restaurants.size} restaurants")
            call.respond(HttpStatusCode.OK, restaurants)
        }

        /**
         * GET /api/restaurants/check-duplicate?lat={lat}&lng={lng}
         * 중복 체크 (반경 50m 내)
         */
        get("/check-duplicate") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()

            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lat, lng 파라미터가 필요합니다"))
                return@get
            }

            val result = RestaurantRepository.checkDuplicates(lat, lng)
            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * GET /api/restaurants/{id}?userId={userId}
         * 맛집 상세 조회
         */
        get("/{id}") {
            val restaurantId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "맛집 ID가 필요합니다")
            )
            val userId = call.request.queryParameters["userId"]

            val restaurant = RestaurantRepository.getRestaurantById(restaurantId, userId)
            if (restaurant != null) {
                call.respond(HttpStatusCode.OK, restaurant)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "맛집을 찾을 수 없습니다"))
            }
        }

        /**
         * POST /api/restaurants
         * 맛집 등록 (+ 첫 리뷰)
         */
        post {
            val request = call.receive<RestaurantCreateRequest>()
            logger.info("Creating restaurant: ${request.name} at (${request.latitude}, ${request.longitude})")

            val restaurant = RestaurantRepository.createRestaurant(request)
            call.respond(HttpStatusCode.Created, restaurant)
        }

        /**
         * POST /api/restaurants/{id}/reviews
         * 리뷰 추가
         */
        post("/{id}/reviews") {
            val restaurantId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "맛집 ID가 필요합니다")
            )

            val request = call.receive<ReviewCreateRequest>()
            val review = RestaurantRepository.addReview(restaurantId, request)

            if (review != null) {
                logger.info("Review added to restaurant $restaurantId by ${request.userId}")
                call.respond(HttpStatusCode.Created, review)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "맛집을 찾을 수 없습니다"))
            }
        }

        /**
         * GET /api/restaurants/{id}/reviews
         * 맛집 리뷰 목록
         */
        get("/{id}/reviews") {
            val restaurantId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "맛집 ID가 필요합니다")
            )

            val restaurant = RestaurantRepository.getRestaurantById(restaurantId)
            if (restaurant != null) {
                call.respond(HttpStatusCode.OK, restaurant.reviews)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "맛집을 찾을 수 없습니다"))
            }
        }

        /**
         * POST /api/restaurants/{id}/bookmark?userId={userId}
         * 북마크 추가
         */
        post("/{id}/bookmark") {
            val restaurantId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "맛집 ID가 필요합니다")
            )
            val userId = call.request.queryParameters["userId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "사용자 ID가 필요합니다")
            )

            val success = RestaurantRepository.addBookmark(userId, restaurantId)
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "북마크 추가됨"))
            } else {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "이미 북마크되어 있습니다"))
            }
        }

        /**
         * DELETE /api/restaurants/{id}/bookmark?userId={userId}
         * 북마크 삭제
         */
        delete("/{id}/bookmark") {
            val restaurantId = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "맛집 ID가 필요합니다")
            )
            val userId = call.request.queryParameters["userId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "사용자 ID가 필요합니다")
            )

            val success = RestaurantRepository.removeBookmark(userId, restaurantId)
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "북마크 삭제됨"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "북마크를 찾을 수 없습니다"))
            }
        }
    }

    route("/reviews") {

        /**
         * DELETE /api/reviews/{id}?userId={userId}
         * 리뷰 삭제 (본인만)
         */
        delete("/{id}") {
            val reviewId = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "리뷰 ID가 필요합니다")
            )
            val userId = call.request.queryParameters["userId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "사용자 ID가 필요합니다")
            )

            val success = RestaurantRepository.deleteReview(reviewId, userId)
            if (success) {
                logger.info("Review $reviewId deleted by $userId")
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "삭제되었습니다"))
            } else {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "삭제 권한이 없거나 리뷰를 찾을 수 없습니다"))
            }
        }

        /**
         * POST /api/reviews/{id}/photos
         * 리뷰에 사진 업로드
         */
        post("/{id}/photos") {
            val reviewId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ImageUploadResponse(success = false, message = "리뷰 ID가 필요합니다")
            )

            val multipart = call.receiveMultipart()
            var uploadedUrl: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val bytes = part.streamProvider().readBytes()
                        if (bytes.isNotEmpty()) {
                            uploadedUrl = CloudinaryConfig.uploadImage(bytes, "reviews/$reviewId")
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (uploadedUrl != null) {
                val success = RestaurantRepository.addPhotoToReview(reviewId, uploadedUrl!!)
                if (success) {
                    logger.info("Photo uploaded for review $reviewId: $uploadedUrl")
                    call.respond(HttpStatusCode.OK, ImageUploadResponse(success = true, url = uploadedUrl))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ImageUploadResponse(success = false, message = "사진은 최대 5장까지 업로드할 수 있습니다"))
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, ImageUploadResponse(success = false, message = "이미지 파일이 필요합니다"))
            }
        }
    }

    route("/users") {

        /**
         * GET /api/users/{userId}/reviews
         * 내 리뷰 목록
         */
        get("/{userId}/reviews") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "사용자 ID가 필요합니다")
            )

            val reviews = RestaurantRepository.getReviewsByUserId(userId)
            logger.info("Fetched ${reviews.size} reviews for user $userId")
            call.respond(HttpStatusCode.OK, reviews)
        }

        /**
         * GET /api/users/{userId}/bookmarks
         * 내 북마크 목록
         */
        get("/{userId}/bookmarks") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "사용자 ID가 필요합니다")
            )

            val bookmarks = RestaurantRepository.getBookmarksByUserId(userId)
            logger.info("Fetched ${bookmarks.size} bookmarks for user $userId")
            call.respond(HttpStatusCode.OK, bookmarks)
        }
    }
}
