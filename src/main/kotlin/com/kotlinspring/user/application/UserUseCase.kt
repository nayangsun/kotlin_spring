package com.kotlinspring.user.application

interface UserUseCase {
    fun create(command: CreateUserCommand): UserResult
}
