package app.simplecloud.controller.api.dsl.extensions

import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.shared.group.Group

suspend fun GroupApi.Coroutine.createGroup(
    block: GroupCreateBuilder.() -> Unit
): Group {
    val builder = GroupCreateBuilder().apply(block)
    return createGroup(builder.build())
}

suspend fun GroupApi.Coroutine.updateGroup(
    block: GroupUpdateBuilder.() -> Unit
): Group {
    val builder = GroupUpdateBuilder().apply(block)
    return updateGroup(builder.build())
}
