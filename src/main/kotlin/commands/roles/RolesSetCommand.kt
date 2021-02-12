package org.imrs776.commands.roles

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.Permission
import org.imrs776.Database
import org.imrs776.abstracts.BaseRolesCommand
import org.imrs776.modules.RolesModule
import org.jetbrains.exposed.sql.transactions.transaction

class RolesSetCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "set"
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
        children = arrayOf(RolesSetNameCommand(module), RolesSetDescriptionCommand(module))
        hidden = true
    }
}

class RolesSetNameCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "name"
        help = "set name of role"
        arguments = "<role> <name>"
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
    }

    private val commandRegex = Regex("^<@&(\\d+)>\\s+(.+)\$")
    override fun execute(event: CommandEvent) {
        val match: MatchResult? = commandRegex.find(event.args)
        if (event.args.isEmpty() || match == null)
            event.reactWarning()
        else {
            val role = event.guild.getRoleById(match.groups[1]!!.value)
            if (role != null) {
                transaction {
                    val databaseRole = Database.Role.findById(role.idLong)
                    if (databaseRole != null) 
                        databaseRole.name = match.groups[2]!!.value
                    else
                        event.reactError()
                }
            } else
                event.reactWarning()
        }
    }
}

class RolesSetDescriptionCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "desc"
        help = "set description of role"
        arguments = "<role> <description>"
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
    }

    private val commandRegex = Regex("^<@&(\\d+)>\\s+(.+)\$")
    override fun execute(event: CommandEvent) {
        val match: MatchResult? = commandRegex.find(event.args)
        if (event.args.isEmpty() || match == null)
            event.reactWarning()
        else {
            val role = event.guild.getRoleById(match.groups[1]!!.value)
            if (role != null) {
                transaction {
                    val databaseRole = Database.Role.findById(role.idLong)
                    if (databaseRole != null)
                        databaseRole.description = match.groups[2]!!.value
                    else
                        event.reactError()
                }
            } else
                event.reactWarning()
        }
    }
}