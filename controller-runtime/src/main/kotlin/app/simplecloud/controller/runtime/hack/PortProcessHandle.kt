package app.simplecloud.controller.runtime.hack

import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerDefinition
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object PortProcessHandle {

    private const val WINDOWS_PID_INDEX = 3
    private val windowsPattern = Pattern.compile("\\s*TCP\\s+\\S+:(\\d+)\\s+\\S+:(\\d+)\\s+\\S+\\s+(\\d+)")

    private val preBindPorts = ConcurrentHashMap<Int, LocalDateTime>()

    fun of(port: Int): Optional<ProcessHandle> {
        val os = OS.get() ?: return Optional.empty()
        val command = when (os) {
            OS.LINUX, OS.MAC -> arrayOf("sh", "-c", "lsof -i :$port | awk '{print \$2}'")
            OS.WINDOWS -> arrayOf("cmd", "/c", "netstat -ano | findstr $port")
        }

        val process = Runtime.getRuntime().exec(command)

        val bufferedReader = process.inputReader()
        val processId = bufferedReader.useLines { lines ->
            lines.firstNotNullOfOrNull { parseProcessIdOrNull(os, it) }
        }

        if (processId == null) {
            return Optional.empty()
        }

        return ProcessHandle.of(processId)
    }

    private fun parseProcessIdOrNull(os: OS, line: String): Long? {
        return when (os) {
            OS.LINUX, OS.MAC -> line.toLongOrNull()
            OS.WINDOWS -> {
                val matcher = windowsPattern.matcher(line)
                if (!matcher.matches()) {
                    return null
                }

                return matcher.group(WINDOWS_PID_INDEX).toLongOrNull()
            }
        }
    }

    @Synchronized
    fun findNextFreePort(startPort: Int, serverDefinition: ServerDefinition): Int {
        val server = Server.fromDefinition(serverDefinition)
        var port = startPort
        val time = LocalDateTime.now()
        while (isPortBound(port)) {
            port++
        }
        addPreBind(port, time, server.properties.getOrDefault("max-startup-seconds", "120").toLong())
        return port
    }

    private fun addPreBind(port: Int, time: LocalDateTime, duration: Long) {
        preBindPorts[port] = time.plusSeconds(duration)
    }

    fun isPortBound(port: Int): Boolean {
        return !of(port).isEmpty || LocalDateTime.now().isBefore(preBindPorts.getOrDefault(port, LocalDateTime.MIN))
    }

    fun removePreBind(port: Int, forced: Boolean = false) {
        // Check if max-startup-seconds is reached and return if not
        if (!forced && LocalDateTime.now().isBefore(preBindPorts.getOrDefault(port, LocalDateTime.MAX))) {
            return
        }

        preBindPorts.remove(port)
    }

}