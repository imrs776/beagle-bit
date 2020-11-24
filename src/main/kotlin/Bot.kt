package org.beagle

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.beagle.modules.Module
import org.beagle.utility.Command
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Consumer

class Bot(private val token: String, val prefix: String) {
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

    fun generateEmbed(): Consumer<EmbedCreateSpec> = Consumer { spec ->
        spec.setTitle("Потрогай каждую команду моих модулей, давай же")
        spec.setColor(Color.WHITE)
        modules.forEach { if(it.isVisible) { spec.addField(it.name, "```\n${it.description()}\n```", false) }}
    }
}