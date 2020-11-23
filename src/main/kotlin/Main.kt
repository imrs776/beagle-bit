package org.beagle

fun main() {
    val token = "NzE4Nzg5NTcyNzI4OTc5NTM5.Xtt-9A.kuIz6DvobcOVs1qjhHaNiD3WkbU"
    val prefix = "!"
    Bot(token, prefix)
            .addModule(PingPongModule())
            .start()
}
