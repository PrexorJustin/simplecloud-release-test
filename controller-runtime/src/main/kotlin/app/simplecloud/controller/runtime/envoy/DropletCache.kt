package app.simplecloud.controller.runtime.envoy

import app.simplecloud.controller.runtime.droplet.DropletRepository
import app.simplecloud.droplet.api.droplet.Droplet
import com.google.protobuf.Any
import com.google.protobuf.Duration
import com.google.protobuf.UInt32Value
import io.envoyproxy.controlplane.cache.ConfigWatcher
import io.envoyproxy.controlplane.cache.v3.SimpleCache
import io.envoyproxy.controlplane.cache.v3.Snapshot
import io.envoyproxy.envoy.config.cluster.v3.Cluster
import io.envoyproxy.envoy.config.cluster.v3.Cluster.EdsClusterConfig
import io.envoyproxy.envoy.config.core.v3.*
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints
import io.envoyproxy.envoy.config.listener.v3.Filter
import io.envoyproxy.envoy.config.listener.v3.FilterChain
import io.envoyproxy.envoy.config.listener.v3.Listener
import io.envoyproxy.envoy.config.route.v3.*
import io.envoyproxy.envoy.extensions.filters.http.connect_grpc_bridge.v3.FilterConfig
import io.envoyproxy.envoy.extensions.filters.http.grpc_web.v3.GrpcWeb
import io.envoyproxy.envoy.extensions.filters.http.router.v3.Router
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter
import io.envoyproxy.envoy.extensions.upstreams.http.v3.HttpProtocolOptions
import org.apache.logging.log4j.LogManager
import java.util.*

/**
 * This class handles the remapping of the [DropletRepository] to a [SimpleCache] of [Snapshot]s, which are used by the envoy ADS service.
 */
class DropletCache {
    private val cache = SimpleCache(SimpleCloudNodeGroup())
    private val logger = LogManager.getLogger(DropletCache::class.java)

    //Create a new Snapshot by the droplet repository's data
    fun update(droplets: List<Droplet>) {
        logger.info("Detected new droplets in DropletRepository, adding to ADS...")
        val clusters = mutableListOf<Cluster>()
        val listeners = mutableListOf<Listener>()
        val clas = mutableListOf<ClusterLoadAssignment>()
        droplets.forEach {
            clusters.add(createCluster(it))
            listeners.add(createListener(it))
            clas.add(createCLA(it))
        }
        cache.setSnapshot(
            SimpleCloudNodeGroup.GROUP,
            Snapshot.create(
                clusters,
                clas,
                listeners,
                listOf(), // We don't need routes
                listOf(), // We don't need secrets
                UUID.randomUUID()
                    .toString() //This can be anything, used internally for versioning. THIS HAS TO BE DIFFERENT FOR EVERY SNAPSHOT
            )
        )
    }

    //Creates endpoints users can connect with later
    private fun createListener(it: Droplet): Listener {
        return Listener.newBuilder().setName("${it.type}-${it.id}").setAddress(
            Address.newBuilder().setSocketAddress(
                SocketAddress.newBuilder().setProtocol(SocketAddress.Protocol.TCP).setAddress("0.0.0.0")
                    .setPortValue(it.envoyPort)
            )
        ).setDefaultFilterChain(createListenerFilterChain("${it.type}-${it.id}")).build()

    }

    //Creates load assignments for new droplets (I don't yet know if they need to be called every time?)
    private fun createCLA(it: Droplet): ClusterLoadAssignment {
        return ClusterLoadAssignment.newBuilder().setClusterName("${it.type}-${it.id}")
            .addEndpoints(
                LocalityLbEndpoints.newBuilder().addLbEndpoints(
                    LbEndpoint.newBuilder().setEndpoint(
                        Endpoint.newBuilder()
                            .setAddress(
                                Address.newBuilder().setSocketAddress(
                                    SocketAddress.newBuilder().setPortValue(it.port).setAddress(it.host)
                                        .setProtocol(SocketAddress.Protocol.TCP)
                                )
                            )
                    )
                )
            )
            .build()
    }

    //Creates clusters listening to droplets
    private fun createCluster(it: Droplet): Cluster {
        return Cluster.newBuilder().setName("${it.type}-${it.id}")
            .setConnectTimeout(Duration.newBuilder().setSeconds(5))
            .setType(Cluster.DiscoveryType.EDS)
            .setEdsClusterConfig(
                EdsClusterConfig.newBuilder()
                    .setEdsConfig(ConfigSource.newBuilder().setAds(AggregatedConfigSource.getDefaultInstance()))
            )
            .setLbPolicy(Cluster.LbPolicy.ROUND_ROBIN)
            .setLoadAssignment(
                ClusterLoadAssignment.newBuilder().setClusterName("${it.type}-${it.id}")
                    .addEndpoints(
                        LocalityLbEndpoints.newBuilder()
                            .addLbEndpoints(
                                LbEndpoint.newBuilder().setEndpoint(
                                    Endpoint.newBuilder()
                                        .setAddress(
                                            Address.newBuilder().setSocketAddress(
                                                SocketAddress.newBuilder().setPortValue(it.port).setAddress(it.host)
                                            )
                                        )
                                )
                            )
                    )
            ).putTypedExtensionProtocolOptions(
                "envoy.extensions.upstreams.http.v3.HttpProtocolOptions", Any.pack(
                    HttpProtocolOptions.newBuilder().setExplicitHttpConfig(
                        HttpProtocolOptions.ExplicitHttpConfig.newBuilder().setHttp2ProtocolOptions(
                            Http2ProtocolOptions.newBuilder().setMaxConcurrentStreams(
                                UInt32Value.of(100)
                            )
                        )
                    ).build()
                )
            )
            .build()

    }

    //Creates a filter chain that remaps http to grpc
    private fun createListenerFilterChain(cluster: String): FilterChain.Builder {
        return FilterChain.newBuilder()
            .addFilters(
                Filter.newBuilder().setName("envoy.filters.network.http_connection_manager")
                    .setTypedConfig(
                        Any.pack(
                            HttpConnectionManager.newBuilder().setStatPrefix("ingress_http")
                                .setCodecType(HttpConnectionManager.CodecType.AUTO)
                                .setRouteConfig(
                                    RouteConfiguration.newBuilder().setName("local_route")
                                        .addVirtualHosts(
                                            VirtualHost.newBuilder().setName("local_service").addDomains("*")
                                                .addRoutes(
                                                    Route.newBuilder().setRoute(
                                                        RouteAction.newBuilder().setCluster(cluster)
                                                            .setTimeout(Duration.newBuilder().setSeconds(0).setNanos(0))
                                                    ).setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                                )
                                        )
                                ).addHttpFilters(
                                    HttpFilter.newBuilder().setName("envoy.filters.http.connect_grpc_bridge")
                                        .setTypedConfig(Any.pack(FilterConfig.getDefaultInstance()))
                                ).addHttpFilters(
                                    HttpFilter.newBuilder().setName("envoy.filters.http.grpc_web")
                                        .setTypedConfig(Any.pack(GrpcWeb.getDefaultInstance()))
                                ).addHttpFilters(
                                    HttpFilter.newBuilder().setName("envoy.filters.http.router")
                                        .setTypedConfig(Any.pack(Router.getDefaultInstance()))
                                )

                                .build()
                        )
                    )
            )
    }

    fun getCache(): ConfigWatcher {
        return cache
    }

}