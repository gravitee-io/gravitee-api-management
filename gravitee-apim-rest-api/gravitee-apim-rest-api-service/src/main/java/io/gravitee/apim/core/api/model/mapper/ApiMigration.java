/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static io.gravitee.definition.model.v4.flow.execution.FlowMode.BEST_MATCH;
import static io.gravitee.definition.model.v4.flow.execution.FlowMode.DEFAULT;
import static io.gravitee.plugin.configurations.http.ProtocolVersion.HTTP_1_1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.api.model.utils.MigrationWarnings;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.definition.model.services.schedule.ScheduledService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.jspecify.annotations.Nullable;

@CustomLog
class ApiMigration {

    public static final String HTTP_PROXY = "http-proxy";
    private static final String TYPE_ENDPOINT = "ENDPOINT";
    private static final String TYPE_ENDPOINTGROUP = "ENDPOINTGROUP";
    public static final String CONSUL_DISCOVERY_SERVICE_TYPE = "consul-service-discovery";
    public static final String HTTP_HEALTH_CHECK_SERVICE_TYPE = "http-health-check";
    private final ObjectMapper jsonMapper;
    private final ApiServicesMigration apiServicesMigration;
    private final SharedConfigurationMigration sharedConfigurationMigration;
    private static final Set<String> HTTP11_ALLOWED = Set.of(
        "version",
        "keepAlive",
        "keepAliveTimeout",
        "connectTimeout",
        "pipelining",
        "readTimeout",
        "useCompression",
        "propagateClientAcceptEncoding",
        "propagateClientHost",
        "idleTimeout",
        "followRedirects",
        "maxConcurrentConnections"
    );
    private static final Set<String> HTTP2_ALLOWED = Set.of(
        "version",
        "clearTextUpgrade",
        "keepAlive",
        "keepAliveTimeout",
        "connectTimeout",
        "pipelining",
        "readTimeout",
        "useCompression",
        "propagateClientAcceptEncoding",
        "propagateClientHost",
        "idleTimeout",
        "followRedirects",
        "maxConcurrentConnections",
        "http2MultiplexingLimit"
    );

    public ApiMigration(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        apiServicesMigration = new ApiServicesMigration(jsonMapper);
        sharedConfigurationMigration = new SharedConfigurationMigration(jsonMapper);
    }

    MigrationResult<Api> mapApi(Api source) {
        if (source.getApiDefinitionValue() instanceof io.gravitee.definition.model.Api apiDefinitionV2) {
            return apiDefinitionHttpV4(apiDefinitionV2).map(definition ->
                source.toBuilder().apiDefinitionValue(definition).type(ApiType.PROXY).build()
            );
        } else {
            return MigrationResult.issue(MigrationWarnings.V2_API_NOT_NULL, MigrationResult.State.IMPOSSIBLE);
        }
    }

    private MigrationResult<io.gravitee.definition.model.v4.Api> apiDefinitionHttpV4(io.gravitee.definition.model.Api apiDefinitionV2) {
        if (apiDefinitionV2 == null) {
            return MigrationResult.issue(MigrationWarnings.V2_API_NOT_NULL, MigrationResult.State.IMPOSSIBLE);
        }
        List<Listener> listeners = List.of(
            HttpListener.builder()
                .cors(apiDefinitionV2.getProxy().getCors())
                .servers(apiDefinitionV2.getProxy().getServers())
                .type(ListenerType.HTTP)
                .paths(
                    stream(apiDefinitionV2.getProxy().getVirtualHosts())
                        .map(e -> new Path(e.getHost(), addSlashIfNeeded(e.getPath()), e.isOverrideEntrypoint()))
                        .toList()
                )
                .pathMappingsPattern(apiDefinitionV2.getPathMappings())
                .entrypoints(List.of(Entrypoint.builder().type(HTTP_PROXY).build()))
                .build()
        );
        Analytics analytics = mapAnalytics(apiDefinitionV2.getProxy().getLogging());
        var endpointGroups = stream(apiDefinitionV2.getProxy().getGroups())
            .map(source -> mapEndpointGroup(source, apiDefinitionV2.getServices()))
            .collect(MigrationResult.collectList());
        Failover failover = mapFailOver(apiDefinitionV2.getProxy());
        MigrationResult<ApiServices> apiServicesMigrationResult = mapApiServices(apiDefinitionV2.getServices());
        return endpointGroups.flatMap(endpointGroupsList ->
            apiServicesMigrationResult.map(apiServices -> {
                var api = new io.gravitee.definition.model.v4.Api(
                    listeners,
                    endpointGroupsList,
                    analytics,
                    failover,
                    null /* plans are managed in another place because is in a different collection */,
                    null,
                    null /* flows are managed in another place because is in a different collection */,
                    apiDefinitionV2.getResponseTemplates(),
                    apiServices,
                    false /* allowedInApiProducts - defaults to false for migrated APIs */
                );
                api.setId(apiDefinitionV2.getId());
                api.setName(apiDefinitionV2.getName());
                api.setApiVersion(apiDefinitionV2.getVersion());
                api.setTags(apiDefinitionV2.getTags());
                api.setType(ApiType.PROXY);
                api.setProperties(mapProperties(apiDefinitionV2.getProperties()));
                api.setResources(mapResources(apiDefinitionV2.getResources()));
                api.setFlowExecution(mapFlowExecution(apiDefinitionV2.getFlowMode()));
                return api;
            })
        );
    }

    private MigrationResult<ApiServices> mapApiServices(Services services) {
        if (services == null || services.isEmpty() || services.getDynamicPropertyService() == null) {
            return MigrationResult.value(ApiServices.builder().build());
        }
        MigrationResult<Service> dynamicPropertyServiceMigrationResult = apiServicesMigration.convert(
            services.getDynamicPropertyService(),
            null,
            null
        );

        return dynamicPropertyServiceMigrationResult.map(service -> ApiServices.builder().dynamicProperty(service).build());
    }

    private Failover mapFailOver(Proxy proxy) {
        return proxy.failoverEnabled()
            ? Failover.builder()
                .enabled(proxy.failoverEnabled())
                .slowCallDuration(proxy.getFailover().getRetryTimeout())
                .maxRetries(proxy.getFailover().getMaxAttempts())
                .build()
            : null;
    }

    private MigrationResult<EndpointGroup> mapEndpointGroup(io.gravitee.definition.model.EndpointGroup source, Services endpointServices) {
        MigrationResult<EndpointGroupServices> endpointGroupServicesMigrationResult = mapEndpointGroupServices(
            endpointServices,
            source.getServices(),
            source.getName()
        );
        MigrationResult<List<Endpoint>> endpoints = MigrationResult.value(List.of());
        MigrationResult<EndpointGroup> endpointGroupMigrationResult = MigrationResult.value(
            EndpointGroup.builder().name(source.getName()).type(HTTP_PROXY).loadBalancer(mapLoadBalancer(source.getLoadBalancer())).build()
        );
        String sharedConfiguration = null;
        try {
            sharedConfiguration = sharedConfigurationMigration.convert(source);
            endpoints = stream(source.getEndpoints()).map(this::mapEndpoint).collect(MigrationResult.collectList());
        } catch (JsonProcessingException e) {
            log.error("Unable to map configuration for endpoint group {}", source.getName(), e);
            endpointGroupMigrationResult.addIssue(
                new MigrationResult.Issue(
                    MigrationWarnings.ENDPOINT_GROUP_PARSE_ERROR.formatted(source.getName()),
                    MigrationResult.State.IMPOSSIBLE
                )
            );
        }

        return endpointGroupMigrationResult
            .foldLeft(endpointGroupServicesMigrationResult, (egp, egs) -> {
                if (egp != null) {
                    egp.setServices(egs);
                }
                return egp;
            })
            .foldLeft(endpoints, (egp, b) -> {
                if (egp != null) {
                    egp.setEndpoints(b);
                }
                return egp;
            })
            .foldLeft(MigrationResult.value(sharedConfiguration), (egp, b) -> {
                if (egp != null) {
                    egp.setSharedConfiguration(b);
                }
                return egp;
            });
    }

    private MigrationResult<EndpointGroupServices> mapEndpointGroupServices(
        Services endpointServices,
        Services endpointGroupServices,
        String name
    ) {
        if (endpointServices == null && endpointGroupServices == null) {
            return MigrationResult.value(new EndpointGroupServices());
        }

        var migratedServices = Stream.of(endpointServices, endpointGroupServices)
            .filter(Objects::nonNull)
            .flatMap(s -> s.getAll().stream())
            .filter(s -> !(s instanceof ScheduledService) || ((ScheduledService) s).getSchedule() != null)
            .flatMap(service -> Stream.ofNullable(apiServicesMigration.convert(service, TYPE_ENDPOINTGROUP, name)))
            .collect(MigrationResult.collectList());

        return migratedServices.map(services -> {
            var servicesByType = services
                .stream()
                .filter(
                    service ->
                        CONSUL_DISCOVERY_SERVICE_TYPE.equals(service.getType()) || HTTP_HEALTH_CHECK_SERVICE_TYPE.equals(service.getType())
                )
                .collect(Collectors.toMap(Service::getType, svc -> svc, (svc1, svc2) -> svc2));

            var egs = new EndpointGroupServices();
            egs.setDiscovery(servicesByType.get(CONSUL_DISCOVERY_SERVICE_TYPE));
            egs.setHealthCheck(servicesByType.get(HTTP_HEALTH_CHECK_SERVICE_TYPE));
            return egs;
        });
    }

    private MigrationResult<EndpointServices> mapEndPointServices(String configuration, String name) {
        EndpointServices endpointServices = new EndpointServices();
        MigrationResult<EndpointServices> migrationResult = MigrationResult.value(endpointServices);
        if (configuration == null) {
            return migrationResult;
        }
        try {
            JsonNode jsonNode = jsonMapper.readTree(configuration);
            JsonNode hcNode = jsonNode.path("healthcheck");
            if (!hcNode.isMissingNode() && !hcNode.isNull()) {
                EndpointHealthCheckService epHealthCheckService = jsonMapper.treeToValue(hcNode, EndpointHealthCheckService.class);
                if (epHealthCheckService.getSchedule() != null && !epHealthCheckService.isInherit()) {
                    var serviceMigrationResult = apiServicesMigration.convert(epHealthCheckService, TYPE_ENDPOINT, name);
                    if (serviceMigrationResult != null) {
                        return migrationResult.foldLeft(serviceMigrationResult, (endpointSvc, serviceV4) -> {
                            if (endpointSvc != null) {
                                endpointSvc.setHealthCheck(serviceV4);
                            }
                            return endpointSvc;
                        });
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to map configuration for endpoint {}", name, e);
            return migrationResult.addIssue(
                new MigrationResult.Issue(
                    MigrationWarnings.HEALTHCHECK_ENDPOINT_PARSE_ERROR.formatted(name),
                    MigrationResult.State.IMPOSSIBLE
                )
            );
        }
        return migrationResult;
    }

    @Nullable
    private List<Property> mapProperties(@Nullable Properties properties) {
        return properties == null
            ? null
            : stream(properties.getProperties())
                .map(a -> new Property(a.getKey(), a.getValue(), a.isEncrypted(), a.isDynamic()))
                .toList();
    }

    private List<Resource> mapResources(List<io.gravitee.definition.model.plugins.resources.Resource> resources) {
        return stream(resources).map(this::toV4Resource).toList();
    }

    private Resource toV4Resource(io.gravitee.definition.model.plugins.resources.Resource resource) {
        return new Resource(resource.getName(), resource.getType(), resource.getConfiguration(), resource.isEnabled());
    }

    private FlowExecution mapFlowExecution(FlowMode flowMode) {
        var flowExecution = new FlowExecution();
        var mode = switch (flowMode) {
            case DEFAULT -> DEFAULT;
            case BEST_MATCH -> BEST_MATCH;
        };
        flowExecution.setMode(mode);
        return flowExecution;
    }

    private LoadBalancer mapLoadBalancer(io.gravitee.definition.model.LoadBalancer lb) {
        return switch (lb.getType()) {
            case RANDOM -> new LoadBalancer(LoadBalancerType.RANDOM);
            case ROUND_ROBIN -> new LoadBalancer(LoadBalancerType.ROUND_ROBIN);
            case WEIGHTED_RANDOM -> new LoadBalancer(LoadBalancerType.WEIGHTED_RANDOM);
            case WEIGHTED_ROUND_ROBIN -> new LoadBalancer(LoadBalancerType.WEIGHTED_ROUND_ROBIN);
            case null -> null;
        };
    }

    private MigrationResult<Endpoint> mapEndpoint(io.gravitee.definition.model.Endpoint lb) {
        MigrationResult<EndpointServices> endPointServices = mapEndPointServices(lb.getConfiguration(), lb.getName());
        MigrationResult<String> sharedConfigurationOverride = mapSharedConfigurationOverride(lb.getConfiguration());
        MigrationResult<String> configuration = mapConfiguration(lb);

        return endPointServices.flatMap(epServices ->
            sharedConfigurationOverride.flatMap(sharedConfigurationOverride1 ->
                configuration.map(configuration1 ->
                    Endpoint.builder()
                        .name(lb.getName())
                        .type(HTTP_PROXY)
                        .secondary(lb.isBackup())
                        .tenants(lb.getTenants())
                        .weight(lb.getWeight())
                        .configuration(configuration1)
                        .inheritConfiguration(lb.getInherit())
                        .sharedConfigurationOverride(sharedConfigurationOverride1)
                        .services(epServices)
                        .build()
                )
            )
        );
    }

    private MigrationResult<String> mapSharedConfigurationOverride(String config) {
        try {
            if (config == null) {
                return MigrationResult.value(null);
            }
            ObjectNode root = (ObjectNode) jsonMapper.readTree(config);
            if (root == null || root.isNull() || root.isMissingNode()) {
                return MigrationResult.value(config);
            }
            JsonNode healthcheckNode = root.path("healthcheck");
            if (healthcheckNode == null || healthcheckNode.isNull() || healthcheckNode.isMissingNode()) {
                return MigrationResult.value(config);
            }
            ArrayNode steps = root.path("healthcheck").path("steps").isArray()
                ? (ArrayNode) root.path("healthcheck").path("steps")
                : JsonNodeFactory.instance.arrayNode();
            for (JsonNode step : steps) {
                ObjectNode response = (ObjectNode) step.path("response");
                JsonNode assertionsNode = response.get("assertions");
                response.remove("assertions");
                if (assertionsNode != null && assertionsNode.isArray() && assertionsNode.size() == 1) {
                    response.put("assertion", StringUtils.appendCurlyBraces(assertionsNode.get(0).asText()));
                }
            }
            JsonNode httpProxyNode = root.path("proxy");
            System.out.println("httpProxyNode is = " + httpProxyNode);
            if (httpProxyNode.isObject()) {
                ObjectNode proxyObject = (ObjectNode) httpProxyNode;

                boolean enabled = proxyObject.path("enabled").asBoolean(false);
                boolean useSystemProxy = proxyObject.path("useSystemProxy").asBoolean(false);

                if (enabled && useSystemProxy) {
                    proxyObject.remove("port");
                    proxyObject.remove("type");
                }
            }
            return MigrationResult.value(jsonMapper.writeValueAsString(root));
        } catch (JsonProcessingException e) {
            log.error("Unable to map configuration for endpoint", e);
            return MigrationResult.issue(MigrationWarnings.ENDPOINT_PARSE_ERROR, MigrationResult.State.IMPOSSIBLE);
        }
    }

    private Analytics mapAnalytics(Logging v2logging) {
        var loggingEnabled = isLoggingEnabled(v2logging);
        var v4logging = loggingEnabled ? mapLogging(v2logging) : null;
        return Analytics.builder().enabled(true).logging(v4logging).build();
    }

    private boolean isLoggingEnabled(Logging v2logging) {
        if (v2logging == null) {
            return false;
        }
        return v2logging.getMode() != io.gravitee.definition.model.LoggingMode.NONE;
    }

    private io.gravitee.definition.model.v4.analytics.logging.Logging mapLogging(Logging v2logging) {
        return io.gravitee.definition.model.v4.analytics.logging.Logging.builder()
            .condition(v2logging.getCondition())
            .mode(new LoggingMode(v2logging.getMode().isClientMode(), v2logging.getMode().isProxyMode()))
            .content(new LoggingContent(v2logging.getContent().isHeaders(), false, v2logging.getContent().isPayloads(), false, false))
            .phase(new LoggingPhase(v2logging.getScope().isRequest(), v2logging.getScope().isResponse()))
            .build();
    }

    private MigrationResult<String> mapConfiguration(io.gravitee.definition.model.Endpoint lb) {
        try {
            if (lb.getConfiguration() == null) {
                return MigrationResult.value(null);
            }
            var jsonNode = jsonMapper.readTree(lb.getConfiguration()).get("target");
            var target = jsonNode != null ? jsonNode.asText() : null;
            var target1 = jsonMapper.createObjectNode().put("target", target);
            var proxyNode = jsonMapper.readTree(lb.getConfiguration()).get("proxy");
            if (proxyNode != null) {
                ObjectNode proxyObject = (ObjectNode) proxyNode;
                boolean enabled = proxyNode.path("enabled").asBoolean(false);
                boolean useSystemProxy = proxyNode.path("useSystemProxy").asBoolean(false);

                if (enabled && useSystemProxy) {
                    proxyObject.remove("port");
                    proxyObject.remove("type");
                }
            }
            //uncomment this for http issue
            /*var httpNode = jsonMapper.readTree(lb.getConfiguration()).get("http");
            if (httpNode != null) {
                ObjectNode httpObject = (ObjectNode) httpNode;
                String version = httpNode.path("version").asText();
                if (version.equals(HTTP_1_1.name())) {
                    httpObject.retain(HTTP11_ALLOWED);
                } else {
                    httpObject.retain(HTTP2_ALLOWED);
                }
            }*/
            return MigrationResult.value(jsonMapper.writeValueAsString(target1));
        } catch (JsonProcessingException e) {
            log.error("Unable to map configuration for endpoint {}", lb.getName(), e);
            return MigrationResult.issue(MigrationWarnings.ENDPOINT_PARSE_ERROR, MigrationResult.State.IMPOSSIBLE);
        }
    }

    @Nullable
    private String addSlashIfNeeded(@Nullable String path) {
        return path == null || path.isEmpty() || path.endsWith("/") ? path : path + '/';
    }
}
