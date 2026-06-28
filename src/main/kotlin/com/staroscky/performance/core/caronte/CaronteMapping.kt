package com.staroscky.performance.core.caronte

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CaronteMapping(val rel: String)
