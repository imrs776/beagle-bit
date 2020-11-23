package org.beagle

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface Module {
    fun initialize(bot : Bot) {
        LoggerFactory.getLogger(this::class.simpleName)
                .info("Initializing module ${this::class.simpleName}")
    }
}

class PingPongModule : Module {
    override fun initialize(bot : Bot) {
        super.initialize(bot)

        val subcommand = Command.CommandBuilder("", "ping")
                .argument("amount", Long::class)
                .argument("amount2", Long::class)
                .callback { e, a ->
                    val amount : Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                    e.message.channel
                            .repeat(amount - 1)
                            .flatMap { it.createMessage("Pong") }
                            .subscribe()
                }
                .build()

        val command = Command.CommandBuilder(bot.prefix, "test")
                .argument("amount", Long::class)
                .argument("amount2", Long::class)
                .callback { e, a ->
                    val amount : Long = java.lang.Long.min(a["amount"]?.toLongOrNull() ?: return@callback, 5)
                    e.message.channel
                            .repeat(amount - 1)
                            .flatMap { it.createMessage("Pong") }
                            .subscribe()
                }
                .subcommand(subcommand)
                .permission(Permission.MODERATOR)
                .condition(Condition.IN_VOICE)
                .build()
        bot.addCommand(command)
    }
}

class Bot(private val token: String, val prefix : String) {
    private var client : GatewayDiscordClient? = null
    private var modules : MutableList<Module> = mutableListOf()
    init {
        client = DiscordClientBuilder.create(token).build().login().block()
        client?.eventDispatcher?.on(ReadyEvent::class.java)?.subscribe {
            LoggerFactory.getLogger(Bot::class.simpleName)
                    .info("Logged in as ${it.self.username}, ${it.self.discriminator}")
        }
    }
    fun addModule(module : Module) = apply { modules.add(module) }
    fun addCommand(commands: Iterable<Command>)  = apply {
        client?.eventDispatcher?.on(MessageCreateEvent::class.java)
                ?.flatMap { event -> Mono.just(event.message)
                        .filter { it.author.map { !it.isBot }.orElse(false) }
                        .filter { it.content.startsWith(prefix)}
                        .flatMap { Flux.fromIterable(commands)
                                .map { it.execute(event) }
                                .next() } }
                ?.subscribe() ?:
        LoggerFactory.getLogger(Bot::class.simpleName)
                .warn("Failed to add commands $commands")
    }
    fun addCommand(command: Command) = apply {
        client?.eventDispatcher?.on(MessageCreateEvent::class.java)
                ?.flatMap { event -> Mono.just(event.message)
                        .filter { it.author.map { !it.isBot }.orElse(false) }
                        .filter { it.content.startsWith(prefix)}
                        .map { it.content.removePrefix(prefix) }
                        .map { command.execute(event) } }
                ?.subscribe() ?:
        LoggerFactory.getLogger(Bot::class.simpleName)
                .warn("Failed to add command $command")
    }
    fun start() {
        modules.forEach { it.initialize(this) }
        LoggerFactory.getLogger(this::class.simpleName)
                .info("Starting bot")
        client?.onDisconnect()?.block()
    }
}

data class Config (
        val token : String,
        val prefix: String
) {


}