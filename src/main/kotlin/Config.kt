package org.beagle
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Config(
    var token: String,
    var prefix: String,
    var databaseUrl: String,
)
