package org.imrs776

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

object Config {
    fun load(file: File): ConfigData? {
        return if (!file.createNewFile()) {
            try {
                val data: ConfigData = Json.decodeFromString(file.readText())
                LoggerFactory.getLogger(Config::class.simpleName)
                    .info("Configuration file config.json has been loaded:\n$data ")
                data
            } catch (e: Exception) {
                LoggerFactory.getLogger(Config::class.java.simpleName)
                    .error(e.stackTraceToString())
                null
            }
        } else {
            val string = Json.encodeToString(ConfigData("", "", "!", "", "", "", "", "", ""))
            file.writeText(string)
            LoggerFactory.getLogger(Config::class.java.simpleName)
                .info("Configuration file config.json has been created ")
            null
        }
    }

    @Serializable
    data class ConfigData(
        val token: String,
        val prefix: String,
        val databaseName: String,
        val databaseUser: String,
        val databasePassword: String,
        val databaseServiceHost: String,
        val databaseServicePort: String,
        val databaseDefault: String,
        val ownerId: String
    )
}