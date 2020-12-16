package org.imrs776.modules

import com.jagrosh.jdautilities.command.Command
import org.imrs776.commands.CatCommand
import org.imrs776.commands.ChooseCommand
import org.imrs776.commands.HugCommand
import org.imrs776.commands.MemeCommand


class UtilityModule : BaseModule(Command.Category("Utility")) {
    override val commands: Array<Command> = arrayOf(
        CatCommand(category),
        MemeCommand(category),
        HugCommand(category),
        ChooseCommand(category)
    )
}

