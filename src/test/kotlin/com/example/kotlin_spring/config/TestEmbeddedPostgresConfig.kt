package com.example.kotlin_spring.config

import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@TestConfiguration
class TestEmbeddedPostgresConfig {

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val embedded = EmbeddedPostgres.builder()
            .start()
        
        val hikari = HikariDataSource().apply {
            jdbcUrl = embedded.getJdbcUrl("postgres", "postgres")
            username = "postgres"
            isAutoCommit = false
        }
        
        // Wrap to ensure the embedded process closes when the DataSource closes
        return object : HikariDataSource(hikari), AutoCloseable {
            override fun close() {
                super.close()
                embedded.close()
            }
        }
    }
}
