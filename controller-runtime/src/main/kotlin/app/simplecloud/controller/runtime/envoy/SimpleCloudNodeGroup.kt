package app.simplecloud.controller.runtime.envoy

import io.envoyproxy.controlplane.cache.NodeGroup
import io.envoyproxy.envoy.config.core.v3.Node

/**
 * SimpleCloud only uses one envoy node. That's why we can just
 * have one node in the nodegroup.
 */
class SimpleCloudNodeGroup : NodeGroup<String> {

    companion object {
        const val GROUP = "simplecloud"
    }

    override fun hash(node: Node?): String {
        if (node == null) {
            throw IllegalArgumentException("Null node")
        }
        return GROUP
    }
}