package com.yourcompany.friendlocator.repository

import com.yourcompany.friendlocator.database.Places
import com.yourcompany.friendlocator.model.PlaceMarkerResponse
import com.yourcompany.friendlocator.model.PlaceResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object PlaceRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * 장소 생성
     */
    fun createPlace(
        userId: String,
        nickname: String,
        latitude: Double,
        longitude: Double,
        memo: String,
        photos: List<String>
    ): PlaceResponse {
        val placeId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        transaction {
            Places.insert {
                it[id] = placeId
                it[Places.userId] = userId
                it[Places.nickname] = nickname
                it[Places.latitude] = latitude
                it[Places.longitude] = longitude
                it[Places.memo] = memo
                it[Places.photos] = json.encodeToString(photos)
                it[createdAt] = now
            }
        }

        return PlaceResponse(
            id = placeId,
            userId = userId,
            nickname = nickname,
            latitude = latitude,
            longitude = longitude,
            memo = memo,
            photos = photos,
            createdAt = now.format(dateFormatter)
        )
    }

    /**
     * 모든 장소 마커 조회 (지도 표시용)
     */
    fun getAllPlaceMarkers(): List<PlaceMarkerResponse> {
        return transaction {
            Places.selectAll()
                .orderBy(Places.createdAt, SortOrder.DESC)
                .map { row ->
                    val photos: List<String> = try {
                        json.decodeFromString(row[Places.photos])
                    } catch (e: Exception) {
                        emptyList()
                    }
                    PlaceMarkerResponse(
                        id = row[Places.id],
                        latitude = row[Places.latitude],
                        longitude = row[Places.longitude],
                        nickname = row[Places.nickname],
                        photoCount = photos.size
                    )
                }
        }
    }

    /**
     * 장소 상세 조회
     */
    fun getPlaceById(placeId: String): PlaceResponse? {
        return transaction {
            Places.selectAll().where { Places.id eq placeId }
                .singleOrNull()
                ?.let { row ->
                    val photos: List<String> = try {
                        json.decodeFromString(row[Places.photos])
                    } catch (e: Exception) {
                        emptyList()
                    }
                    PlaceResponse(
                        id = row[Places.id],
                        userId = row[Places.userId],
                        nickname = row[Places.nickname],
                        latitude = row[Places.latitude],
                        longitude = row[Places.longitude],
                        memo = row[Places.memo],
                        photos = photos,
                        createdAt = row[Places.createdAt].format(dateFormatter)
                    )
                }
        }
    }

    /**
     * 장소에 사진 추가
     */
    fun addPhotoToPlace(placeId: String, photoUrl: String): Boolean {
        return transaction {
            val place = Places.selectAll().where { Places.id eq placeId }.singleOrNull()
                ?: return@transaction false

            val currentPhotos: MutableList<String> = try {
                json.decodeFromString(place[Places.photos])
            } catch (e: Exception) {
                mutableListOf()
            }

            // 최대 5장 제한
            if (currentPhotos.size >= 5) {
                return@transaction false
            }

            currentPhotos.add(photoUrl)

            Places.update({ Places.id eq placeId }) {
                it[photos] = json.encodeToString(currentPhotos)
            }
            true
        }
    }

    /**
     * 장소 삭제
     */
    fun deletePlace(placeId: String, userId: String): Boolean {
        return transaction {
            val deleted = Places.deleteWhere {
                (Places.id eq placeId) and (Places.userId eq userId)
            }
            deleted > 0
        }
    }

    /**
     * 특정 위치 근처의 장소 조회 (반경 km)
     */
    fun getPlacesNearby(lat: Double, lng: Double, radiusKm: Double = 10.0): List<PlaceMarkerResponse> {
        return transaction {
            // 간단한 방식: 위도/경도 범위로 필터링
            // 1도 ≈ 111km
            val latRange = radiusKm / 111.0
            val lngRange = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)))

            Places.selectAll().where {
                (Places.latitude greaterEq (lat - latRange)) and
                (Places.latitude lessEq (lat + latRange)) and
                (Places.longitude greaterEq (lng - lngRange)) and
                (Places.longitude lessEq (lng + lngRange))
            }.map { row ->
                val photos: List<String> = try {
                    json.decodeFromString(row[Places.photos])
                } catch (e: Exception) {
                    emptyList()
                }
                PlaceMarkerResponse(
                    id = row[Places.id],
                    latitude = row[Places.latitude],
                    longitude = row[Places.longitude],
                    nickname = row[Places.nickname],
                    photoCount = photos.size
                )
            }
        }
    }
}
