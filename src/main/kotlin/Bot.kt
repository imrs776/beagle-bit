package org.imrs776

import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.examples.command.PingCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import org.imrs776.abstracts.BaseModule

class Bot(config: Config.ConfigData, vararg modules: BaseModule) {
    private val waiter = EventWaiter()
    private val client = CommandClientBuilder()
    private val discordApi: JDA

    init {
        client.useDefaultGame()
        client.setOwnerId(config.ownerId)
        client.setPrefix(config.prefix)
        modules.forEach { client.addCommands(*it.commands) }
        client.addCommand(PingCommand())
        discordApi = JDABuilder.createDefault(config.token)
            .setStatus(OnlineStatus.DO_NOT_DISTURB)
            .addEventListeners(waiter, client.build())
            .build()
    }
}
