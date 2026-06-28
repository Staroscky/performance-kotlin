package com.staroscky.performance.sugestoes

import com.staroscky.performance.core.caronte.CaronteMapping
import com.staroscky.performance.sugestoes.domain.Sugestao
import com.staroscky.performance.sugestoes.service.SugestoesService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sugestoes")
@CaronteMapping(rel = "sugestoes")
class SugestoesController(private val service: SugestoesService) {

    @GetMapping
    fun listar(): Map<String, List<Sugestao>> =
        mapOf("data" to service.buscar())
}
