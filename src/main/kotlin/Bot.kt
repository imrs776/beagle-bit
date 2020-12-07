package org.beagle

import discord4j.common.util.Snowflake
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.beagle.modules.Module
import org.beagle.utility.CommandHandler
import org.beagle.utility.ReactionHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Consumer

class Bot(private val config: Config) {
    val prefix: String = config.prefix
    private var client: GatewayDiscordClient? = null
    private val modules: MutableList<Module> = mutableListOf()
    private val commandHandlers: MutableList<CommandHandler> = mutableListOf()
    private val reactionHandlers: MutableList<ReactionHandler> = mutableListOf()
    private var isValid = false

    init {
        val tempClient: GatewayDiscordClient?
        try {
            Database.connect(config.databaseUrl)
            transaction {
                SchemaUtils.create(Data.Users)
                SchemaUtils.create(Data.Guilds)
                SchemaUtils.create(Data.Members)
            }
            tempClient = DiscordClientBuilder.create(config.token).build().login().block()
            client = tempClient
            client?.eventDispatcher?.on(ReadyEvent::class.java)?.subscribe {
                LoggerFactory.getLogger(Bot::class.simpleName)
                    .info("Logged in as ${it.self.username}, ${it.self.discriminator} with config:\n$config")
            }
            client?.guilds?.map {
                transaction {
                    val id = it.id.asLong()
                    Data.Guild.findById(id) ?: Data.Guild.new(id) {
                        name = it.name
                        setting1 = false
                        setting2 = false
                    }
                }
            }?.blockLast()
            isValid = client != null
        } catch (e: Exception) {
            LoggerFactory.getLogger(Bot::class.simpleName)
                .error("Failed to initialize bot with config:\n $config")
            isValid = false
        }
    }

    fun addModule(module: Module) =
        apply { if (isValid) modules.add(module) }

    fun addCommandHandler(commandHandler: CommandHandler) =
        apply { if (isValid) commandHandlers.add(commandHandler) }

    fun addReactionHandler(reactionHandler: ReactionHandler) =
        apply { if (isValid) reactionHandlers.add(reactionHandler) }

    fun start() {
        if (isValid) {
            modules.forEach { it.initialize(this) }
            LoggerFactory.getLogger(this::class.simpleName)
                .info("Starting bot")
            initializeCommands()
            initializeReactions()
            client?.onDisconnect()?.block()
        }
    }

    private fun initializeCommands() {
        client?.eventDispatcher?.on(MessageCreateEvent::class.java)
            ?.flatMap { event ->
                Mono.just(event.message)
                    .filter { it.author.map { !it.isBot }.orElse(false) }
                    .filter { it.content.startsWith(prefix) }
                    .flatMap {
                        Flux.fromIterable(commandHandlers)
                            .map { it.execute(event) }
                            .then()
                    }
            }
            ?.subscribe() ?: LoggerFactory.getLogger(Bot::class.simpleName)
            .warn("Failed to add command handlers $commandHandlers")
    }

    private fun initializeReactions() {
        var result: Boolean = client?.eventDispatcher?.on(ReactionAddEvent::class.java)
            ?.flatMap { event ->
                Mono.just(event.message)
                    .flatMap {
                        Flux.fromIterable(reactionHandlers)
                            .map { it.execute(event) }
                            .then()
                    }
            }
            ?.subscribe() != null

        result = result && client?.eventDispatcher?.on(ReactionRemoveEvent::class.java)
            ?.flatMap { event ->
                Mono.just(event.message)
                    .flatMap {
                        Flux.fromIterable(reactionHandlers)
                            .map { it.execute(event) }
                            .then()
                    }
            }
            ?.subscribe() != null
        if (!result) LoggerFactory.getLogger(Bot::class.simpleName)
            .warn("Failed to add reaction handlers $reactionHandlers")
    }

    fun generateEmbed(): Consumer<EmbedCreateSpec> = Consumer { spec ->
        spec.setTitle("Потрогай каждую команду моих модулей, давай же")
        spec.setColor(Color.WHITE)
        modules.forEach {
            if (it.isVisible) {
                spec.addField(it.name, "```\n${it.description()}\n```", false)
            }
        }
    }

    fun getUserById(userId: Snowflake): User? {
        return client?.guilds?.flatMap { client?.getMemberById(it.id, userId) }?.blockFirst()
    }

    fun getUserByName(username: String): User? {
        return client?.users?.filter { it.username.equals(username, ignoreCase = true) }?.blockFirst()
    }
}