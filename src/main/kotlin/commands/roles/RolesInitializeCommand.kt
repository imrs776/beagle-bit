package org.imrs776.commands.roles

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.Permission
import org.imrs776.abstracts.BaseRolesCommand
import org.imrs776.modules.RolesModule

class RolesInitializeCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "roles init"
        help = "initialize roles message"
        arguments = "<channel>"
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
    }

    override fun execute(event: CommandEvent) {


        super.execute(event)
    }

}