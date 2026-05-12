package com.kotlinspring.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/health")
    fun healthCheck(): String = HEALTH_MESSAGE

    companion object {
        private const val HEALTH_MESSAGE = "OK - Java 25 & Kotlin 2.3 is running with PostgreSQL!"
    }
}
