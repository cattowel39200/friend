package com.yourcompany.friendlocator.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.yourcompany.friendlocator.database.Places
import org.slf4j.LoggerFactory
import java.io.File

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)

    fun init() {
        val dbType = System.getenv("DB_TYPE") ?: "sqlite"  // sqlite or postgres

        when (dbType.lowercase()) {
            "postgres" -> initPostgres()
            else -> initSqlite()
        }
    }

    private fun initSqlite() {
        logger.info("Initializing SQLite database...")

        val dbFile = File("data/friendlocator.db")
        dbFile.parentFile?.mkdirs()

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 1  // SQLite는 단일 연결만 지원
            isAutoCommit = false
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(Places)
            logger.info("SQLite tables created successfully")
        }

        logger.info("SQLite database connected: ${dbFile.absolutePath}")
    }

    private fun initPostgres() {
        logger.info("Initializing PostgreSQL database...")

        val host = System.getenv("DB_HOST") ?: "aws-1-ap-northeast-1.pooler.supabase.com"
        val port = System.getenv("DB_PORT") ?: "6543"  // Transaction Pooler port
        val database = System.getenv("DB_NAME") ?: "postgres"
        val user = System.getenv("DB_USER") ?: "postgres.ppybykiciyhvtlmqrnly"
        val password = System.getenv("DB_PASSWORD") ?: "sejonggps0520!"

        logger.info("Connecting to PostgreSQL: $host:$port/$database as $user")

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$database?prepareThreshold=0&sslmode=require"
            driverClassName = "org.postgresql.Driver"
            username = user
            this.password = password
            maximumPoolSize = 1  // Supabase free tier - single connection
            minimumIdle = 0  // Don't keep idle connections
            isAutoCommit = true  // Required for Transaction Pooler
            connectionTimeout = 60000  // 60 seconds
            idleTimeout = 30000  // 30 seconds
            maxLifetime = 60000  // 1 minute max lifetime
            initializationFailTimeout = -1  // Don't fail fast
            validationTimeout = 10000  // 10 seconds for validation
        }

        try {
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)

            transaction {
                SchemaUtils.create(Places)
                logger.info("PostgreSQL tables created successfully")
            }

            logger.info("PostgreSQL database connected: $host:$port")
        } catch (e: Exception) {
            logger.error("Failed to connect to PostgreSQL: ${e.message}", e)
            logger.warn("Falling back to SQLite...")
            initSqlite()
        }
    }
}
