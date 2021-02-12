package org.imrs776.commands.roles

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import org.imrs776.Database
import org.imrs776.abstracts.BaseRolesCommand
import org.imrs776.modules.RolesModule
import org.jetbrains.exposed.sql.transactions.transaction

class RolesInitializeCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "init"
        help = "initialize roles message"
        arguments = "<channel>"
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
    }

    private val mentionChannelRegex = Regex("^<#&?(\\d+)>\$")
    override fun execute(event: CommandEvent) {
        val match: MatchResult? = mentionChannelRegex.find(event.args)
        if (event.args.isEmpty() || match == null)
            event.reactWarning()
        else {
            val channel = event.guild.getTextChannelById(match.groups[1]!!.value)
            if (channel == null)
                event.reactWarning()
            else {
                val embed = generateEmbed(event)
                if (embed == null)
                    event.reactWarning()
                else
                    channel.sendMessage(embed).queue()
            }
        }
        super.execute(event)
    }

    private fun generateEmbed(event: CommandEvent): MessageEmbed? {
        val embed = EmbedBuilder()
        embed.setTitle("To get access to certain server channels, put the appropriate reactions")
        embed.setColor((0..0xffffff).random())
        val guildID = event.guild.idLong
        val stringBuilder = StringBuilder()
        stringBuilder.append("=========================================================\n")
        val success = transaction {
            val guild = Database.Guild.findById(guildID) ?: return@transaction false
            for (manageableRole in guild.roles) {
                val role = event.guild.getRoleById(manageableRole.id.value) ?: continue
                val emote = event.guild.getEmoteById(manageableRole.emojiID) ?: continue
                stringBuilder.append(emote.asMention)
                stringBuilder.append(" " + (manageableRole.name ?: role.name))
                stringBuilder.append(" " + (manageableRole.description ?: ""))
                stringBuilder.append("\n")
            }
            return@transaction true
        }
        embed.setDescription(stringBuilder.toString())
        return if (success) embed.build() else null
    }
}