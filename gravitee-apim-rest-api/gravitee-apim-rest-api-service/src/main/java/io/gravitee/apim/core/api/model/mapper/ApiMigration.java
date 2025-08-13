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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
class ApiMigration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String HTTP_PROXY = "http-proxy";

    MigrationResult<Api> mapApi(Api source) {
        return apiDefinitionHttpV4(source.getApiDefinition())
            .map(definition ->
                source
                    .toBuilder()
                    .definitionVersion(DefinitionVersion.V4)
                    .apiDefinitionHttpV4(definition)
                    .apiDefinition(null)
                    .type(ApiType.PROXY)
                    .build()
            );
    }

    private MigrationResult<io.gravitee.definition.model.v4.Api> apiDefinitionHttpV4(io.gravitee.definition.model.Api apiDefinitionV2) {
        if (apiDefinitionV2 == null) {
            return MigrationResult.issue("apiDefinitionV2 must not be null", MigrationResult.State.IMPOSSIBLE);
        }
        List<Listener> listeners = List.of(
            HttpListener
                .builder()
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

        var endpointGroups = stream(apiDefinitionV2.getProxy().getGroups()).map(this::mapEndpointGroup).toList();

        Analytics analytics = mapAnalytics(apiDefinitionV2.getProxy().getLogging());

        Failover failover = new Failover(false, 0, 50, 500, 1, false);

        // TODO handle flows
        FlowExecution flowExecution = null;

        ApiServices services = null;
        var api = new io.gravitee.definition.model.v4.Api(
            listeners,
            endpointGroups,
            analytics,
            failover,
            null/* plans are managed in another place because is in a different collection */,
            flowExecution,
            null/* flows are managed in another place because is in a different collection */,
            apiDefinitionV2.getResponseTemplates(),
            services
        );
        api.setId(apiDefinitionV2.getId());
        api.setName(apiDefinitionV2.getName());
        api.setApiVersion(apiDefinitionV2.getVersion());
        api.setTags(apiDefinitionV2.getTags());
        api.setType(ApiType.PROXY);
        api.setProperties(mapProperties(apiDefinitionV2.getProperties()));
        api.setResources(List.of()); // TODO apiDefinitionV2.getResources());
        return MigrationResult.value(api);
    }

    private EndpointGroup mapEndpointGroup(io.gravitee.definition.model.EndpointGroup source) {
        var endpoints = stream(source.getEndpoints()).map(this::mapEndpoint).toList();
        return EndpointGroup
            .builder()
            .name(source.getName())
            .type(HTTP_PROXY)
            .loadBalancer(mapLoadBalancer(source.getLoadBalancer()))
            .endpoints(endpoints)
            //.services(source.getServices())
            .build();
    }

    @Nullable
    private List<Property> mapProperties(@Nullable Properties properties) {
        return properties == null
            ? null
            : stream(properties.getProperties()).map(a -> new Property(a.getKey(), a.getValue(), a.isEncrypted(), a.isDynamic())).toList();
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

    private Endpoint mapEndpoint(io.gravitee.definition.model.Endpoint lb) {
        return Endpoint
            .builder()
            .name(lb.getName())
            .type(HTTP_PROXY)
            .secondary(lb.isBackup())
            .tenants(lb.getTenants())
            .weight(lb.getWeight())
            .configuration(mapConfiguration(lb))
            //.inheritConfiguration(lb.getInheritConfiguration())
            //.sharedConfigurationOverride(lb.getSharedConfigurationOverride())
            //.services(lb.getS)
            .build();
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
        return io.gravitee.definition.model.v4.analytics.logging.Logging
            .builder()
            .condition(v2logging.getCondition())
            .mode(new LoggingMode(v2logging.getMode().isClientMode(), v2logging.getMode().isProxyMode()))
            .content(new LoggingContent(v2logging.getContent().isHeaders(), false, v2logging.getContent().isPayloads(), false, false))
            .phase(new LoggingPhase(v2logging.getScope().isRequest(), v2logging.getScope().isResponse()))
            .build();
    }

    @Nullable
    private static String mapConfiguration(io.gravitee.definition.model.Endpoint lb) {
        try {
            if (lb.getConfiguration() == null) {
                return null;
            }
            var jsonNode = OBJECT_MAPPER.readTree(lb.getConfiguration()).get("target");
            var target = jsonNode != null ? jsonNode.asText() : null;
            var target1 = OBJECT_MAPPER.createObjectNode().put("target", target);
            return OBJECT_MAPPER.writeValueAsString(target1);
        } catch (JsonProcessingException e) {
            log.error("Unable to map configuration for endpoint {}", lb.getName(), e);
            return null;
        }
    }

    @Nullable
    private String addSlashIfNeeded(@Nullable String path) {
        return path == null || path.isEmpty() || path.endsWith("/") ? path : path + '/';
    }
}
