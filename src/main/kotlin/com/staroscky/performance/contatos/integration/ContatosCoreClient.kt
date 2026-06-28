package com.staroscky.performance.contatos.integration

import com.staroscky.performance.core.feign.LogbookFeignConfig
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "contatos-core", configuration = [LogbookFeignConfig::class])
interface ContatosCoreClient {

    @GetMapping("/contatos-core")
    fun buscar(
        @RequestParam("idPessoa") idPessoa: String,
        @RequestParam("idConta") idConta: String,
        @RequestParam("cursor") cursor: String? = null
    ): ContatosCoreResponse
}
