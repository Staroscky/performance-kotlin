package com.staroscky.performance.sugestoes.integration

import com.staroscky.performance.core.feign.LogbookFeignConfig
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "inteligencia", configuration = [LogbookFeignConfig::class])
interface InteligenciaClient {

    @GetMapping("/inteligencia")
    fun buscar(
        @RequestParam("tela") tela: String,
        @RequestParam("cliente") cliente: String
    ): InteligenciaResponse
}
