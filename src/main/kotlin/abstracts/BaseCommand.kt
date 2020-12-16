package org.imrs776.abstracts

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import org.slf4j.LoggerFactory

abstract class BaseCommand(module: BaseModule) : Command() {
    init {
        category = module.category
    }

    override fun execute(event: CommandEvent) {
        LoggerFactory.getLogger(this::class.simpleName)
            .info("Command [${event.message.contentDisplay}] executed by ${event.author}");
    }
}