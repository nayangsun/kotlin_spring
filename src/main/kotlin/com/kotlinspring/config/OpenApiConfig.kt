package com.kotlinspring.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Kotlin Spring API")
                    .description("API documentation for the Kotlin Spring sample project.")
                    .version("v0.0.1")
            )
            .servers(
                listOf(
                    Server()
                        .url("/")
                        .description("Current server")
                )
            )
    }
}
