package org.beagle.utility

import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

typealias ReactionAddedCallback = (event: ReactionAddEvent) -> Mono<Void>
typealias ReactionRemovedCallback = (event: ReactionRemoveEvent) -> Mono<Void>

class ReactionHandler private constructor(
    val addedCallbacks: List<ReactionAddedCallback>,
    val removedCallbacks: List<ReactionRemovedCallback>,
    val messages: List<Snowflake>,
    val channels: List<Snowflake>,
    val users: List<Snowflake>,
    val emojis: List<ReactionEmoji>
) {
    class ReactionHandlerBuilder() {
        private val addedCallbacks = mutableListOf<ReactionAddedCallback>()
        private val removedCallbacks = mutableListOf<ReactionRemovedCallback>()
        private val messages = mutableListOf<Snowflake>()
        private val channels = mutableListOf<Snowflake>()
        private val users = mutableListOf<Snowflake>()
        private val emojis = mutableListOf<ReactionEmoji>()
        fun added(c: ReactionAddedCallback) = apply { addedCallbacks.add(c) }
        fun removed(c: ReactionRemovedCallback) = apply { removedCallbacks.add(c) }
        fun message(messageID: Snowflake) = apply { messages.add(messageID) }
        fun channel(channelID: Snowflake) = apply { channels.add(channelID) }
        fun user(userID: Snowflake) = apply { users.add(userID) }
        fun emoji(emoji: ReactionEmoji) = apply { emojis.add(emoji) }
        fun build() = ReactionHandler(addedCallbacks, removedCallbacks, messages, channels, users, emojis)
    }

    fun execute(event: ReactionAddEvent): Mono<Void> {
        return Mono.just(event)
            .filter { !(it.user.block()?.isBot ?: false) }
            .filter { messages.isEmpty() || messages.contains(it.messageId) }
            .filter { channels.isEmpty() || channels.contains(it.channelId) }
            .filter { users.isEmpty() || users.contains(it.userId) }
            .filter { emojis.isEmpty() || emojis.contains(it.emoji) }
            .flatMap {
                Flux.fromIterable(addedCallbacks)
                    .flatMap { it(event) }
                    .then()
            }
    }

    fun execute(event: ReactionRemoveEvent): Mono<Void> {
        return Mono.just(event)
            .filter { !(it.user.block()?.isBot ?: false) }
            .filter { messages.isEmpty() || messages.contains(it.messageId) }
            .filter { channels.isEmpty() || channels.contains(it.channelId) }
            .filter { users.isEmpty() || users.contains(it.userId) }
            .filter { emojis.isEmpty() || emojis.contains(it.emoji) }
            .flatMap {
                Flux.fromIterable(removedCallbacks)
                    .flatMap { it(event) }
                    .then()
            }
    }
}