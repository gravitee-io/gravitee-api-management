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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
class ApiMigration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            return MigrationResult.issues(
                List.of(new MigrationResult.Issue("apiDefinitionV2 must not be null", MigrationResult.State.IMPOSSIBLE))
            );
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
                .entrypoints(List.of(Entrypoint.builder().type("http-proxy").build()))
                .build()
        );

        var endpointGroups = stream(apiDefinitionV2.getProxy().getGroups()).map(this::mapEndpointGroup).toList();

        Analytics analytics = new Analytics(false, null, null, null);

        Failover failover = new Failover(false, 0, 50, 500, 1, false);

        // TODO handle flows
        FlowExecution flowExecution = null;
        List<Flow> flows = List.of();

        var responseTemplates = apiDefinitionV2.getResponseTemplates();

        ApiServices services = null;
        var a = new io.gravitee.definition.model.v4.Api(
            listeners,
            endpointGroups,
            analytics,
            failover,
            null/* plans are managed in another place because is in a different collection */,
            flowExecution,
            flows,
            responseTemplates,
            services
        );
        a.setId(apiDefinitionV2.getId());
        a.setName(apiDefinitionV2.getName());
        a.setApiVersion(apiDefinitionV2.getVersion());
        a.setTags(apiDefinitionV2.getTags());
        a.setType(ApiType.PROXY);
        a.setProperties(List.of()); // TODO apiDefinitionV2.getProperties())
        a.setResources(List.of()); // TODO apiDefinitionV2.getResources());
        return MigrationResult.value(a);
    }

    private EndpointGroup mapEndpointGroup(io.gravitee.definition.model.EndpointGroup source) {
        var endpoints = stream(source.getEndpoints()).map(this::mapEndpoint).toList();
        return EndpointGroup
            .builder()
            .name(source.getName())
            .type("http-proxy")
            .loadBalancer(mapLoadBalancer(source.getLoadBalancer()))
            .endpoints(endpoints)
            //.services(source.getServices())
            .build();
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
            .type(lb.getType())
            .secondary(lb.isBackup())
            .tenants(lb.getTenants())
            .weight(lb.getWeight())
            .configuration(mapConfiguration(lb))
            //.inheritConfiguration(lb.getInheritConfiguration())
            //.sharedConfigurationOverride(lb.getSharedConfigurationOverride())
            //.services(lb.getS)
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
