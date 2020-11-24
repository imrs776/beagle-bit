package org.beagle.utility

import org.slf4j.LoggerFactory

class Arguments : ArrayList<Argument> {
    internal constructor() : super()
    internal constructor(list: List<Argument>) : super(list)

    operator fun get(string: String): Argument? {
        forEach { if (it.name == string) return it }
        LoggerFactory.getLogger(Arguments::class.simpleName)
            .warn("Invalid argument [$string]")
        return null
    }

    override fun toString(): String = super.toString().trimStart('[').trimEnd(']')
}