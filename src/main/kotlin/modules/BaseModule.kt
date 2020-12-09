package org.imrs776.modules

import com.jagrosh.jdautilities.command.Command

abstract class BaseModule(val category: Command.Category) {
    abstract val commands: Array<Command>
}
