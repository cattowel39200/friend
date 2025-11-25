package com.yourcompany.friendlocator.model

import kotlinx.serialization.Serializable

// ===== Request DTOs =====

@Serializable
data class RoomJoinRequest(
    val roomCode: String,
    val userId: String,
    val nickname: String
)

@Serializable
data class LocationRequest(
    val userId: String,
    val nickname: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)

// ===== Response DTOs =====

@Serializable
data class RoomJoinResponse(
    val success: Boolean,
    val roomId: String,
    val message: String
)

@Serializable
data class LocationResponse(
    val userId: String,
    val nickname: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
