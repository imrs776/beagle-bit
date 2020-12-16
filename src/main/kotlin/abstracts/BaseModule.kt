package org.imrs776.abstracts

import com.jagrosh.jdautilities.command.Command

abstract class BaseModule(val category: Command.Category) {
    abstract val commands: Array<Command>
}
