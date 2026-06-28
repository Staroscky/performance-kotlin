package com.staroscky.performance.contatos.integration

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "contatos-core")
interface ContatosCoreClient {

    @GetMapping("/contatos-core")
    fun buscar(
        @RequestParam("idPessoa") idPessoa: String,
        @RequestParam("idConta") idConta: String,
        @RequestParam("cursor") cursor: String? = null
    ): ContatosCoreResponse
}
