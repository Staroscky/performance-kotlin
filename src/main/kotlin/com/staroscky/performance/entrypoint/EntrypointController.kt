package com.staroscky.performance.entrypoint

import com.staroscky.performance.core.caronte.CaronteItem
import com.staroscky.performance.core.caronte.CaronteRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class EntrypointController(private val registry: CaronteRegistry) {

    @GetMapping("/entrypoint")
    fun entrypoint(): Map<String, List<CaronteItem>> =
        mapOf("data" to registry.items)
}
