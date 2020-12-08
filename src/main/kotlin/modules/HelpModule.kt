package org.beagle.modules

import org.beagle.Bot
import org.beagle.utility.CommandHandler

class HelpModule : Module("Help", true) {
    override fun initialize(bot: Bot) {
        commandHandlers.add(
            CommandHandler.CommandHandlerBuilder(bot.prefix, "help")
                .callback { e, _ ->
                    e.message.channel
                        .flatMap { it.createEmbed(bot.generateEmbed()) }
                        .then()
                }
                .build())
        super.initialize(bot)
    }
}