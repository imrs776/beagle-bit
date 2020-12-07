package org.beagle

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table

object Data {
    object Users : LongIdTable() {
        val name = varchar("name", 50)
        override val primaryKey = PrimaryKey(id)
    }

    class User(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<User>(Users)

        var name by Users.name
    }

    object Guilds : LongIdTable() {
        val name = varchar("name", 50)
        val setting1 = bool("setting1")
        val setting2 = bool("setting2")
        override val primaryKey = PrimaryKey(id)
    }

    class Guild(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Guild>(Guilds)

        var name by Guilds.name
        var setting1 by Guilds.setting1
        var setting2 by Guilds.setting2
        var users by User via Members
    }

    object Members : Table() {
        val user = reference("user", Users)
        val guild = reference("guild", Guilds)
        override val primaryKey = PrimaryKey(user, guild) // PK_StarWarsFilmActors_swf_act is optional here
    }
}

