package com.moviepilot.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * MFA (Multi-Factor Authentication) models
 */
data class MFAStatus(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("unverified") val unverified: Boolean?
)

data class MFASecret(
    @SerializedName("secret") val secret: String,
    @SerializedName("otpauth_url") val otpauthUrl: String
)

data class MFAVerifyRequest(
    @SerializedName("code") val code: String
)

data class MFAEnableRequest(
    @SerializedName("code") val code: String,
    @SerializedName("secret") val secret: String
)
