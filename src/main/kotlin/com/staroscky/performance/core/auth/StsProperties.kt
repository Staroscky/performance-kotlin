package com.staroscky.performance.core.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("sts")
data class StsProperties(
    val url: String,
    val client: Client,
    val appId: String
) {
    data class Client(val id: String, val secret: String)
}
