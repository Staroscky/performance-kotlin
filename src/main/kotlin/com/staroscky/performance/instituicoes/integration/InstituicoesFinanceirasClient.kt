package com.staroscky.performance.instituicoes.integration

import com.staroscky.performance.core.feign.LogbookFeignConfig
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "instituicoes-financeiras", configuration = [LogbookFeignConfig::class])
interface InstituicoesFinanceirasClient {

    @GetMapping("/instituicoes-financeiras")
    fun buscar(): InstituicoesFinanceirasResponse
}
