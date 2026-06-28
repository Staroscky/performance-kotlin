package com.staroscky.performance.sugestoes.integration

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "inteligencia")
interface InteligenciaClient {

    @GetMapping("/inteligencia")
    fun buscar(
        @RequestParam("tela") tela: String,
        @RequestParam("cliente") cliente: String
    ): InteligenciaResponse
}
