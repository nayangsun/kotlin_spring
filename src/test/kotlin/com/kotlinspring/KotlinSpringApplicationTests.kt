package com.kotlinspring

import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class KotlinSpringApplicationTests : StringSpec() {

    init {
        extension(SpringExtension())

        "contextLoads" {
            Unit
        }
    }
}
