package app.simplecloud.controller.api.dsl.builders

import app.simplecloud.controller.api.dsl.markers.PropertyDsl

@PropertyDsl
class PropertyBuilder {
    private val properties = mutableMapOf<String, String>()

    operator fun String.unaryPlus() = this

    operator fun Pair<String, String>.unaryPlus() {
        properties[first] = second
    }

    infix fun String.to(value: String) {
        properties[this] = value
    }

    fun property(key: String, value: String) {
        properties[key] = value
    }

    fun properties(vararg pairs: Pair<String, String>) {
        properties.putAll(pairs)
    }

    internal fun build(): Map<String, String> = properties.toMap()
}
