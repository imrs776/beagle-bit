package org.imrs776.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.async.Callback
import com.mashape.unirest.http.exceptions.UnirestException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission

class CatCommand(superCategory: Category) : Command() {
    init {
        category = superCategory
        name = "cat"
        aliases = arrayOf("meow")
        help = "shows a random cat"
        botPermissions = arrayOf<Permission>(Permission.MESSAGE_EMBED_LINKS)
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
                        .build()
                )
            }

            override fun failed(e: UnirestException) = cancelled()
            override fun cancelled() = event.replyError("Cats source is not available...");
        })
    }

}