package org.imrs776

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.imrs776.abstracts.BaseModule
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

class Bot(private val config: Config.ConfigData, vararg modules: BaseModule) : ListenerAdapter() {
    private val waiter = EventWaiter()
    private val client = CommandClientBuilder()
    private val discordApi: JDA
    private val modules = modules

    init {
        val databaseName = System.getenv(config.databaseName) ?: null
        val databaseUser = System.getenv(config.databaseUser) ?: null
        val databasePassword = System.getenv(config.databasePassword) ?: null
        val databaseServiceHost = System.getenv(config.databaseServiceHost) ?: null
        val databaseServicePort = System.getenv(config.databaseServicePort) ?: null

        val databaseUrl: String = if (databaseName == null ||
            databaseUser == null ||
            databasePassword == null ||
            databaseServiceHost == null ||
            databaseServicePort == null
        )
            config.databaseDefault
        else
            "jdbc:postgresql://${databaseServiceHost}:${databaseServicePort}/" +
                    "${databaseName}?user=${databaseUser}&password=${databasePassword}"

        org.jetbrains.exposed.sql.Database.connect(databaseUrl)
        transaction {
            SchemaUtils.create(Database.Users)
            SchemaUtils.create(Database.Guilds)
            SchemaUtils.create(Database.Members)
            SchemaUtils.create(Database.Roles)
        }

        client.setStatus(OnlineStatus.DO_NOT_DISTURB)
            .setOwnerId(config.ownerId)
            .setPrefix(config.prefix)
            .setHelpConsumer { help(it) }
            .setEmojis("\uD83D\uDE03", "\uD83D\uDE2E", "\uD83D\uDE26")

        modules.forEach { client.addCommands(*it.commands) }
        discordApi = JDABuilder.createDefault(config.token)
            .addEventListeners(waiter, client.build())
            .addEventListeners(this)
            .build()
    }

    override fun onReady(event: ReadyEvent) {
        transaction {
            discordApi.guilds.forEach { guild ->
                val memberList = mutableListOf<Database.User>()
                guild.members.forEach { member ->
                    val memberID = member.id.toLong()
                    memberList.add(Database.User.findById(memberID) ?: Database.User.new(memberID) {
                        name = member.user.name
                    })
                }
                val guildID = guild.id.toLong()
                Database.Guild.findById(guildID) ?: Database.Guild.new(guildID) {
                    name = guild.name
                    members = SizedCollection(memberList)
                }
            }
        }
    }

    private fun help(event: CommandEvent) {
        fun commandHelp(prefix: String, command: Command): String {
            val builder = StringBuilder()
            if (!command.isHidden && (!command.isOwnerCommand || event.isOwner))
                builder.append("\n")
                    .append(prefix)
                    .append(command.name)
                    .append(if (command.arguments == null) "" else " " + command.arguments)
                    .append(" - ").append(command.help)
            for (child in command.children)
                builder.append(commandHelp(prefix + command.name + " ", child))
            return builder.toString()
        }

        val embed = EmbedBuilder()
            .setColor((0..0xffffff).random())
            .setTitle("**${event.selfUser.name}** commands:")
        for (module in modules) {
            var moduleBuffer = ""
            for (command in module.commands)
                moduleBuffer += commandHelp(config.prefix, command)
            if (moduleBuffer.isNotEmpty()) {
                moduleBuffer.trim('\n')
                embed.addField(module.category.name, "```$moduleBuffer```", false)
            }
        }
        event.jda.getUserById(config.ownerId)?.let {
            embed.setFooter("\n\nFor additional help, contact ${it.name}#${it.discriminator}")
        }
        event.replyInDm(embed.build(),
            { if (event.isFromType(ChannelType.TEXT)) event.reactSuccess() },
            { event.replyWarning("Help cannot be sent because you are blocking Direct Messages.") })
    }
}