/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.services.envoy.cp;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.util.Durations;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.ReferenceMetadataEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.event.ApiEvent;
import io.gravitee.rest.api.service.exceptions.ApiMetadataNotFoundException;
import io.gravitee.rest.api.services.envoy.cp.utils.EnvoyResources;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.envoyproxy.envoy.config.core.v3.ApiVersion.V3;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvoyControlPlaneService extends AbstractService implements EventListener<ApiEvent, GenericApiEntity> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(EnvoyControlPlaneService.class);

    public static final String ENVOY_META_API_ID = "x-envoy-api-id";

    public static final String ENVOY_META_SYNC_FIELD = "x-envoy-sync";

    public static final String ENVOY_META_LISTENER_PORT = "x-envoy-listener-port";

    private static final int ENVOY_DEFAULT_LISTENER_PORT = 8080;

    protected static final int UNSECURE_PORT = 80;
    protected static final int SECURE_PORT = 443;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ApiMetadataService apiMetadataService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private MetadataService metadataService;

    private static final String GROUP = "key";

    private final SimpleCache<String> cache = new SimpleCache<>(node -> GROUP);

    @Override
    protected String name() {
        return "Envoy Control Plane";
    }

    private Server server;

    /**
     * Dummy {@link URLStreamHandler} implementation to avoid unknown protocol issue with default implementation
     * (which knows how to handle only http and https protocol).
     */
    private final URLStreamHandler URL_HANDLER = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) {
            return null;
        }
    };

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, ApiEvent.class);

        V3DiscoveryServer v3DiscoveryServer = new V3DiscoveryServer(cache);

        ServerBuilder<NettyServerBuilder> builder = NettyServerBuilder.forPort(12345)
                .addService(v3DiscoveryServer.getAggregatedDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getClusterDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getEndpointDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getListenerDiscoveryServiceImpl())
                .addService(v3DiscoveryServer.getRouteDiscoveryServiceImpl());

        server = builder.build();

        server.start();

        logger.info("Envoy Control Plane has started on port {}", server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (server != null) {
            server.shutdownNow();
        }
    }

    private final Map<Integer, Listener> listeners = new HashMap<>();
    private final Map<Integer, RouteConfiguration> routes = new HashMap<>();
    private final Map<Integer, List<ClusterRoute>> clusterRoutes = new HashMap<>();

    private final Map<String, Cluster> clusters = new HashMap<>();

    @Override
    public void onEvent(Event<ApiEvent, GenericApiEntity> event) {
        final GenericApiEntity genericApiEntity = event.content();
        // TODO Add v4 api handling when service is compatible
        if (genericApiEntity.getDefinitionVersion() == null || genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            ApiEntity api = (ApiEntity) genericApiEntity;
            switch (event.type()) {
                case DEPLOY:
                    addApiToSnapshot(api);
                    break;
                case UNDEPLOY:
                    removeApiFromSnapshot(api);
                    break;
                case UPDATE:
                    removeApiFromSnapshot(api);
                    addApiToSnapshot(api);
                    break;
            }
        } else {
            logger.warn("Envoy Control Plane is not compatible with API V4.");
        }
    }

    private void removeApiFromSnapshot(ApiEntity api) {
        final String clusterName = "cluster_" + api.getId();
        Cluster cluster = clusters.remove(clusterName);

        // The API was already known
        if (cluster != null) {
            // Check specific configuration
            List<ApiMetadataEntity> apiMetadata = apiMetadataService.findAllByApi(api.getId());

            int listenerPort = readMetadataOrDefault(apiMetadata, ENVOY_META_LISTENER_PORT, ENVOY_DEFAULT_LISTENER_PORT);

            clusterRoutes.put(listenerPort,
                    clusterRoutes.get(listenerPort)
                            .stream()
                            .filter(new Predicate<ClusterRoute>() {
                                @Override
                                public boolean test(ClusterRoute clusterRoute) {
                                    return clusterRoute.clusterName.equals(clusterName);
                                }
                            })
                            .collect(Collectors.toList()));
        }
    }

    private void addApiToSnapshot(ApiEntity api) {
        try {
            // Check that the API is having the x-envoy-sync metadata
            apiMetadataService.findByIdAndApi(ENVOY_META_SYNC_FIELD, api.getId());

            // Check specific configuration
            List<ApiMetadataEntity> apiMetadata = apiMetadataService.findAllByApi(api.getId());

            int listenerPort = readMetadataOrDefault(apiMetadata, ENVOY_META_LISTENER_PORT, ENVOY_DEFAULT_LISTENER_PORT);

            // Ensure listener is already created, if not create it
            createListener(listenerPort);

            // Create cluster if endpoint are defined
            if (! api.getProxy().getGroups().isEmpty()) {
                Cluster cluster = createFromEndpointGroup(api.getId(), api.getProxy().getGroups().iterator().next());
                clusters.put(cluster.getName(), cluster);
            }

            // Create routes
            updateListenerRoutes(listenerPort, "cluster_" + api.getId(), api.getProxy().getVirtualHosts());

            // Publish new version of the snapshot
            publishSnapshot();
        } catch (ApiMetadataNotFoundException amnfe) {
            // do nothing
        }
    }

    private final AtomicLong routeCounter = new AtomicLong(0);

    private void updateListenerRoutes(int listenerPort, String clusterName, List<VirtualHost> virtualHosts) {
        // Convert null virtual host to wildcard host
        virtualHosts = virtualHosts.stream().map(virtualHost -> {
            if (virtualHost.getHost() == null) {
                virtualHost.setHost("*");
            }

            // Add a trailing slash
            if (! virtualHost.getPath().endsWith("/")) {
                virtualHost.setPath(virtualHost.getPath() + '/');
            }

            return virtualHost;
        }).collect(Collectors.toList());

        // Define new routes
        ClusterRoute route = new ClusterRoute(clusterName, virtualHosts);

        List<ClusterRoute> clusterRoutesLst = clusterRoutes.get(listenerPort);
        if (clusterRoutesLst == null) {
            clusterRoutesLst = new ArrayList<>();
        }
        clusterRoutesLst.add(route);

        clusterRoutes.put(listenerPort, clusterRoutesLst);

        // Create the new RouteConfiguration
        RouteConfiguration.Builder routeConfigurationBuilder = RouteConfiguration
                .newBuilder()
                .setName("route_http_" + listenerPort);

        // Group by domain
        Map<String, List<Target>> targetByDomain = clusterRoutesLst.stream()
                .flatMap(new java.util.function.Function<ClusterRoute, Stream<Target>>() {
                    @Override
                    public Stream<Target> apply(ClusterRoute clusterRoute) {
                        return clusterRoute.virtualHosts.stream().map(new java.util.function.Function<VirtualHost, Target>() {
                            @Override
                            public Target apply(VirtualHost virtualHost) {
                                return new Target(virtualHost.getHost(), virtualHost.getPath(), clusterRoute.clusterName);
                            }
                        });
                    }
                })
                .collect(Collectors.groupingBy(Target::getHost));

        targetByDomain.forEach(new BiConsumer<String, List<Target>>() {
            @Override
            public void accept(String domain, List<Target> targets) {

                io.envoyproxy.envoy.config.route.v3.VirtualHost.Builder builder = io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder()
                        .setName("local_route_" + routeCounter.incrementAndGet())
                        .addDomains(domain);

                targets.forEach(new Consumer<Target>() {
                    @Override
                    public void accept(Target target) {
                        builder.addRoutes(Route.newBuilder()
                                .setMatch(RouteMatch.newBuilder()
                                        .setPrefix(target.getPath()))
                                .setRoute(RouteAction.newBuilder()
                                        .setPrefixRewrite("/")
                                        .setCluster(target.getClusterName())));
                    }
                });


                routeConfigurationBuilder.addVirtualHosts(builder.build());
            }
        });

        routes.put(listenerPort, routeConfigurationBuilder.build());
    }

    private final AtomicLong snapshotCounter = new AtomicLong(0);

    private void publishSnapshot() {
        Snapshot snapshot = Snapshot.create(
                clusters.values(),
                ImmutableList.of(),
                listeners.values(),
                routes.values(),
                ImmutableList.of(),
                "v" + snapshotCounter.incrementAndGet());

        cache.setSnapshot(
                GROUP,
                snapshot);
    }

    private void createListener(int listenerPort) {
        listeners.computeIfAbsent(listenerPort, port -> EnvoyResources.createListenerV3(false, V3, V3,
                "listener_http_" + port, port, "route_http_" + port));
    }

    private Cluster createFromEndpointGroup(String apiId, EndpointGroup group) {
        final String clusterName = "cluster_" + apiId;
        LocalityLbEndpoints.Builder builder = LocalityLbEndpoints.newBuilder();

        group.getEndpoints().forEach(endpoint -> builder.addLbEndpoints(createFromEndpoint(endpoint))
                .setLoadBalancingWeight(UInt32Value.newBuilder().setValue(endpoint.getWeight()).build()));

        return Cluster.newBuilder()
                .setName(clusterName)
                .setConnectTimeout(Durations.fromSeconds(5))
                .setType(Cluster.DiscoveryType.STRICT_DNS)
                .setLoadAssignment(ClusterLoadAssignment.newBuilder()
                        .setClusterName(clusterName)
                        .addEndpoints(builder.build())
                )
                .build();
    }

    private LbEndpoint createFromEndpoint(Endpoint endpoint) {
        LbEndpoint.Builder builder = LbEndpoint.newBuilder();
        try {
            URL url = new URL(null, endpoint.getTarget(), URL_HANDLER);

            final int defaultPort = isSecureProtocol(url.getProtocol()) ? SECURE_PORT : UNSECURE_PORT;
            final int port = url.getPort() != -1 ? url.getPort() : defaultPort;

            return builder.setEndpoint(io.envoyproxy.envoy.config.endpoint.v3.Endpoint.newBuilder()
                    .setAddress(Address.newBuilder()
                            .setSocketAddress(SocketAddress.newBuilder()
                                    .setAddress(url.getHost())
                                    .setPortValue(port)
                                    .setProtocolValue(io.envoyproxy.envoy.api.v2.core.SocketAddress.Protocol.TCP_VALUE))))
                    .build();

        } catch (MalformedURLException murle) {
            logger.error("Unexpected error: ", murle);
            return null;
        }
    }

    private Completable synchronize() {
        return Single
                .fromCallable(
                        () ->
                                metadataService
                                        .findByKeyAndReferenceType(EnvoyControlPlaneService.ENVOY_META_SYNC_FIELD, MetadataReferenceType.API)
                                        .stream()
                                        .map(MetadataEntity::getValue)
                                        .collect(Collectors.toSet())
                )
                .flattenAsFlowable((Function<Set<String>, Iterable<String>>) strings -> strings)
                .flatMapCompletable(new Function<String, CompletableSource>() {
                    @Override
                    public CompletableSource apply(String apiId) throws Throwable {
                        return synchronizeApi(apiId);
                    }
                });
    }

    private Completable synchronizeApi(String apiId) {
        return Completable.fromCallable(new Callable<ApiEntity>() {
            @Override
            public ApiEntity call() throws Exception {
                return apiService.findById(GraviteeContext.getExecutionContext(), apiId);
            }
        });
    }

    protected static boolean isSecureProtocol(String protocol) {
        return protocol.charAt(protocol.length() - 1) == 's' && protocol.length() > 2;
    }

    private Optional<ApiMetadataEntity> readMetadata(List<ApiMetadataEntity> metadata, String key) {
        return metadata.stream().filter(apiMetadataEntity -> apiMetadataEntity.getKey().equals(key))
                .findFirst();
    }

    private String readMetadataOrDefault(List<ApiMetadataEntity> metadata, String key, String defaultValue) {
        return metadata.stream().filter(apiMetadataEntity -> apiMetadataEntity.getKey().equals(key))
                .findFirst()
                .map(ReferenceMetadataEntity::getValue)
                .orElse(defaultValue);
    }

    private int readMetadataOrDefault(List<ApiMetadataEntity> metadata, String key, int defaultValue) {
        return metadata.stream().filter(apiMetadataEntity -> apiMetadataEntity.getKey().equals(key))
                .findFirst()
                .map(apiMetadataEntity -> Integer.parseInt(apiMetadataEntity.getValue()))
                .orElse(defaultValue);
    }

    private static class ClusterRoute {
        private final String clusterName;
        private final List<VirtualHost> virtualHosts;

        private ClusterRoute(String clusterName, List<VirtualHost> virtualHosts) {
            this.clusterName = clusterName;
            this.virtualHosts = virtualHosts;
        }
    }

    private static class Target {
        private final String host;
        private final String path;
        private final String clusterName;

        public Target(String host, String path, String clusterName) {
            this.host = host;
            this.path = path;
            this.clusterName = clusterName;
        }

        public String getHost() {
            return host;
        }

        public String getPath() {
            return path;
        }

        public String getClusterName() {
            return clusterName;
        }
    }
}
