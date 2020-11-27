package org.beagle

import org.beagle.modules.HelpModule
import org.beagle.modules.TestModule

fun main() {
    val token = ""
    val prefix = "-"
    Bot(token, prefix)
        .addModule(HelpModule())
        .addModule(TestModule())
        .start()
}
