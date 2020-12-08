package org.beagle.utility

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Consumer

typealias DialogCallback = (event: ReactionAddEvent) -> Int?


class DialogHandler private constructor(
    val stages: List<DialogStage>
) {
    class DialogHandlerBuilder() {
        private val stages = mutableListOf<DialogStage>()
        fun stage(stage: DialogStage) = apply { stages.add(stage) }
        fun build() = DialogHandler(stages)
    }


    fun start(user: User, channel: Mono<MessageChannel>) = DialogSession(this, user, channel)
}

class DialogStage private constructor(
    val embed: Consumer<EmbedCreateSpec>,
    val callbacks: List<Pair<ReactionEmoji, DialogCallback>>
) {
    class DialogStageBuilder() {
        private var embed: Consumer<EmbedCreateSpec> = Consumer { it.setColor(Helper.getRandomColor()) }
        private val callbacks = mutableListOf<Pair<ReactionEmoji, DialogCallback>>()

        fun title(title: String) = apply { embed = embed.andThen { it.setTitle(title) } }
        fun text(text: String) = apply { embed = embed.andThen { it.setDescription(text) } }
        fun image(url: String) = apply { embed = embed.andThen { it.setImage(url) } }
        fun callback(emoji: ReactionEmoji, callback: DialogCallback) = apply { callbacks.add(Pair(emoji, callback)) }

        fun build() = DialogStage(embed, callbacks)
    }
}

class DialogSession(
    private val dialogHandler: DialogHandler,
    private val user: User,
    private val channel: Mono<MessageChannel>
) {
    val reactionHandles = mutableListOf<ReactionHandler>()
    var isExist = true

    fun updateStage(stageNumber: Int, message: Mono<Message> = Mono.empty()): Mono<Void> {
        return channel.filter { stageNumber >= 0 && stageNumber < dialogHandler.stages.size && isExist }
            .flatMap { channel ->
                message.flatMap { it.edit { c -> c.setEmbed(dialogHandler.stages[stageNumber].embed) } }
                    .switchIfEmpty(channel.createEmbed(dialogHandler.stages[stageNumber].embed))
                    .doOnError { isExist = false }
            }.flatMap { msg ->
                reactionHandles.clear()
                val mono1 = msg.removeAllReactions()
                val mono2 = Flux.fromIterable(dialogHandler.stages[stageNumber].callbacks).flatMap { pair ->
                    reactionHandles.add(ReactionHandler.ReactionHandlerBuilder()
                        .message(msg.id)
                        .channel(msg.channelId)
                        .user(user.id)
                        .emoji(pair.first)
                        .added { e ->
                            if (!isExist) return@added Mono.empty()
                            pair.second(e)?.let { this.updateStage(it, e.message) } ?: run {
                                isExist = false
                                e.message.flatMap { it.delete() }
                            }
                        }
                        .build())
                    msg.addReaction(pair.first)
                }
                Flux.concat(mono1, mono2).then()
            }
    }
}