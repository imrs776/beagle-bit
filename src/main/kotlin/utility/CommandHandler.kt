package org.beagle.utility

import discord4j.core.`object`.entity.Member
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

typealias MessageCreatedCallback = (event: MessageCreateEvent, arguments: Arguments) -> Unit

class CommandHandler private constructor(
    val prefix: String,
    val name: String,
    val arguments: Arguments,
    val callbacks: List<MessageCreatedCallback>,
    val permission: PERMISSION,
    val conditions: List<CONDITION>,
    val subcommands: List<CommandHandler>,
    val channels: List<Long>
) {
    private val patterns: List<Regex>

    init {
        val result = mutableListOf<Regex>()
        if (callbacks.isNotEmpty())
            result.add(
                ("\\s*$prefix$name" + "\\s+\\w+".repeat(arguments.size) + "\\s*")
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
        private val arguments: MutableList<Argument> = mutableListOf()
        private val callbacks: MutableList<MessageCreatedCallback> = mutableListOf()
        private var permission: PERMISSION = PERMISSION.USER
        private val conditions: MutableList<CONDITION> = mutableListOf()
        private val subcommands: MutableList<CommandHandler> = mutableListOf()
        private var channels: MutableList<Long> = mutableListOf()

        fun <T : Any> argument(arg: String, type: KClass<T>) = apply {
            arguments.add(Argument(arg, null, type.simpleName ?: "null"))
        }

        fun callback(c: MessageCreatedCallback) = apply { callbacks.add(c) }
        fun permission(p: PERMISSION) = apply { permission = p }
        fun condition(c: CONDITION) = apply { conditions.add(c) }
        fun subcommand(c: CommandHandler) = apply { subcommands.add(c) }
        fun channel(channelID: Long) = apply { channels.add(channelID) }
        fun build() =
            CommandHandler(prefix, name, Arguments(arguments), callbacks, permission, conditions, subcommands, channels)
    }

    fun execute(event: MessageCreateEvent) {
        Mono.just(event)
            .filter { channels.isEmpty() || channels.contains(it.message.channelId.asLong()) }
            .map { event.member }
            .filter { it.isPresent }
            .filter { checkPermissions(it.get()) }
            .filter { checkConditions(it.get()) }
            .map { event.message }
            .filter { patterns.any { i -> i.matches(it.content) } }
            .map { it.content.split(" ").map { w -> w.trim() } }
            .filter { parseInput(it) }
            .doOnNext { content ->
                subcommands.find { content[1].equals(it.prefix + it.name, ignoreCase = true) }
                    ?.executeSubcommand(event, content, 1) ?: run {
                    callbacks.forEach { it.invoke(event, arguments) }
                }
                LoggerFactory.getLogger(CommandHandler::class.simpleName)
                    .info("Command $content executed by @${event.member.get().displayName}")
                clearArguments()
            }.subscribe()
    }

    private fun executeSubcommand(event: MessageCreateEvent, content: List<String>, contentStart: Int) {
        subcommands.find { content[contentStart + 1].equals(it.prefix + it.name, ignoreCase = true) }
            ?.executeSubcommand(event, content, contentStart + 1) ?: run {
            callbacks.forEach { it.invoke(event, arguments) }
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

    private fun parseInput(content: List<String>, contentStart: Int = 0): Boolean {
        if (arguments.isEmpty() && content.size == contentStart + 1)
            return true
        return subcommands.find { content[contentStart + 1].equals(it.prefix + it.name, ignoreCase = true) }
            ?.parseInput(content, contentStart + 1) ?: run {
            for (i in arguments.indices) {
                arguments[i].value = content[contentStart + i + 1]
                if (!arguments[i].isValid())
                    return false
            }
            return true
        }
    }

    private fun clearArguments() {
        arguments.forEach { it.value = null }
        subcommands.forEach { it.clearArguments() }
    }
}