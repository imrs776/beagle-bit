package org.beagle.utility

import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import reactor.core.publisher.Mono

typealias ReactionAddedCallback = (event: ReactionAddEvent) -> Unit
typealias ReactionRemovedCallback = (event: ReactionRemoveEvent) -> Unit

class ReactionHandler private constructor(
    val addedCallbacks: List<ReactionAddedCallback>,
    val removedCallbacks: List<ReactionRemovedCallback>,
    val messages: List<Long>,
    val channels: List<Long>

) {
    class ReactionHandlerBuilder() {
        private val addedCallbacks: MutableList<ReactionAddedCallback> = mutableListOf()
        private val removedCallbacks: MutableList<ReactionRemovedCallback> = mutableListOf()
        private var messages: MutableList<Long> = mutableListOf()
        private var channels: MutableList<Long> = mutableListOf()
        fun added(c: ReactionAddedCallback) = apply { addedCallbacks.add(c) }
        fun removed(c: ReactionRemovedCallback) = apply { removedCallbacks.add(c) }
        fun message(messageID: Long) = apply { messages.add(messageID) }
        fun channel(channelID: Long) = apply { channels.add(channelID) }
        fun build() = ReactionHandler(addedCallbacks, removedCallbacks, messages, channels)
    }

    fun execute(event: ReactionAddEvent) {
        Mono.just(event)
            .filter { it.user.block()?.isBot ?: true }
            .filter { messages.isEmpty() || messages.contains(it.messageId.asLong()) }
            .filter { channels.isEmpty() || channels.contains(it.channelId.asLong()) }
            .map { addedCallbacks.forEach { it(event) } }
            .subscribe()
    }

    fun execute(event: ReactionRemoveEvent) {
        Mono.just(event)
            .filter { it.user.block()?.isBot ?: true }
            .filter { messages.isEmpty() || messages.contains(it.messageId.asLong()) }
            .filter { channels.isEmpty() || channels.contains(it.channelId.asLong()) }
            .map { removedCallbacks.forEach { it(event) } }
            .subscribe()
    }
}