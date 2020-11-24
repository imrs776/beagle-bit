package org.beagle.modules

import org.beagle.Bot
import org.beagle.utility.Command

class TestModule : Module("Test") {
    override fun initialize(bot: Bot) {
        val subcommand1 = Command.CommandBuilder("", "ping")
            .argument("amount", Long::class)
            .callback { e, a ->
                val amount: Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                e.message.channel
                    .repeat(amount - 1)
                    .flatMap { it.createMessage("Pong") }
                    .subscribe()
            }
            .build()

        val subcommand2 = Command.CommandBuilder("", "pong")
            .argument("amount", Long::class)
            .callback { e, a ->
                val amount: Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                e.message.channel
                    .repeat(amount - 1)
                    .flatMap { it.createMessage("Ping") }
                    .subscribe()
            }
            .build()

        val command = Command.CommandBuilder(bot.prefix, "test")
            .callback { e, a ->
                val amount: Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                e.message.channel
                    .repeat(amount - 1)
                    .flatMap { it.createMessage("Pong") }
                    .subscribe()
            }
            .subcommand(subcommand1)
            .subcommand(subcommand2)
            .build()
        commands.add(command)
        super.initialize(bot)
    }
}