package org.beagle.utility

import discord4j.core.`object`.entity.Member

enum class PERMISSION() {
    USER {
        override fun check(member: Member) = true
    },
    MODERATOR {
        override fun check(member: Member) = ADMIN.check(member) ||
                member.basePermissions.block()?.contains(discord4j.rest.util.Permission.BAN_MEMBERS) ?: false
    },
    ADMIN {
        override fun check(member: Member) =
            member.basePermissions.block()?.contains(discord4j.rest.util.Permission.ADMINISTRATOR) ?: false
    },
    NEVER {
        override fun check(member: Member) = false
    };

    abstract fun check(member: Member): Boolean
}

enum class CONDITION {
    IN_VOICE {
        override fun check(member: Member) = member.voiceState.hasElement().block() ?: false
    },
    NEVER {
        override fun check(member: Member) = false
    };

    abstract fun check(member: Member): Boolean
}