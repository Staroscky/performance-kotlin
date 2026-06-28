package com.staroscky.performance.contatos

import com.staroscky.performance.contatos.domain.Contato
import com.staroscky.performance.contatos.service.ContatosService
import com.staroscky.performance.core.caronte.CaronteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/contatos")
@CaronteMapping(rel = "contatos")
class ContatosController(private val service: ContatosService) {

    @GetMapping
    fun listar(): Map<String, List<Contato>> =
        mapOf("data" to service.buscar())
}
