package com.kotlinspring

import com.kotlinspring.config.TestEmbeddedPostgresConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestEmbeddedPostgresConfig::class)
@ActiveProfiles("test")
class KotlinSpringApplicationTests : StringSpec() {

    init {
        extension(SpringExtension())

        "contextLoads" {
            Unit
        }
    }
}
