package com.staroscky.performance.core

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import java.util.Base64
import java.util.UUID

@Component
@RequestScope
class UsuarioContext(request: HttpServletRequest) {

    val idPessoa: UUID
    val idConta: UUID

    init {
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?: error("Header Authorization ausente")
        val payloadJson = String(
            Base64.getUrlDecoder().decode(token.split(".").getOrElse(1) { "" })
        )
        @Suppress("UNCHECKED_CAST")
        val claims = ObjectMapper().readValue(payloadJson, Map::class.java) as Map<String, Any?>
        idPessoa = UUID.fromString(
            claims["idPessoa"] as? String ?: error("Claim idPessoa ausente")
        )
        idConta = UUID.fromString(
            claims["idConta"] as? String ?: error("Claim idConta ausente")
        )
    }
}
