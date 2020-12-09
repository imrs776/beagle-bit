package org.imrs776

import org.imrs776.modules.UtilityModule
import java.io.File


fun main() {
    val config = Config.load(File("config.json"))
    if (config != null) {
        Bot(config, UtilityModule())
    }
}