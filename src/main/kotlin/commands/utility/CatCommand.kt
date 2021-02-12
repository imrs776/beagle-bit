package org.imrs776.commands.utility

import com.jagrosh.jdautilities.command.CommandEvent
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.async.Callback
import com.mashape.unirest.http.exceptions.UnirestException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import org.imrs776.abstracts.BaseUtilityCommand
import org.imrs776.modules.UtilityModule

class CatCommand(module: UtilityModule) : BaseUtilityCommand(module) {
    init {
        name = "cat"
        aliases = arrayOf("meow")
        help = "shows a random cat"
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        Unirest.get("https://aws.random.cat/meow").asJsonAsync(object : Callback<JsonNode> {
            override fun completed(hr: HttpResponse<JsonNode>) {
                event.reply(
                    EmbedBuilder()
                        .setColor((0..0xffffff).random())
                        .setDescription("Here is random `cat`")
                        .setImage(hr.body.getObject().getString("file"))
                        .build(),
                    { if (event.isFromType(ChannelType.TEXT)) event.reactSuccess() },
                    { if (event.isFromType(ChannelType.TEXT)) event.reactError() }
                )
            }

            override fun failed(e: UnirestException) = cancelled()
            override fun cancelled() = event.replyError("Cats source is not available...");
        })
        super.execute(event)
    }
}