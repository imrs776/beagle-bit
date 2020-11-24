package org.beagle

fun main() {
    val token = ""
    val prefix = "-"
    Bot(token, prefix)
        .addModule(HelpModule())
        .addModule(Test())
        .start()
}
