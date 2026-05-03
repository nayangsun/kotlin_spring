package com.example.kotlin_spring.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/health")
    fun healthCheck(): String {
        return "OK - Java 25 & Kotlin 2.3 is running with PostgreSQL!"
    }
}
