package org.beagle.modules

import discord4j.core.`object`.reaction.ReactionEmoji
import org.beagle.Bot
import org.beagle.utility.CommandHandler

class TestModule : Module("Test", false) {
    private var messageID: Long? = null

    override fun initialize(bot: Bot) {
        val command = CommandHandler.CommandHandlerBuilder(bot.prefix, "test")
            .callback { e, _ ->
                e.message.channel.map {
                    it.createMessage("test reaction handler")
                        .map { message ->
                            messageID = message.id.asLong()
                            message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4D")).subscribe()
                            message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4E")).subscribe()
                        }
                        .subscribe()
                }.subscribe()
            }
            .channel(763113530710163466)
            .build()
//        reactionHandlers.add(
//            ReactionHandler.ReactionHandlerBuilder()
//                .channel(763113530710163466)
//                .added {
//                    it.channel
//                        .flatMap { it.createMessage("123123") }
//                        .subscribe()
//                }
//                .removed {
//                    it.channel
//                        .flatMap { it.createMessage("asdasdasdasda") }
//                        .subscribe()
//                }
//                .build()
//        )
        commandHandlers.add(command)
        super.initialize(bot)
    }
}