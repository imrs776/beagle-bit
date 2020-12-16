package org.imrs776.modules

import com.jagrosh.jdautilities.command.Command
import org.imrs776.abstracts.BaseModule
import org.imrs776.commands.utility.CatCommand
import org.imrs776.commands.utility.ChooseCommand
import org.imrs776.commands.utility.HugCommand
import org.imrs776.commands.utility.MemeCommand


class UtilityModule : BaseModule(Command.Category("Utility")) {
    override val commands: Array<Command> = arrayOf(
        CatCommand(this),
        MemeCommand(this),
        HugCommand(this),
        ChooseCommand(this)
    )
}

