package com.yourcompany.friendlocator.model

import kotlinx.serialization.Serializable

/**
 * 장소 생성 요청
 */
@Serializable
data class PlaceCreateRequest(
    val userId: String,
    val nickname: String,
    val latitude: Double,
    val longitude: Double,
    val memo: String
)

/**
 * 장소 응답
 */
@Serializable
data class PlaceResponse(
    val id: String,
    val userId: String,
    val nickname: String,
    val latitude: Double,
    val longitude: Double,
    val memo: String,
    val photos: List<String>,
    val createdAt: String,
    val photoCount: Int = 0
)

/**
 * 장소 목록 응답 (지도 마커용 - 간략 정보)
 */
@Serializable
data class PlaceMarkerResponse(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val nickname: String,
    val photoCount: Int
)

/**
 * 이미지 업로드 응답
 */
@Serializable
data class ImageUploadResponse(
    val success: Boolean,
    val url: String? = null,
    val message: String? = null
)
