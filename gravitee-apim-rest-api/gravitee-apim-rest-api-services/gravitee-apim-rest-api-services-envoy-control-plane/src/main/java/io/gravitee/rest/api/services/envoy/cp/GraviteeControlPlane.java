package io.gravitee.rest.api.services.envoy.cp;

import com.google.common.collect.ImmutableList;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.gravitee.rest.api.services.envoy.cp.utils.EnvoyResources;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;

import static io.envoyproxy.envoy.config.core.v3.ApiVersion.V3;

public class GraviteeControlPlane {

    private static final String GROUP = "key";

    /**
     * Example minimal xDS implementation using the java-control-plane lib.
     *
     * @param arg command-line args
     */
    public static void main(String[] arg) throws IOException, InterruptedException {
        SimpleCache<String> cache = new SimpleCache<>(node -> GROUP);

        /*
        Snapshot snapshot = createSnapshotNoEds(false, V3, V3,
                "upstream",
                "localhost",
                4004,
                "listener0",
                8080,
                "route0",
                "1");
         */

        Cluster cluster1 = EnvoyResources.createClusterV3("console", "localhost", 3000);
        Listener listener1 = EnvoyResources.createListenerV3(false, V3, V3,
                "listener1", 8080, "route1");
        RouteConfiguration route1 = EnvoyResources.createRouteV3("route1", "console");

        Cluster cluster2 = EnvoyResources.createClusterV3("echo", "api.gravitee.io", 443);
        Listener listener2 = EnvoyResources.createListenerV3(false, V3, V3,
                "listener2", 8081, "route2");
        RouteConfiguration route2 = EnvoyResources.createRouteV3("route2", "echo");

        Snapshot snapshot = Snapshot.create(
                ImmutableList.of(cluster1),
                ImmutableList.of(),
                ImmutableList.of(listener1),
                ImmutableList.of(route1),
                ImmutableList.of(),
                "2");

        cache.setSnapshot(
                GROUP,
                snapshot);
        /*
                Snapshot.create(
                        ImmutableList.of(EnvoyResources.createCluster("local_cluster")),
                        ImmutableList.of(EnvoyResources.createEndpoint("some_service", "127.0.0.1", 8000)),
                        ImmutableList.of(EnvoyResources.createListener(false, false, ApiVersion.V3, ApiVersion.V3, "listener_0", 8081, "local_route")),
                        ImmutableList.of(EnvoyResources.createRoute("local_route", "local_cluster")),
                        ImmutableList.of(),
                        "1"));
         */

        V3DiscoveryServer v3DiscoveryServer = new V3DiscoveryServer(cache);

        ServerBuilder<NettyServerBuilder> builder = NettyServerBuilder.forPort(12345)
                .addService(v3DiscoveryServer.getAggregatedDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getClusterDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getEndpointDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getListenerDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getRouteDiscoveryServiceImpl());

        Server server = builder.build();

        server.start();

        System.out.println("Server has started on port " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        server.awaitTermination();
    }


    private static Snapshot createSnapshotNoEds(
            boolean ads,
            ApiVersion rdsTransportVersion,
            ApiVersion rdsResourceVersion,
            String clusterName,
            String endpointAddress,
            int endpointPort,
            String listenerName,
            int listenerPort,
            String routeName,
            String version) {

        Cluster cluster = EnvoyResources.createClusterV3(clusterName, endpointAddress, endpointPort);
        Listener listener = EnvoyResources.createListenerV3(ads, rdsTransportVersion, rdsResourceVersion,
                listenerName, listenerPort, routeName);
        RouteConfiguration route = EnvoyResources.createRouteV3(routeName, clusterName);

        return Snapshot.create(
                ImmutableList.of(cluster),
                ImmutableList.of(),
                ImmutableList.of(listener),
                ImmutableList.of(route),
                ImmutableList.of(),
                version);
    }
}
