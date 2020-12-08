package org.beagle.modules

import discord4j.core.spec.EmbedCreateSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.beagle.Bot
import org.beagle.utility.CommandHandler
import org.beagle.utility.Helper
import reactor.core.publisher.Mono
import java.net.URL
import java.util.function.Consumer

class UtilityModule : Module("Utility", true) {
    override fun initialize(bot: Bot) {
        val urlHugImageApi = URL("https://some-random-api.ml/animu/hug")
        val urlMemeImageApi = URL("https://some-random-api.ml/meme")
        commandHandlers.add(
            CommandHandler.CommandHandlerBuilder(bot.prefix, "hug")
                .argument("member name", String::class)
                .callback { e, a ->
                    val name = a["member name"]?.toStringOrNull() ?: return@callback Mono.empty()
                    if (!e.member.isPresent) return@callback Mono.empty()
                    var mention = name
                    if (!Helper.checkIfMention(name))
                        mention = bot.getUserByName(name)?.mention ?: name

                    val json = Json.parseToJsonElement(urlHugImageApi.readText())
                    val link = json.jsonObject["link"].toString().trimStart('"').trimEnd('"')
                    val embed: Consumer<EmbedCreateSpec> = Consumer {
                        it.setDescription(e.member.get().mention + " hugs **$mention**")
                        it.setImage(link)
                        it.setColor(Helper.getRandomColor())
                    }
                    e.message.channel
                        .flatMap { it.createEmbed(embed) }
                        .then()
                }
                .build())

        commandHandlers.add(
            CommandHandler.CommandHandlerBuilder(bot.prefix, "meme")
                .callback { e, _ ->
                    val json = Json.parseToJsonElement(urlMemeImageApi.readText())
                    val link = json.jsonObject["image"].toString().trimStart('"').trimEnd('"')
                    val category = json.jsonObject["category"].toString().trimStart('"').trimEnd('"')
                    val embed: Consumer<EmbedCreateSpec> = Consumer {
                        it.setDescription("Here is random meme from category: **$category**")
                        it.setImage(link)
                        it.setColor(Helper.getRandomColor())
                    }
                    e.message.channel
                        .flatMap { it.createEmbed(embed) }
                        .then()
                }
                .build())

        super.initialize(bot)
    }
}