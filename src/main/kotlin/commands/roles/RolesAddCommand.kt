package org.imrs776.commands.roles

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.Permission
import org.imrs776.Database
import org.imrs776.abstracts.BaseRolesCommand
import org.imrs776.modules.RolesModule
import org.jetbrains.exposed.sql.transactions.transaction

class RolesAddCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "add"
        help = "add to roles manage new role"
        arguments = "<emoji> <role>"
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
    }

    private val commandRegex = Regex("^<:\\S+:(\\d+)>\\s+<@&(\\d+)>\$")
    override fun execute(event: CommandEvent) {
        val match: MatchResult? = commandRegex.find(event.args)
        if (event.args.isEmpty() || match == null)
            event.reactWarning()
        else {
            val role = event.guild.getRoleById(match.groups[2]!!.value)
            val emote = event.guild.getEmoteById(match.groups[1]!!.value)
            if (role != null && emote != null)
                transaction {
                    val databaseGuild = Database.Guild.findById(event.guild.idLong)
                    if (databaseGuild != null)
                        Database.Role.new(role.idLong) { emojiID = emote.idLong; guild = databaseGuild }
                    else
                        event.reactError()
                }
            else
                event.reactWarning()
        }
        super.execute(event)
    }
}