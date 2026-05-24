package com.kotlinspring.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.util.StringUtils
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.function.Supplier

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        securityContextRepository: SecurityContextRepository,
        csrfTokenRepository: CsrfTokenRepository,
    ): SecurityFilterChain {
        return http
            .csrf {
                it.csrfTokenRepository(csrfTokenRepository)
                it.csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
            }
            .cors { }
            .securityContext { it.securityContextRepository(securityContextRepository) }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                it.sessionFixation { fixation -> fixation.migrateSession() }
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .headers {
                it.contentTypeOptions { }
                it.frameOptions { frame -> frame.deny() }
                it.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)
                }
                it.contentSecurityPolicy { csp -> csp.policyDirectives("default-src 'self'") }
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    writeJson(
                        response = response,
                        status = HttpServletResponse.SC_UNAUTHORIZED,
                        code = "UNAUTHORIZED",
                        message = "Authentication is required.",
                    )
                }
                it.accessDeniedHandler { _, response, _ ->
                    writeJson(
                        response = response,
                        status = HttpServletResponse.SC_FORBIDDEN,
                        code = "ACCESS_DENIED",
                        message = "Access is denied.",
                    )
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(*publicEndpointPatterns).permitAll()
                it.anyRequest().authenticated()
            }
            .build()
    }

    @Bean
    fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager {
        return configuration.authenticationManager
    }

    @Bean
    fun securityContextRepository(): SecurityContextRepository {
        return HttpSessionSecurityContextRepository()
    }

    @Bean
    fun csrfTokenRepository(): CsrfTokenRepository {
        return CookieCsrfTokenRepository.withHttpOnlyFalse()
    }

    @Bean
    fun sessionAuthenticationStrategy(csrfTokenRepository: CsrfTokenRepository): SessionAuthenticationStrategy {
        return CompositeSessionAuthenticationStrategy(
            listOf(
                ChangeSessionIdAuthenticationStrategy(),
                CsrfAuthenticationStrategy(csrfTokenRepository),
            )
        )
    }

    @Bean
    fun corsConfigurationSource(
        @Value("\${app.security.cors.allowed-origins:http://localhost:3000}") allowedOrigins: String,
    ): CorsConfigurationSource {
        val origins = allowedOrigins.split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)

        require(origins.isNotEmpty() && origins.none { it.contains('*') }) {
            "app.security.cors.allowed-origins must list explicit origins."
        }

        val configuration = CorsConfiguration().apply {
            setAllowedOrigins(origins)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf(
                "Content-Type",
                "X-Requested-With",
                "X-XSRF-TOKEN",
                "Accept",
                "Accept-Language",
                "Cache-Control",
            )
            allowCredentials = true
            maxAge = CORS_MAX_AGE_SECONDS
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    private fun writeJson(
        response: HttpServletResponse,
        status: Int,
        code: String,
        message: String,
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }

    private class SpaCsrfTokenRequestHandler : CsrfTokenRequestHandler {
        private val plain = CsrfTokenRequestAttributeHandler()
        private val xor = XorCsrfTokenRequestAttributeHandler()

        override fun handle(
            request: HttpServletRequest,
            response: HttpServletResponse,
            csrfToken: Supplier<CsrfToken>,
        ) {
            xor.handle(request, response, csrfToken)
            csrfToken.get()
        }

        override fun resolveCsrfTokenValue(
            request: HttpServletRequest,
            csrfToken: CsrfToken,
        ): String? {
            return if (StringUtils.hasText(request.getHeader(csrfToken.headerName))) {
                plain.resolveCsrfTokenValue(request, csrfToken)
            } else {
                xor.resolveCsrfTokenValue(request, csrfToken)
            }
        }
    }

    private companion object {
        const val HSTS_MAX_AGE_SECONDS = 31_536_000L
        const val CORS_MAX_AGE_SECONDS = 3_600L

        val publicEndpointPatterns = arrayOf(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/auth/csrf",
            "/auth/login",
            "/auth/register",
            "/auth/logout",
        )
    }
}
