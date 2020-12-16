package org.imrs776.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent

class ChooseCommand(superCategory: Category) : Command() {
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