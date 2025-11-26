package com.yourcompany.friendlocator.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * 장소 테이블 정의
 */
object Places : Table("places") {
    val id = varchar("id", 36)  // UUID
    val userId = varchar("user_id", 36)
    val nickname = varchar("nickname", 100)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val memo = text("memo")
    val photos = text("photos")  // JSON array of URLs
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
