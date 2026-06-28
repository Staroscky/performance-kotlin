package com.staroscky.performance.instituicoes

import com.staroscky.performance.core.caronte.CaronteMapping
import com.staroscky.performance.instituicoes.domain.Instituicao
import com.staroscky.performance.instituicoes.service.InstituicoesService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/instituicoes")
@CaronteMapping(rel = "instituicoes")
class InstituicoesController(private val service: InstituicoesService) {

    @GetMapping
    fun listar(): Map<String, List<Instituicao>> =
        mapOf("data" to service.buscar())
}
