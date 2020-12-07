package org.beagle

import org.beagle.modules.HelpModule
import org.beagle.modules.TestModule
import org.beagle.modules.UtilityModule
import org.jetbrains.exposed.sql.Database

fun main() {
    val url = "jdbc:postgresql://localhost:5432/test?user=postgres&password=1"
    val t = Database.connect(url)

    val token = ""
    val prefix = "."
    Bot(token, prefix)
        .addModule(HelpModule())
        .addModule(TestModule())
        .addModule(UtilityModule())
        .start()
}