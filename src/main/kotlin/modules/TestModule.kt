package org.beagle.modules

import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji
import org.beagle.Bot
import org.beagle.utility.CommandHandler
import org.beagle.utility.DialogHandler
import org.beagle.utility.DialogStage

class TestModule : Module("Test", true) {
    private var messageID: Long? = null

    override fun initialize(bot: Bot) {

        val dialog = DialogHandler.DialogHandlerBuilder()
            .stage(DialogStage.DialogStageBuilder()
                .title("First Stage")
                .text("Text of the first stage")
                .callback(ReactionEmoji.unicode("\uD83D\uDC4D")) { e ->
                    1
                }
                .callback(ReactionEmoji.unicode("\uD83D\uDC4E")) { _ -> null }
                .build())
            .stage(DialogStage.DialogStageBuilder()
                .title("Second Stage")
                .text("Text of the second stage")
                .callback(ReactionEmoji.unicode("\uD83D\uDC4D")) { _ -> null }
                .build())
            .build()

        val command = CommandHandler.CommandHandlerBuilder(bot.prefix, "test")
            .callback { e, _ ->
                e.message.channel
                    .filter { e.message.author.isPresent }
                    .flatMap {
                        bot.startDialogSession(dialog.start(e.message.author.get(), e.message.channel))
                    }
            }
            .channel(Snowflake.of(763113530710163466))
            .build()

        commandHandlers.add(command)
        super.initialize(bot)
    }
}