package com.kotlinspring.user.api

import com.kotlinspring.common.api.ApiResponse
import com.kotlinspring.user.application.CreateUserCommand
import com.kotlinspring.user.application.UserUseCase
import com.kotlinspring.user.application.security.CurrentUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CsrfLogoutHandler
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Session authentication and current user APIs")
class AuthController(
    private val userUseCase: UserUseCase,
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
    private val sessionAuthenticationStrategy: SessionAuthenticationStrategy,
    private val csrfTokenRepository: CsrfTokenRepository,
) {

    @GetMapping("/csrf")
    @Operation(
        summary = "Get CSRF token",
        description = "Returns the current CSRF token for cookie-based session requests. " +
            "Call this before unsafe methods such as POST, PUT, PATCH, and DELETE, " +
            "and call it again after login or logout because authentication changes rotate the CSRF token."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "CSRF token issued.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
        ]
    )
    fun csrf(@Parameter(hidden = true) csrfToken: CsrfToken): ApiResponse<CsrfResponse> {
        return ApiResponse.success(
            CsrfResponse(
                parameterName = csrfToken.parameterName,
                headerName = csrfToken.headerName,
                token = csrfToken.token,
            )
        )
    }

    @PostMapping("/login")
    @Operation(
        summary = "Log in",
        description = "Authenticates a user and stores the security context in the HTTP session. " +
            "Send a valid X-XSRF-TOKEN header for this request, then call GET /auth/csrf again " +
            "before the next unsafe request."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Login succeeded.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            OpenApiResponse(
                responseCode = "401",
                description = "Username or password is invalid.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"UNAUTHORIZED","message":"Authentication failed.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
    ): ApiResponse<UserResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, servletRequest, servletResponse)

        return ApiResponse.success(UserResponse.from(authentication.principal as CurrentUserPrincipal))
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register user",
        description = "Creates a user account with USER role."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "201",
                description = "User registered.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            OpenApiResponse(
                responseCode = "409",
                description = "Username already exists.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"USER_ALREADY_EXISTS","message":""" +
                                    """"User 'admin' already exists.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userUseCase.create(
            CreateUserCommand(
                username = request.username,
                password = request.password,
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(UserResponse.from(user), message = "User registered."))
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Log out",
        description = "Clears the current HTTP session security context and CSRF token. " +
            "Send a valid X-XSRF-TOKEN header for this request. Call GET /auth/csrf again " +
            "before any later unsafe request."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(responseCode = "204", description = "Logout succeeded."),
        ]
    )
    fun logout(
        authentication: Authentication?,
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
    ): ResponseEntity<Void> {
        CsrfLogoutHandler(csrfTokenRepository).logout(servletRequest, servletResponse, authentication)
        SecurityContextLogoutHandler().logout(servletRequest, servletResponse, authentication)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Returns the authenticated session user."
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Current user returned.",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            OpenApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [
                    Content(
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                value = """{"code":"UNAUTHORIZED","message":""" +
                                    """"Authentication is required.","data":null}"""
                            ),
                        ]
                    ),
                ]
            ),
        ]
    )
    fun me(@AuthenticationPrincipal principal: CurrentUserPrincipal): ApiResponse<UserResponse> {
        return ApiResponse.success(UserResponse.from(principal))
    }
}
