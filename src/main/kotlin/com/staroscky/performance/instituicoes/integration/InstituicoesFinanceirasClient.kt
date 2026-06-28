package com.staroscky.performance.instituicoes.integration

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "instituicoes-financeiras")
interface InstituicoesFinanceirasClient {

    @GetMapping("/instituicoes-financeiras")
    fun buscar(): InstituicoesFinanceirasResponse
}
