package org.beagle

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.beagle.modules.HelpModule
import org.beagle.modules.TestModule
import org.beagle.modules.UtilityModule
import org.slf4j.LoggerFactory
import java.io.File

fun main() {
    val file = File("config.json")
    var config: Config?
    if (!file.createNewFile()) {
        config = try {
            Json.decodeFromString(file.readText())
        } catch (e: Exception) {
            null
        }
        if (config == null || config.token.isEmpty() || config.databaseUrl.isEmpty())
            config = null
    } else {
        val string = Json.encodeToString(Config("", "", ""))
        file.writeText(string)
        LoggerFactory.getLogger(Bot::class.simpleName)
            .info("Configuration file config.json has been created ")
        return
    }
    if (config == null) {
        LoggerFactory.getLogger(Bot::class.simpleName)
            .error("Configuration file contains invalid values")
        return
    }

    Bot(config)
        .addModule(HelpModule())
        .addModule(TestModule())
        .addModule(UtilityModule())
        .start()
}