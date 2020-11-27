package org.beagle.modules

import org.beagle.Bot
import org.beagle.utility.CommandHandler
import org.beagle.utility.ReactionHandler
import org.slf4j.LoggerFactory

abstract class Module(val name: String, val isVisible: Boolean = true) {
    open fun initialize(bot: Bot) {
        commandHandlers.forEach { bot.addCommandHandler(it) }
        reactionHandlers.forEach { bot.addReactionHandler(it) }
        LoggerFactory.getLogger(this::class.simpleName)
            .info("Initializing module ${this::class.simpleName}")
    }

    protected var commandHandlers: MutableList<CommandHandler> = mutableListOf()
    protected var reactionHandlers: MutableList<ReactionHandler> = mutableListOf()

    open fun description(): String {
        var description = ""
        commandHandlers.forEach { command ->
            fun addDescription(c: CommandHandler, s: String) {
                if (c.callbacks.isNotEmpty())
                    description += s + c.prefix + c.name + c.arguments.joinToString { " [${it.name}]" } + "\n"
                c.subcommands.forEach { addDescription(it, "$s${c.prefix}${c.name} ") }
            }

            addDescription(command, "")
        }
        return description
    }
}



