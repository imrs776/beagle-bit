package org.beagle

import discord4j.core.`object`.entity.Member
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import javax.lang.model.element.Element
import kotlin.reflect.KClass

typealias MessageCommand = (event: MessageCreateEvent, arguments : Arguments) -> Unit

class Argument internal constructor(
        val name : String,
        var value : String?,
        private val type : String){
    fun toStringOrNull() = checkCastType(this, String::class)
            .apply { this ?: logCastError<String>(this@Argument) }
    fun toByteOrNull()   = checkCastType(this, Byte::class)?.toByteOrNull()
            .apply { this ?: logCastError<Byte>(this@Argument) }
    fun toShortOrNull()   = checkCastType(this, Short::class)?.toShortOrNull()
            .apply { this ?: logCastError<Short>(this@Argument) }
    fun toIntOrNull()   = checkCastType(this, Int::class)?.toIntOrNull()
            .apply { this ?: logCastError<Int>(this@Argument) }
    fun toLongOrNull()   = checkCastType(this, Long::class)?.toLongOrNull()
            .apply { this ?: logCastError<Long>(this@Argument) }
    fun toFloatOrNull()   = checkCastType(this, Float::class)?.toFloatOrNull()
            .apply { this ?: logCastError<Float>(this@Argument) }
    fun toDoubleOrNull()   = checkCastType(this, Double::class)?.toDoubleOrNull()
            .apply { this ?: logCastError<Double>(this@Argument) }

    fun isValid() : Boolean {
        return when(type) {
            String::class.simpleName -> this.toStringOrNull() != null
            Byte::class.simpleName   -> this.toByteOrNull()   != null
            Short::class.simpleName  -> this.toShortOrNull()  != null
            Int::class.simpleName    -> this.toIntOrNull()    != null
            Long::class.simpleName   -> this.toLongOrNull()   != null
            Float::class.simpleName  -> this.toFloatOrNull()  != null
            Double::class.simpleName -> this.toDoubleOrNull() != null
            else -> false
        }
    }

    private fun <T : Any> isType(type : KClass<T>) = this.type == type.simpleName
    private inline fun <reified T> logCastError(arg : Argument) =
            LoggerFactory.getLogger(Argument::class.simpleName)
                    .warn("Invalid cast of [$arg] to ${T::class.simpleName}")
    private inline fun <reified T : Any> checkCastType(arg: Argument, type : KClass<T>) : String? =
            if (arg.isType(type)) arg.value else null

    override fun toString() : String = "($name):$type" + (value?.let { " = $value" } ?: "")
}

//class Arguments internal constructor(private val list : List<Argument>) {
//    operator fun get(string : String) : Argument? {
//        list.forEach { if (it.name == string ) return it }
//        LoggerFactory.getLogger(Arguments::class.simpleName)
//                .warn("Invalid argument [$string]")
//        return null
//    }
//    val indices = list.indices
//    fun forEach(action: (Argument) -> Unit) { list.forEach(action) }
//    operator fun get(i : Int) : Argument = list[i]
//    override fun toString() : String = toString().trimStart('[').trimEnd(']')
//    fun isEmpty() = list.isEmpty()
//}

class Arguments : ArrayList<Argument> {

    internal constructor() : super()
    internal constructor(list : List<Argument>) : super(list)

    operator fun get(string : String) : Argument? {
        forEach { if (it.name == string ) return it }
        LoggerFactory.getLogger(Arguments::class.simpleName)
            .warn("Invalid argument [$string]")
        return null
    }
    override fun toString() : String = super.toString().trimStart('[').trimEnd(']')
}

enum class Permission() {
    USER { override fun check(member: Member) = true },
    MODERATOR {
        override fun check(member: Member) =  ADMIN.check(member) ||
                member.basePermissions.block()?.contains(discord4j.rest.util.Permission.BAN_MEMBERS) ?: false
    },
    ADMIN {
        override fun check(member: Member) =
                member.basePermissions.block()?.contains(discord4j.rest.util.Permission.ADMINISTRATOR) ?: false
    },
    NEVER { override fun check(member: Member) = false };
    abstract fun check(member : Member) : Boolean
}

enum class Condition {
    IN_VOICE {
        override fun check(member: Member) = member.voiceState.hasElement().block() ?: false
    },
    NEVER { override fun check(member: Member) = false };
    abstract fun check(member : Member) : Boolean
}

class Command private constructor(
        val prefix: String,
        val name: String,
        val arguments : Arguments,
        val callbacks : List<MessageCommand>,
        val permission : Permission,
        val conditions : List<Condition>,
        val subcommands : List<Command>) {
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
            private val prefix: String,
            private val name: String) {
        private var arguments : MutableList<Argument> = mutableListOf()
        private var callbacks : MutableList<MessageCommand> = mutableListOf()
        private var permission : Permission = Permission.USER
        private var conditions : MutableList<Condition> = mutableListOf()
        private var subcommands : MutableList<Command> = mutableListOf()
        fun <T : Any> argument(arg : String, type : KClass<T>) = apply {
            arguments.add(Argument(arg, null, type.simpleName ?: "null"))
        }
        fun callback(c: MessageCommand) = apply{ callbacks.add(c)}
        fun permission(p : Permission) = apply { permission = p  }
        fun condition(c : Condition) = apply { conditions.add(c) }
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
            subcommands.find { content[1] == (it.prefix + it.name) }
                ?.executeSubcommand(event, content, 1) ?: run {
                callbacks.forEach { it.invoke(event, arguments) }
            }
            LoggerFactory.getLogger(Command::class.simpleName)
                .info("Command [${event.message.content}] executed by @${member.displayName}")
            clearArguments()
        }
    }

    private fun executeSubcommand(event: MessageCreateEvent, content : List<String>, contentStart : Int) {
        subcommands.find { content[contentStart + 1] == (it.prefix + it.name) }
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
        return subcommands.find { content[contentStart + 1] == (it.prefix + it.name) }
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

    override fun toString() =  "$name $arguments"
}
