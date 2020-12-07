package org.beagle.utility

import discord4j.common.util.Snowflake
import discord4j.rest.util.Color

object Helper {
    private val regexMention = Regex("^<@!?&?(\\d+)>\$")
    fun checkIfMention(value: String) = regexMention.matches(value)
    fun getMentionId(value: String) = regexMention.find(value)?.groupValues?.get(1)?.let { Snowflake.of(it) }
    fun getRandomColor(): Color = Color.of((0..0x1000000).random())
}