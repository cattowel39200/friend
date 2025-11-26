package com.yourcompany.friendlocator.repository

import com.yourcompany.friendlocator.model.LocationResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * 메모리 기반 위치 저장소
 * - 개발/테스트용
 * - 프로덕션에서는 Redis 또는 DB로 교체 권장
 */
object LocationRepository {

    // roomId -> (userId -> LocationResponse)
    private val roomLocations = ConcurrentHashMap<String, ConcurrentHashMap<String, LocationResponse>>()

    // roomCode -> roomId 매핑
    private val roomCodeToId = ConcurrentHashMap<String, String>()

    // 위치 데이터 만료 시간 (2분)
    private const val LOCATION_EXPIRY_MS = 120_000L

    /**
     * 방 참여 또는 생성
     * - 방 코드가 없으면 새로 생성
     * - 있으면 기존 방 ID 반환
     */
    fun joinOrCreateRoom(roomCode: String): String {
        return roomCodeToId.getOrPut(roomCode) {
            val roomId = "room-${roomCode.lowercase()}"
            roomLocations[roomId] = ConcurrentHashMap()
            roomId
        }
    }

    /**
     * 사용자 위치 업데이트
     */
    fun updateLocation(roomId: String, location: LocationResponse) {
        roomLocations.getOrPut(roomId) { ConcurrentHashMap() }[location.userId] = location
    }

    /**
     * 특정 방의 모든 친구 위치 조회 (본인 제외)
     * 만료 시간 체크 제거 - 위치 공유 중단 시에만 삭제됨
     */
    fun getFriendsLocations(roomId: String, excludeUserId: String): List<LocationResponse> {
        val locations = roomLocations[roomId] ?: return emptyList()

        return locations.values
            .filter { it.userId != excludeUserId }
            .toList()
    }

    /**
     * 사용자 위치 삭제 (위치 공유 중단 시)
     */
    fun removeUserLocation(roomId: String, userId: String) {
        roomLocations[roomId]?.remove(userId)
    }

    /**
     * 방 존재 여부 확인
     */
    fun roomExists(roomId: String): Boolean {
        return roomLocations.containsKey(roomId)
    }

    /**
     * 만료된 위치 데이터 정리 (주기적으로 호출)
     */
    fun cleanupExpiredLocations() {
        val now = System.currentTimeMillis()
        roomLocations.forEach { (_, locations) ->
            locations.entries.removeIf { (_, location) ->
                now - location.timestamp > LOCATION_EXPIRY_MS
            }
        }
    }
}
