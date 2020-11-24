package org.beagle

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Consumer

interface EmbedCreation {
    fun generateEmbed() : Consumer<EmbedCreateSpec>
}

abstract class Module(val name: String, val isVisible: Boolean = true) : EmbedCreation{
    open fun initialize(bot: Bot) {
        commands.forEach {bot.addCommand(it)}
        LoggerFactory.getLogger(this::class.simpleName)
                .info("Initializing module ${this::class.simpleName}")
    }
    protected var commands : MutableList<Command> = mutableListOf()

    open fun description() : String {
        var description = ""
        commands.forEach { command ->
            fun addDescription(c : Command, s: String) {
                if (c.callbacks.isNotEmpty())
                    description += s +  c.prefix + c.name + c.arguments.joinToString { " [${it.name}]" } + "\n"
                c.subcommands.forEach { addDescription(it, "$s${c.prefix}${c.name} ") }
            }
            addDescription(command, "")
        }
        return description
    }
    override fun generateEmbed(): Consumer<EmbedCreateSpec> = Consumer { spec ->
        spec.setTitle("Узри описание для этого прекрасного $name")
        spec.setColor(Color.WHITE)
        spec.setDescription("```\n${description()}\n```")
    }
}

class Test : Module("Test") {
    override fun initialize(bot: Bot) {
        val subcommand1 = Command.CommandBuilder("", "ping")
                .argument("amount", Long::class)
                .callback { e, a ->
                    val amount : Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                    e.message.channel
                            .repeat(amount - 1)
                            .flatMap { it.createMessage("Pong") }
                            .subscribe()
                }
                .build()

        val subcommand2 = Command.CommandBuilder("", "pong")
            .argument("amount", Long::class)
            .callback { e, a ->
                val amount : Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                e.message.channel
                    .repeat(amount - 1)
                    .flatMap { it.createMessage("Ping") }
                    .subscribe()
            }
            .build()

        val command = Command.CommandBuilder(bot.prefix, "test")
                .callback { e, a ->
                    val amount : Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
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

class HelpModule : Module("Help", true) {
    override fun initialize(bot: Bot) {
        commands.add(Command.CommandBuilder(bot.prefix, "help")
            .callback { e, _ ->
                e.message.channel
                    .flatMap { it.createEmbed(bot.generateEmbed()) }
                    .subscribe()
            }
            .build())

        commands.add(Command.CommandBuilder(bot.prefix, "help")
            .argument("module name", String::class)
            .callback { e, a ->
                bot.modules.find { it.name.equals(a["module name"]?.toStringOrNull(), ignoreCase = true) }?.let {
                    e.message.channel
                        .flatMap { channel -> channel.createEmbed(it.generateEmbed()) }
                        .subscribe()
                }
            }
            .build())
        super.initialize(bot)
    }
}

class Bot(private val token: String, val prefix: String) : EmbedCreation {
    private var client : GatewayDiscordClient? = null
    var modules : MutableList<Module> = mutableListOf()
        private set
    var commands : MutableList<Command> = mutableListOf()
        private set

    init {
        client = DiscordClientBuilder.create(token).build().login().block()
        client?.eventDispatcher?.on(ReadyEvent::class.java)?.subscribe {
            LoggerFactory.getLogger(Bot::class.simpleName)
                    .info("Logged in as ${it.self.username}, ${it.self.discriminator}")
        }
    }
    fun addModule(module: Module) = apply { modules.add(module) }
    fun addCommand(command: Command) = apply { this.commands.add(command) }

    fun start() {
        modules.forEach { it.initialize(this) }
        LoggerFactory.getLogger(this::class.simpleName)
                .info("Starting bot")
        client?.eventDispatcher?.on(MessageCreateEvent::class.java)
            ?.flatMap { event -> Mono.just(event.message)
                .filter { it.author.map { !it.isBot }.orElse(false) }
                .filter { it.content.startsWith(prefix)}
                .flatMap { Flux.fromIterable(commands)
                    .map { it.execute(event) }
                    .then() } }
            ?.subscribe() ?:
                LoggerFactory.getLogger(Bot::class.simpleName)
                    .warn("Failed to add commands $commands")
        client?.onDisconnect()?.block()
    }

    override fun generateEmbed(): Consumer<EmbedCreateSpec> = Consumer { spec ->
        spec.setTitle("Потрогай каждую команду моих модулей, давай же")
        spec.setColor(Color.WHITE)
        modules.forEach { if(it.isVisible) { spec.addField(it.name, "```\n${it.description()}\n```", false) }}
    }
}

data class Config(
    val token: String,
    val prefix: String
) {


}