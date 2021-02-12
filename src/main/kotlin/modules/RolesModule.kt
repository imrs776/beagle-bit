package org.imrs776.modules

import com.jagrosh.jdautilities.command.Command
import org.imrs776.abstracts.BaseModule
import org.imrs776.commands.roles.RolesCommand

class RolesModule : BaseModule(Command.Category("Roles management")) {
    override val commands: Array<Command> = arrayOf(
        RolesCommand(this)
    )


}