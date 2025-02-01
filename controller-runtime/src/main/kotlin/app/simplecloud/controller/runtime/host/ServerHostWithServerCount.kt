package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.shared.host.ServerHost

data class ServerHostWithServerCount(
    val serverHost: ServerHost,
    val serverCount: Int
)
