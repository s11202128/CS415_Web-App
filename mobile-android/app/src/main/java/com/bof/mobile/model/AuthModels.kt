package com.bof.mobile.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val fullName: String,
    val userId: Int,
    val customerId: Int?,
    val email: String,
    val mobile: String?,
    val nationalId: String?,
    val isAdmin: Boolean
)

data class RegisterRequest(
    val fullName: String,
    val mobile: String,
    val email: String,
    val accountType: String? = null,
    val password: String,
    val confirmPassword: String
)

data class RegisterResponse(
    val userId: Int,
    val customerId: Int,
    val fullName: String,
    val email: String,
    val emailVerified: Boolean,
    val message: String
)
