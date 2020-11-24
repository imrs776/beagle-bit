package org.beagle.utility

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class Argument internal constructor(
    val name: String,
    var value: String?,
    private val type: String
) {
    fun toStringOrNull() = checkCastType(this, String::class)
        .apply { this ?: logCastError<String>(this@Argument) }

    fun toByteOrNull() = checkCastType(this, Byte::class)?.toByteOrNull()
        .apply { this ?: logCastError<Byte>(this@Argument) }

    fun toShortOrNull() = checkCastType(this, Short::class)?.toShortOrNull()
        .apply { this ?: logCastError<Short>(this@Argument) }

    fun toIntOrNull() = checkCastType(this, Int::class)?.toIntOrNull()
        .apply { this ?: logCastError<Int>(this@Argument) }

    fun toLongOrNull() = checkCastType(this, Long::class)?.toLongOrNull()
        .apply { this ?: logCastError<Long>(this@Argument) }

    fun toFloatOrNull() = checkCastType(this, Float::class)?.toFloatOrNull()
        .apply { this ?: logCastError<Float>(this@Argument) }

    fun toDoubleOrNull() = checkCastType(this, Double::class)?.toDoubleOrNull()
        .apply { this ?: logCastError<Double>(this@Argument) }

    fun isValid(): Boolean {
        return when (type) {
            String::class.simpleName -> this.toStringOrNull() != null
            Byte::class.simpleName -> this.toByteOrNull() != null
            Short::class.simpleName -> this.toShortOrNull() != null
            Int::class.simpleName -> this.toIntOrNull() != null
            Long::class.simpleName -> this.toLongOrNull() != null
            Float::class.simpleName -> this.toFloatOrNull() != null
            Double::class.simpleName -> this.toDoubleOrNull() != null
            else -> false
        }
    }

    private fun <T : Any> isType(type: KClass<T>) = this.type == type.simpleName
    private inline fun <reified T> logCastError(arg: Argument) =
        LoggerFactory.getLogger(Argument::class.simpleName)
            .warn("Invalid cast of [$arg] to ${T::class.simpleName}")

    private inline fun <reified T : Any> checkCastType(arg: Argument, type: KClass<T>): String? =
        if (arg.isType(type)) arg.value else null

    override fun toString(): String = "($name):$type" + (value?.let { " = $value" } ?: "")
}


