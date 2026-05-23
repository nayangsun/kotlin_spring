package com.kotlinspring.user.domain

class UserAlreadyExistsException(
    username: String,
) : RuntimeException("User '$username' already exists.")
