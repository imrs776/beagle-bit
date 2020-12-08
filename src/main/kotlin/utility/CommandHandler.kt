package org.beagle.utility

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

typealias MessageCreatedCallback = (event: MessageCreateEvent, arguments: Arguments) -> Mono<Void>

class CommandHandler private constructor(
    val prefix: String,
    val name: String,
    val arguments: Arguments,
    val callbacks: List<MessageCreatedCallback>,
    val permission: PERMISSION,
    val conditions: List<CONDITION>,
    val subcommands: List<CommandHandler>,
    val channels: List<Snowflake>
) {
    private val patterns: List<Regex>

    init {
        val result = mutableListOf<Regex>()
        if (callbacks.isNotEmpty())
            result.add(
                ("\\s*$prefix$name" + "\\s+\\S+".repeat(arguments.size) + "\\s*")
                    .toRegex(RegexOption.IGNORE_CASE)
            )
        subcommands.forEach { i ->
            i.patterns.forEach { j ->
                result.add(
                    ("\\s*$prefix$name\\s+${j.pattern.removePrefix("\\s*")}")
                        .toRegex(RegexOption.IGNORE_CASE)
                )
            }
        }
        patterns = result
    }

    class CommandHandlerBuilder(private val prefix: String, private val name: String) {
        private val arguments = mutableListOf<Argument>()
        private val callbacks = mutableListOf<MessageCreatedCallback>()
        private var permission = PERMISSION.USER
        private val conditions = mutableListOf<CONDITION>()
        private val subcommands = mutableListOf<CommandHandler>()
        private val channels = mutableListOf<Snowflake>()

        fun <T : Any> argument(arg: String, type: KClass<T>) = apply {
            arguments.add(Argument(arg, null, type.simpleName ?: "null"))
        }

        fun callback(c: MessageCreatedCallback) = apply { callbacks.add(c) }
        fun permission(p: PERMISSION) = apply { permission = p }
        fun condition(c: CONDITION) = apply { conditions.add(c) }
        fun subcommand(c: CommandHandler) = apply { subcommands.add(c) }
        fun channel(channelID: Snowflake) = apply { channels.add(channelID) }
        fun build() =
            CommandHandler(prefix, name, Arguments(arguments), callbacks, permission, conditions, subcommands, channels)
    }

    fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.just(event)
            .filter { event.member.isPresent && (channels.isEmpty() || channels.contains(it.message.channelId)) }
            .filter { checkPermissions(event.member.get()) && checkConditions(event.member.get()) }
            .filter { patterns.any { i -> i.matches(event.message.content) } }
            .flatMap {
                val content = event.message.content.split(" ").map { w -> w.trim() }
                LoggerFactory.getLogger(CommandHandler::class.simpleName)
                    .info("Command $content executed by @${event.member.get().displayName}")

                parseInput(content).flatMap { arguments ->
                    findAndExecute(event, content, 0, arguments)
                        .onErrorContinue { e, i ->
                            LoggerFactory.getLogger(CommandHandler::class.simpleName)
                                .error("Failed to execute command $content, $e")
                        }
                }
            }
            .then()
    }

    private fun findAndExecute(
        event: MessageCreateEvent,
        content: List<String>,
        contentStart: Int,
        arguments: Arguments
    ): Mono<Void> {
        return subcommands.find { content[contentStart + 1].equals(it.prefix + it.name, ignoreCase = true) }
            ?.findAndExecute(event, content, contentStart + 1, arguments) ?: run {
            Flux.fromIterable(callbacks).flatMap { it.invoke(event, arguments) }.then()
        }
    }

    private fun checkPermissions(member: Member): Boolean {
        if (!permission.check(member)) {
            LoggerFactory.getLogger(CommandHandler::class.simpleName)
                .warn("Member @${member.displayName} doesn't have permission [$permission]")
            return false
        }
        return true
    }

    private fun checkConditions(member: Member): Boolean {
        var check = true
        conditions.forEach { check = check && it.check(member) }
        if (!check) {
            LoggerFactory.getLogger(CommandHandler::class.simpleName)
                .warn("Member @${member.displayName} doesn't have conditions $conditions")
            return false
        }
        return true
    }

    private fun parseInput(content: List<String>, contentStart: Int = 0): Mono<Arguments> {
        val result = arguments
        if (result.isEmpty() && content.size == contentStart + 1)
            return Mono.just(result)
        return subcommands.find { content[contentStart + 1].equals(it.prefix + it.name, ignoreCase = true) }
            ?.parseInput(content, contentStart + 1) ?: run {
            for (i in result.indices) {
                result[i].value = content[contentStart + i + 1]
                if (!result[i].isValid())
                    return Mono.empty()
            }
            return Mono.just(result)
        }
    }

    private fun clearArguments() {
        arguments.forEach { it.value = null }
        subcommands.forEach { it.clearArguments() }
    }
}