package org.beagle.utility

import discord4j.core.`object`.entity.Member
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

typealias MessageCommand = (event: MessageCreateEvent, arguments : Arguments) -> Unit

class Command private constructor(
    val prefix          : String,
    val name            : String,
    val arguments       : Arguments,
    val callbacks       : List<MessageCommand>,
    val permission      : PERMISSION,
    val conditions      : List<CONDITION>,
    val subcommands     : List<Command>) {
    private val patterns : List<Regex>
    init {
        val result = mutableListOf<Regex>()
        if (callbacks.isNotEmpty())
            result.add(("\\s*$prefix$name" + "\\s+\\w+".repeat(arguments.size) + "\\s*")
                .toRegex(RegexOption.IGNORE_CASE))
        subcommands.forEach { i -> i.patterns.forEach { j ->
            result.add(("\\s*$prefix$name\\s+${j.pattern.removePrefix("\\s*")}")
                .toRegex(RegexOption.IGNORE_CASE))}
        }
        patterns = result
    }

    class CommandBuilder(
            private val prefix  : String,
            private val name    : String) {
        private val arguments       : MutableList<Argument> = mutableListOf()
        private val callbacks       : MutableList<MessageCommand> = mutableListOf()
        private var permission      : PERMISSION = PERMISSION.USER
        private val conditions      : MutableList<CONDITION> = mutableListOf()
        private val subcommands     : MutableList<Command> = mutableListOf()
        fun <T : Any> argument(arg : String, type : KClass<T>) = apply {
            arguments.add(Argument(arg, null, type.simpleName ?: "null"))
        }
        fun callback(c: MessageCommand) = apply{ callbacks.add(c)}
        fun permission(p : PERMISSION) = apply { permission = p  }
        fun condition(c : CONDITION) = apply { conditions.add(c) }
        fun subcommand(c : Command) =
                apply { subcommands.add(c)  }
        fun build() = Command(prefix, name, Arguments(arguments), callbacks, permission, conditions, subcommands)
    }

    fun execute(event: MessageCreateEvent) {
        if (!event.member.isPresent) return
        val member = event.member.get()
        if(!checkPermissions(member)) return
        if(!checkConditions(member)) return
        var patternsMatched = 0
        patterns.forEach { if (it.matches(event.message.content)) patternsMatched++ }
        if(patternsMatched > 0 ) {
            val content = event.message.content.split(" ").map { it.trim() }
            if(!parseInput(member, content)) return
            subcommands.find { content[1].equals(it.prefix + it.name, ignoreCase = true) }
                ?.executeSubcommand(event, content, 1) ?: run {
                callbacks.forEach { it.invoke(event, arguments) }
            }
            LoggerFactory.getLogger(Command::class.simpleName)
                .info("Command [${event.message.content}] executed by @${member.displayName}")
            clearArguments()
        }
    }

    private fun executeSubcommand(event: MessageCreateEvent, content : List<String>, contentStart : Int) {
        subcommands.find { content[contentStart + 1].equals(it.prefix + it.name, ignoreCase = true) }
                ?.executeSubcommand(event, content, contentStart + 1) ?: run {
            callbacks.forEach { it.invoke(event, arguments) }
        }
    }

    private fun checkPermissions(member: Member) : Boolean {
        if(!permission.check(member)) {
            LoggerFactory.getLogger(Command::class.simpleName)
                    .warn("Member @${member.displayName} doesn't have permission [$permission]")
            return false
        }
        return true
    }

    private fun checkConditions(member: Member) : Boolean {
        var check = true
        conditions.forEach { check = check && it.check(member) }
        if(!check) {
            LoggerFactory.getLogger(Command::class.simpleName)
                    .warn("Member @${member.displayName} doesn't have conditions $conditions")
            return false
        }
        return true
    }

    private fun parseInput(member: Member, content : List<String>, contentStart : Int = 0) : Boolean {
        if(arguments.isEmpty() && content.size == contentStart + 1)
            return true
        return subcommands.find { content[contentStart + 1].equals(it.prefix + it.name, ignoreCase = true) }
                ?.parseInput(member, content, contentStart + 1) ?: run {
            for (i in arguments.indices) {
                arguments[i].value = content[contentStart + i + 1]
                if(!arguments[i].isValid())
                    return false
            }
            return true
        }
    }

    private fun clearArguments() {
        arguments.forEach {it.value = null}
        subcommands.forEach { it.clearArguments() }
    }
}