package org.beagle.modules

import org.beagle.Bot
import org.beagle.utility.Command

class HelpModule : Module("Help", true) {
    override fun initialize(bot: Bot) {
        commands.add(
            Command.CommandBuilder(bot.prefix, "help")
                .callback { e, _ ->
                    e.message.channel
                        .flatMap { it.createEmbed(bot.generateEmbed()) }
                        .subscribe()
                }
                .build())
        super.initialize(bot)
    }
}