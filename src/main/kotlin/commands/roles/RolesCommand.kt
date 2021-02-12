package org.imrs776.commands.roles

import net.dv8tion.jda.api.Permission
import org.imrs776.abstracts.BaseRolesCommand
import org.imrs776.modules.RolesModule


class RolesCommand(module: RolesModule) : BaseRolesCommand(module) {
    init {
        name = "roles"
        userPermissions = arrayOf(Permission.ADMINISTRATOR)
        children = arrayOf(RolesInitializeCommand(module), RolesAddCommand(module), RolesSetCommand(module))
        hidden = true
    }
}