package com.staroscky.performance.core.auth

import com.fasterxml.jackson.annotation.JsonProperty

data class StsTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Long
)
