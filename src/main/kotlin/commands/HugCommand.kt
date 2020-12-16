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

class HugCommand(superCategory: Category) : Command() {
    init {
        category = superCategory
        name = "hug"
        aliases = arrayOf("love", "kiss")
        help = "hugs someone <3"
        arguments = "<item> <item> ..."
        botPermissions = arrayOf<Permission>(Permission.MESSAGE_EMBED_LINKS)
    }

    private val mentionRegex = Regex("^<@!?&?(\\d+)>\$")
    override fun execute(event: CommandEvent) {
        if (event.author.isBot) return
        val args = event.args.split("\\s+".toRegex())
        if (event.args.isEmpty() || args.isEmpty()) {
            event.replyWarning("There is nothing to hug...");
        } else {
            val thingsToHug = mutableListOf<String>()
            args.forEach { argument ->
                thingsToHug.add(
                    if (argument.matches(mentionRegex)) argument else
                        event.guild.members
                            .find { it.effectiveName.equals(argument, ignoreCase = true) }?.asMention ?: argument
                )
            }
            Unirest.get("https://some-random-api.ml/animu/hug").asJsonAsync(object : Callback<JsonNode> {
                override fun completed(response: HttpResponse<JsonNode>) {
                    event.reply(
                        EmbedBuilder()
                            .setColor((0..0xffffff).random())
                            .setDescription(
                                event.author.asMention
                                        + " hugs `${thingsToHug.joinToString(separator = " and ")}`"
                            )
                            .setImage(response.body.`object`.getString("link"))
                            .build()
                    )
                }

                override fun failed(e: UnirestException) = cancelled()
                override fun cancelled() = event.replyError("Hugs is not available...");
            })
        }
    }
}