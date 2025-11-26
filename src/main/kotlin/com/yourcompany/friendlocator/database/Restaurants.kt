package com.yourcompany.friendlocator.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * 맛집(가게) 테이블 정의 - 공용
 */
object Restaurants : Table("restaurants") {
    val id = varchar("id", 36)  // UUID
    val name = varchar("name", 200)  // 가게명
    val address = varchar("address", 500)  // 주소
    val latitude = double("latitude")
    val longitude = double("longitude")
    val floor = varchar("floor", 50).nullable()  // 층수 (3층, 지하1층)
    val detailLocation = varchar("detail_location", 200).nullable()  // 상세위치 (A동 302호)
    val category = varchar("category", 50)  // 한식/중식/양식/일식/카페/술집/기타
    val priceRange = varchar("price_range", 10)  // ₩/₩₩/₩₩₩
    val averageRating = double("average_rating").default(0.0)  // 평균 평점
    val reviewCount = integer("review_count").default(0)  // 리뷰 수
    val createdAt = datetime("created_at")
    val createdBy = varchar("created_by", 36)  // 최초 등록자 userId

    override val primaryKey = PrimaryKey(id)
}

/**
 * 리뷰 테이블 정의 - 개인
 */
object Reviews : Table("reviews") {
    val id = varchar("id", 36)  // UUID
    val restaurantId = varchar("restaurant_id", 36).references(Restaurants.id)
    val userId = varchar("user_id", 36)
    val rating = integer("rating")  // 1~5
    val memo = text("memo")  // 후기
    val photos = text("photos")  // JSON array of URLs
    val tags = text("tags")  // JSON array of tags
    val visitDate = varchar("visit_date", 20).nullable()  // 방문일 (yyyy-MM-dd)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

/**
 * 북마크 테이블 정의
 */
object Bookmarks : Table("bookmarks") {
    val id = varchar("id", 36)  // UUID
    val userId = varchar("user_id", 36)
    val restaurantId = varchar("restaurant_id", 36).references(Restaurants.id)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, restaurantId)
    }
}
