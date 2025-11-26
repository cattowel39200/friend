package com.yourcompany.friendlocator.model

import kotlinx.serialization.Serializable

/**
 * 맛집 등록 요청
 */
@Serializable
data class RestaurantCreateRequest(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val floor: String? = null,
    val detailLocation: String? = null,
    val category: String,
    val priceRange: String,
    val createdBy: String,
    // 첫 리뷰 정보
    val rating: Int,
    val memo: String,
    val tags: List<String> = emptyList(),
    val visitDate: String? = null
)

/**
 * 맛집 응답 (목록/마커용)
 */
@Serializable
data class RestaurantMarkerResponse(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val floor: String? = null,
    val category: String,
    val priceRange: String,
    val averageRating: Double,
    val reviewCount: Int
)

/**
 * 맛집 상세 응답
 */
@Serializable
data class RestaurantDetailResponse(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val floor: String? = null,
    val detailLocation: String? = null,
    val category: String,
    val priceRange: String,
    val averageRating: Double,
    val reviewCount: Int,
    val createdAt: String,
    val createdBy: String,
    val reviews: List<ReviewResponse> = emptyList(),
    val isBookmarked: Boolean = false
)

/**
 * 리뷰 등록 요청
 */
@Serializable
data class ReviewCreateRequest(
    val userId: String,
    val rating: Int,
    val memo: String,
    val tags: List<String> = emptyList(),
    val visitDate: String? = null
)

/**
 * 리뷰 응답
 */
@Serializable
data class ReviewResponse(
    val id: String,
    val restaurantId: String,
    val userId: String,
    val userNickname: String = "",
    val rating: Int,
    val memo: String,
    val photos: List<String>,
    val tags: List<String>,
    val visitDate: String? = null,
    val createdAt: String
)

/**
 * 주변 맛집 검색 요청
 */
@Serializable
data class NearbySearchRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 10.0,
    val category: String? = null
)

/**
 * 북마크 응답
 */
@Serializable
data class BookmarkResponse(
    val id: String,
    val restaurantId: String,
    val createdAt: String
)

/**
 * 중복 체크 응답
 */
@Serializable
data class DuplicateCheckResponse(
    val hasDuplicates: Boolean,
    val restaurants: List<RestaurantMarkerResponse>
)
