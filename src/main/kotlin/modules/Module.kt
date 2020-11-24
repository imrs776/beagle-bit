package org.beagle.modules

import org.beagle.Bot
import org.beagle.utility.Command
import org.slf4j.LoggerFactory

abstract class Module(val name: String, val isVisible: Boolean = true){
    open fun initialize(bot: Bot) {
        commands.forEach {bot.addCommand(it)}
        LoggerFactory.getLogger(this::class.simpleName)
            .info("Initializing module ${this::class.simpleName}")
    }
    protected var commands : MutableList<Command> = mutableListOf()
    open fun description() : String {
        var description = ""
        commands.forEach { command ->
            fun addDescription(c : Command, s: String) {
                if (c.callbacks.isNotEmpty())
                    description += s +  c.prefix + c.name + c.arguments.joinToString { " [${it.name}]" } + "\n"
                c.subcommands.forEach { addDescription(it, "$s${c.prefix}${c.name} ") }
            }
            addDescription(command, "")
        }
        return description
    }
}



