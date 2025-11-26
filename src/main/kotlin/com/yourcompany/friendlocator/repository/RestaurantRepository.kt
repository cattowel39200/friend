package com.yourcompany.friendlocator.repository

import com.yourcompany.friendlocator.database.Bookmarks
import com.yourcompany.friendlocator.database.Restaurants
import com.yourcompany.friendlocator.database.Reviews
import com.yourcompany.friendlocator.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object RestaurantRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * 맛집 등록 (+ 첫 리뷰)
     */
    fun createRestaurant(request: RestaurantCreateRequest): RestaurantDetailResponse {
        val restaurantId = UUID.randomUUID().toString()
        val reviewId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        transaction {
            // 맛집 등록
            Restaurants.insert {
                it[id] = restaurantId
                it[name] = request.name
                it[address] = request.address
                it[latitude] = request.latitude
                it[longitude] = request.longitude
                it[floor] = request.floor
                it[detailLocation] = request.detailLocation
                it[category] = request.category
                it[priceRange] = request.priceRange
                it[averageRating] = request.rating.toDouble()
                it[reviewCount] = 1
                it[createdAt] = now
                it[createdBy] = request.createdBy
            }

            // 첫 리뷰 등록
            Reviews.insert {
                it[id] = reviewId
                it[Reviews.restaurantId] = restaurantId
                it[userId] = request.createdBy
                it[rating] = request.rating
                it[memo] = request.memo
                it[photos] = "[]"
                it[tags] = json.encodeToString(request.tags)
                it[visitDate] = request.visitDate
                it[createdAt] = now
            }
        }

        return RestaurantDetailResponse(
            id = restaurantId,
            name = request.name,
            address = request.address,
            latitude = request.latitude,
            longitude = request.longitude,
            floor = request.floor,
            detailLocation = request.detailLocation,
            category = request.category,
            priceRange = request.priceRange,
            averageRating = request.rating.toDouble(),
            reviewCount = 1,
            createdAt = now.format(dateFormatter),
            createdBy = request.createdBy
        )
    }

    /**
     * 모든 맛집 마커 조회
     */
    fun getAllRestaurants(): List<RestaurantMarkerResponse> {
        return transaction {
            Restaurants.selectAll()
                .orderBy(Restaurants.createdAt, SortOrder.DESC)
                .map { row ->
                    RestaurantMarkerResponse(
                        id = row[Restaurants.id],
                        name = row[Restaurants.name],
                        latitude = row[Restaurants.latitude],
                        longitude = row[Restaurants.longitude],
                        floor = row[Restaurants.floor],
                        category = row[Restaurants.category],
                        priceRange = row[Restaurants.priceRange],
                        averageRating = row[Restaurants.averageRating],
                        reviewCount = row[Restaurants.reviewCount]
                    )
                }
        }
    }

    /**
     * 맛집 상세 조회
     */
    fun getRestaurantById(restaurantId: String, userId: String? = null): RestaurantDetailResponse? {
        return transaction {
            val restaurant = Restaurants.selectAll()
                .where { Restaurants.id eq restaurantId }
                .singleOrNull() ?: return@transaction null

            val reviews = Reviews.selectAll()
                .where { Reviews.restaurantId eq restaurantId }
                .orderBy(Reviews.createdAt, SortOrder.DESC)
                .map { row ->
                    ReviewResponse(
                        id = row[Reviews.id],
                        restaurantId = row[Reviews.restaurantId],
                        userId = row[Reviews.userId],
                        rating = row[Reviews.rating],
                        memo = row[Reviews.memo],
                        photos = try { json.decodeFromString(row[Reviews.photos]) } catch (e: Exception) { emptyList() },
                        tags = try { json.decodeFromString(row[Reviews.tags]) } catch (e: Exception) { emptyList() },
                        visitDate = row[Reviews.visitDate],
                        createdAt = row[Reviews.createdAt].format(dateFormatter)
                    )
                }

            val isBookmarked = if (userId != null) {
                Bookmarks.selectAll()
                    .where { (Bookmarks.userId eq userId) and (Bookmarks.restaurantId eq restaurantId) }
                    .count() > 0
            } else false

            RestaurantDetailResponse(
                id = restaurant[Restaurants.id],
                name = restaurant[Restaurants.name],
                address = restaurant[Restaurants.address],
                latitude = restaurant[Restaurants.latitude],
                longitude = restaurant[Restaurants.longitude],
                floor = restaurant[Restaurants.floor],
                detailLocation = restaurant[Restaurants.detailLocation],
                category = restaurant[Restaurants.category],
                priceRange = restaurant[Restaurants.priceRange],
                averageRating = restaurant[Restaurants.averageRating],
                reviewCount = restaurant[Restaurants.reviewCount],
                createdAt = restaurant[Restaurants.createdAt].format(dateFormatter),
                createdBy = restaurant[Restaurants.createdBy],
                reviews = reviews,
                isBookmarked = isBookmarked
            )
        }
    }

    /**
     * 주변 맛집 검색
     */
    fun getNearbyRestaurants(lat: Double, lng: Double, radiusKm: Double, category: String? = null): List<RestaurantMarkerResponse> {
        return transaction {
            val latRange = radiusKm / 111.0
            val lngRange = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(lat)))

            var query = Restaurants.selectAll().where {
                (Restaurants.latitude greaterEq (lat - latRange)) and
                (Restaurants.latitude lessEq (lat + latRange)) and
                (Restaurants.longitude greaterEq (lng - lngRange)) and
                (Restaurants.longitude lessEq (lng + lngRange))
            }

            if (category != null) {
                query = Restaurants.selectAll().where {
                    (Restaurants.latitude greaterEq (lat - latRange)) and
                    (Restaurants.latitude lessEq (lat + latRange)) and
                    (Restaurants.longitude greaterEq (lng - lngRange)) and
                    (Restaurants.longitude lessEq (lng + lngRange)) and
                    (Restaurants.category eq category)
                }
            }

            query.map { row ->
                RestaurantMarkerResponse(
                    id = row[Restaurants.id],
                    name = row[Restaurants.name],
                    latitude = row[Restaurants.latitude],
                    longitude = row[Restaurants.longitude],
                    floor = row[Restaurants.floor],
                    category = row[Restaurants.category],
                    priceRange = row[Restaurants.priceRange],
                    averageRating = row[Restaurants.averageRating],
                    reviewCount = row[Restaurants.reviewCount]
                )
            }
        }
    }

    /**
     * 중복 체크 (반경 내 유사 맛집)
     */
    fun checkDuplicates(lat: Double, lng: Double, radiusM: Double = 50.0): DuplicateCheckResponse {
        val radiusKm = radiusM / 1000.0
        val nearby = getNearbyRestaurants(lat, lng, radiusKm)
        return DuplicateCheckResponse(
            hasDuplicates = nearby.isNotEmpty(),
            restaurants = nearby
        )
    }

    /**
     * 맛집 검색 (이름)
     */
    fun searchByName(query: String): List<RestaurantMarkerResponse> {
        return transaction {
            Restaurants.selectAll()
                .where { Restaurants.name like "%$query%" }
                .orderBy(Restaurants.averageRating, SortOrder.DESC)
                .map { row ->
                    RestaurantMarkerResponse(
                        id = row[Restaurants.id],
                        name = row[Restaurants.name],
                        latitude = row[Restaurants.latitude],
                        longitude = row[Restaurants.longitude],
                        floor = row[Restaurants.floor],
                        category = row[Restaurants.category],
                        priceRange = row[Restaurants.priceRange],
                        averageRating = row[Restaurants.averageRating],
                        reviewCount = row[Restaurants.reviewCount]
                    )
                }
        }
    }

    /**
     * 리뷰 추가
     */
    fun addReview(restaurantId: String, request: ReviewCreateRequest): ReviewResponse? {
        val reviewId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        return transaction {
            // 맛집 존재 확인
            val restaurant = Restaurants.selectAll()
                .where { Restaurants.id eq restaurantId }
                .singleOrNull() ?: return@transaction null

            // 리뷰 추가
            Reviews.insert {
                it[id] = reviewId
                it[Reviews.restaurantId] = restaurantId
                it[userId] = request.userId
                it[rating] = request.rating
                it[memo] = request.memo
                it[photos] = "[]"
                it[tags] = json.encodeToString(request.tags)
                it[visitDate] = request.visitDate
                it[createdAt] = now
            }

            // 평균 평점 및 리뷰 수 업데이트
            val allReviews = Reviews.selectAll()
                .where { Reviews.restaurantId eq restaurantId }
                .map { it[Reviews.rating] }

            val avgRating = allReviews.average()
            val reviewCount = allReviews.size

            Restaurants.update({ Restaurants.id eq restaurantId }) {
                it[averageRating] = avgRating
                it[Restaurants.reviewCount] = reviewCount
            }

            ReviewResponse(
                id = reviewId,
                restaurantId = restaurantId,
                userId = request.userId,
                rating = request.rating,
                memo = request.memo,
                photos = emptyList(),
                tags = request.tags,
                visitDate = request.visitDate,
                createdAt = now.format(dateFormatter)
            )
        }
    }

    /**
     * 리뷰에 사진 추가
     */
    fun addPhotoToReview(reviewId: String, photoUrl: String): Boolean {
        return transaction {
            val review = Reviews.selectAll()
                .where { Reviews.id eq reviewId }
                .singleOrNull() ?: return@transaction false

            val currentPhotos: MutableList<String> = try {
                json.decodeFromString(review[Reviews.photos])
            } catch (e: Exception) {
                mutableListOf()
            }

            if (currentPhotos.size >= 5) return@transaction false

            currentPhotos.add(photoUrl)

            Reviews.update({ Reviews.id eq reviewId }) {
                it[photos] = json.encodeToString(currentPhotos)
            }
            true
        }
    }

    /**
     * 리뷰 삭제
     */
    fun deleteReview(reviewId: String, userId: String): Boolean {
        return transaction {
            val review = Reviews.selectAll()
                .where { Reviews.id eq reviewId }
                .singleOrNull() ?: return@transaction false

            if (review[Reviews.userId] != userId) return@transaction false

            val restaurantId = review[Reviews.restaurantId]

            val deleted = Reviews.deleteWhere { Reviews.id eq reviewId }

            if (deleted > 0) {
                // 평균 평점 및 리뷰 수 업데이트
                val allReviews = Reviews.selectAll()
                    .where { Reviews.restaurantId eq restaurantId }
                    .map { it[Reviews.rating] }

                val avgRating = if (allReviews.isEmpty()) 0.0 else allReviews.average()
                val reviewCount = allReviews.size

                Restaurants.update({ Restaurants.id eq restaurantId }) {
                    it[averageRating] = avgRating
                    it[Restaurants.reviewCount] = reviewCount
                }
            }

            deleted > 0
        }
    }

    /**
     * 내 리뷰 목록
     */
    fun getReviewsByUserId(userId: String): List<ReviewResponse> {
        return transaction {
            Reviews.selectAll()
                .where { Reviews.userId eq userId }
                .orderBy(Reviews.createdAt, SortOrder.DESC)
                .map { row ->
                    ReviewResponse(
                        id = row[Reviews.id],
                        restaurantId = row[Reviews.restaurantId],
                        userId = row[Reviews.userId],
                        rating = row[Reviews.rating],
                        memo = row[Reviews.memo],
                        photos = try { json.decodeFromString(row[Reviews.photos]) } catch (e: Exception) { emptyList() },
                        tags = try { json.decodeFromString(row[Reviews.tags]) } catch (e: Exception) { emptyList() },
                        visitDate = row[Reviews.visitDate],
                        createdAt = row[Reviews.createdAt].format(dateFormatter)
                    )
                }
        }
    }

    /**
     * 북마크 추가
     */
    fun addBookmark(userId: String, restaurantId: String): Boolean {
        return transaction {
            try {
                Bookmarks.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[Bookmarks.userId] = userId
                    it[Bookmarks.restaurantId] = restaurantId
                    it[createdAt] = LocalDateTime.now()
                }
                true
            } catch (e: Exception) {
                false  // 이미 존재
            }
        }
    }

    /**
     * 북마크 삭제
     */
    fun removeBookmark(userId: String, restaurantId: String): Boolean {
        return transaction {
            val deleted = Bookmarks.deleteWhere {
                (Bookmarks.userId eq userId) and (Bookmarks.restaurantId eq restaurantId)
            }
            deleted > 0
        }
    }

    /**
     * 내 북마크 목록
     */
    fun getBookmarksByUserId(userId: String): List<RestaurantMarkerResponse> {
        return transaction {
            val bookmarkedIds = Bookmarks.selectAll()
                .where { Bookmarks.userId eq userId }
                .map { it[Bookmarks.restaurantId] }

            if (bookmarkedIds.isEmpty()) return@transaction emptyList()

            Restaurants.selectAll()
                .where { Restaurants.id inList bookmarkedIds }
                .map { row ->
                    RestaurantMarkerResponse(
                        id = row[Restaurants.id],
                        name = row[Restaurants.name],
                        latitude = row[Restaurants.latitude],
                        longitude = row[Restaurants.longitude],
                        floor = row[Restaurants.floor],
                        category = row[Restaurants.category],
                        priceRange = row[Restaurants.priceRange],
                        averageRating = row[Restaurants.averageRating],
                        reviewCount = row[Restaurants.reviewCount]
                    )
                }
        }
    }
}
