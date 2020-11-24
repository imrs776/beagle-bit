package org.beagle

import org.beagle.modules.HelpModule
import org.beagle.modules.TestModule

fun main() {
    val token = "NzE4Nzg5NTcyNzI4OTc5NTM5.Xtt-9A.xPxxfqlYGp1W5or_8wAuoH2kbvc"
    val prefix = "-"
    Bot(token, prefix)
        .addModule(HelpModule())
        .addModule(TestModule())
        .start()
}
