package com.staroscky.performance.core.feign

import com.staroscky.performance.core.auth.AuthTokenService
import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.stereotype.Component

@Component
class AuthRequestInterceptor(
    private val authTokenService: AuthTokenService
) : RequestInterceptor {

    override fun apply(template: RequestTemplate) {
        template.header("Authorization", "Bearer ${authTokenService.getToken()}")
    }
}
