package org.imrs776

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table

object Database {
    object Guilds : LongIdTable() {
        val name = varchar("name", 50)
        override val primaryKey = PrimaryKey(id)
    }

    class Guild(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Guild>(Guilds)

        var name by Guilds.name
        var members by User via Members
        val roles by Role referrersOn Roles.guild
    }

    object Users : LongIdTable() {
        val name = varchar("name", 32).nullable()
        override val primaryKey = PrimaryKey(id)
    }

    class User(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<User>(Users)

        var name by Users.name
    }

    object Members : Table() {
        val user = reference("user", Users)
        val guild = reference("guild", Guilds)
        override val primaryKey = PrimaryKey(user, guild)
    }

    object Roles : LongIdTable() {
        val guild = reference("guild", Guilds)
        val emojiID = long("emojiID")

        val name = varchar("name", 50).nullable()
        val description = varchar("description", 200).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    class Role(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Role>(Roles)

        var guild by Guild referencedOn Roles.guild
        var emojiID by Roles.emojiID

        var name by Roles.name
        var description by Roles.description
    }
}