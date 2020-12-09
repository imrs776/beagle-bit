package org.imrs776.modules

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.async.Callback
import com.mashape.unirest.http.exceptions.UnirestException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission


class UtilityModule : BaseModule(Command.Category("Utility")) {
    override val commands: Array<Command> = arrayOf(
        CatCommand(category),
        MemeCommand(category),
        HugCommand(category),
        ChooseCommand(category)
    )

    private class HugCommand(superCategory: Category) : Command() {
        init {
            category = superCategory
            name = "hug"
            aliases = arrayOf("love", "kiss")
            help = "hugs someone <3"
            arguments = "<item> <item> ..."
            botPermissions = arrayOf<Permission>(Permission.MESSAGE_EMBED_LINKS)
        }

        val mentionRegex = Regex("^<@!?&?(\\d+)>\$")
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

    private class MemeCommand(superCategory: Category) : Command() {
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

    private class CatCommand(superCategory: Category) : Command() {
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

    private class ChooseCommand(superCategory: Category) : Command() {
        init {
            category = superCategory
            name = "choose"
            help = "make a decision"
            arguments = "<item> <item> ..."
            guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            if (event.args.isEmpty()) {
                event.replyWarning("You didn't give me any choices!")
            } else {
                val items = event.args.split("\\s+".toRegex())
                if (items.size == 1)
                    event.replyWarning("You only gave me one option, `" + items[0] + "`")
                else
                    event.replySuccess("I choose `" + items[(Math.random() * items.size).toInt()] + "`")
            }
        }
    }
}

