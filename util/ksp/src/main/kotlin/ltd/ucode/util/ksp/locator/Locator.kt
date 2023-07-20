package ltd.ucode.util.ksp.locator

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Locator(val kClass: KClass<*>)
