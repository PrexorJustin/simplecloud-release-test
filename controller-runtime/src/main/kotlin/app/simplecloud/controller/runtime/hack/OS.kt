package app.simplecloud.controller.runtime.hack

enum class OS(val names: List<String>) {
    WINDOWS(listOf("windows")),
    LINUX(listOf("linux")),
    MAC(listOf("mac"));

    companion object {
        fun get(): OS? {
            val name = System.getProperty("os.name").lowercase()
            entries.forEach {
                if (it.names.any { osName -> name.contains(osName) }) {
                    return it
                }
            }
            return null
        }
    }
}
