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

class MemeCommand(superCategory: Category) : Command() {
    init {
        category = superCategory
        name = "meme"
        aliases = arrayOf("reddit")
        help = "generate meme"
        botPermissions = arrayOf<Permission>(Permission.MESSAGE_EMBED_LINKS)
        arguments = "<subreddit>"
        guildOnly = false

    }

    override fun execute(event: CommandEvent) {
        if (event.author.isBot) return
        val embed = EmbedBuilder()
        embed.setColor((0..0xffffff).random())
        Unirest.get("https://meme-api.herokuapp.com/gimme/" + event.args).asJsonAsync(object : Callback<JsonNode> {
            override fun completed(response: HttpResponse<JsonNode>) {
                when {
                    response.code != 200 -> event.replyError(response.body.`object`.getString("message"))
                    event.args.toIntOrNull() != null -> event.replyError("This subreddit does not exist.")
                    else -> event.reply(
                        EmbedBuilder()
                            .setColor((0..0xffffff).random())
                            .setDescription(
                                "Here is random post from reddit: "
                                        + response.body.`object`.getString("postLink")
                            )
                            .setImage(response.body.`object`.getString("url"))
                            .build()
                    )
                }
            }

            override fun failed(e: UnirestException) = cancelled()
            override fun cancelled() = event.replyError("Meme source is not available...");
        })
    }
}