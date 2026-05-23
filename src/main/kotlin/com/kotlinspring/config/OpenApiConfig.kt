package com.kotlinspring.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springdoc.core.customizers.OpenApiCustomizer

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
            .components(
                Components()
                    .addSecuritySchemes(
                        SESSION_COOKIE_SECURITY,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.COOKIE)
                            .name("JSESSIONID")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(SESSION_COOKIE_SECURITY))
    }

    @Bean
    fun operationSecurityCustomizer(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            openApi.paths.orEmpty().forEach { (path, item) ->
                item.readOperationsMap().forEach { (method, operation) ->
                    operation.security = when {
                        isPublicOperation(method, path) ->
                            emptyList()

                        method in CSRF_METHODS ->
                            listOf(SecurityRequirement().addList(SESSION_COOKIE_SECURITY))

                        else -> null
                    }
                }
            }
        }
    }

    private fun isPublicOperation(method: PathItem.HttpMethod, path: String): Boolean {
        return path.startsWith("/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path in PUBLIC_ENDPOINTS ||
            method == PathItem.HttpMethod.GET && path in PUBLIC_GET_ENDPOINTS
    }

    private companion object {
        const val SESSION_COOKIE_SECURITY = "sessionCookie"

        val CSRF_METHODS = setOf(
            PathItem.HttpMethod.POST,
            PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.PATCH,
            PathItem.HttpMethod.DELETE,
        )

        val PUBLIC_ENDPOINTS = setOf(
            "/auth/csrf",
            "/auth/login",
            "/auth/register",
            "/auth/logout",
            "/actuator/health",
            "/actuator/info",
        )

        val PUBLIC_GET_ENDPOINTS = emptySet<String>()
    }
}
