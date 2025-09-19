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
package io.gravitee.apim.core.api.model.crd;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.context.OriginContext;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class ApiCRDSpec {

    private String id;

    private String crossId;

    private String name;

    private String description;

    private String version;

    private String type;

    private String state;

    private String lifecycleState;

    private String visibility;

    private DefinitionContext definitionContext;

    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    private Set<String> tags;

    private Set<String> labels;

    private List<Resource> resources;

    private Map<String, PlanCRD> plans;

    private List<? extends AbstractFlow> flows;

    private List<EncryptableProperty> properties;

    private List<ApiMetadata> metadata;

    private List<? extends AbstractListener<? extends AbstractEntrypoint>> listeners;

    private List<@Valid ? extends AbstractEndpointGroup<? extends AbstractEndpoint>> endpointGroups;

    private Analytics analytics;

    private Failover failover;

    private Set<String> groups;

    private Set<MemberCRD> members;

    private boolean notifyMembers;

    private FlowExecution flowExecution;

    private Set<String> categories;

    private ApiServicesCRD services;

    private Map<String, @Valid PageCRD> pages;

    public String getDefinitionVersion() {
        return "V4";
    }

    public boolean isNative() {
        return ApiType.NATIVE.name().equalsIgnoreCase(type);
    }

    /**
     * @return An instance of {@link Api.ApiBuilder} based on the current state of this ApiCRD.
     */
    public Api.ApiBuilder toApiBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return Api.builder()
            .id(id)
            .crossId(crossId)
            .name(name)
            .version(version)
            .definitionVersion(DefinitionVersion.V4)
            .description(description)
            .labels(labels == null ? null : new ArrayList<>(labels))
            .type(ApiType.valueOf(type))
            .apiLifecycleState(Api.ApiLifecycleState.valueOf(lifecycleState))
            .lifecycleState(Api.LifecycleState.valueOf(state))
            .categories(categories)
            .originContext(
                new OriginContext.Kubernetes(
                    OriginContext.Kubernetes.Mode.FULLY_MANAGED,
                    definitionContext.isSyncFromManagement()
                        ? OriginContext.Origin.MANAGEMENT.name()
                        : OriginContext.Origin.KUBERNETES.name()
                )
            )
            .groups(groups);
    }

    /**
     * @return An instance of {@link io.gravitee.definition.model.v4.Api.ApiBuilder} based on the current state of this ApiCRD.
     */
    public io.gravitee.definition.model.v4.Api.ApiBuilder<?, ?> toApiDefinitionBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return io.gravitee.definition.model.v4.Api.builder()
            .analytics(analytics)
            .apiVersion(version)
            .definitionVersion(DefinitionVersion.V4)
            .endpointGroups(endpointGroups != null ? (List<EndpointGroup>) endpointGroups : null)
            .failover(failover)
            .flows(flows != null ? (List<Flow>) flows : null)
            .id(id)
            .listeners(listeners != null ? (List<Listener>) listeners : null)
            .name(name)
            .properties(toProperties(properties))
            .resources(resources)
            .responseTemplates(responseTemplates)
            .plans(toApiPlans(plans))
            .services(services != null ? new ApiServices(services.getDynamicProperty()) : null)
            .tags(tags)
            .type(ApiType.valueOf(type));
    }

    /**
     * @return An instance of {@link io.gravitee.definition.model.v4.nativeapi.NativeApi.NativeApiBuilder} based on the current state of this ApiCRD.
     */
    public io.gravitee.definition.model.v4.nativeapi.NativeApi.NativeApiBuilder<?, ?> toNativeApiDefinitionBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
            .apiVersion(version)
            .definitionVersion(DefinitionVersion.V4)
            .endpointGroups(endpointGroups != null ? (List<NativeEndpointGroup>) endpointGroups : null)
            .flows(flows != null ? (List<NativeFlow>) flows : null)
            .id(id)
            .listeners(listeners != null ? (List<NativeListener>) listeners : null)
            .name(name)
            .properties(toProperties(properties))
            .resources(resources)
            .tags(tags)
            .plans(toNativePlans(plans))
            .services(services != null ? new NativeApiServices(services.getDynamicProperty()) : null)
            .type(ApiType.valueOf(type));
    }

    public List<Path> getPaths() {
        return getListeners()
            .stream()
            .filter(HttpListener.class::isInstance)
            .map(HttpListener.class::cast)
            .flatMap(httpListener ->
                httpListener
                    .getPaths()
                    .stream()
                    .map(path ->
                        Path.builder().host(path.getHost()).path(path.getPath()).overrideAccess(path.isOverrideAccess()).build().sanitize()
                    )
            )
            .toList();
    }

    public List<Property> toProperties(List<EncryptableProperty> properties) {
        return properties
            .stream()
            .map(ep -> Property.builder().key(ep.getKey()).value(ep.getValue()).dynamic(ep.isDynamic()).encrypted(ep.isEncrypted()).build())
            .collect(Collectors.toList());
    }

    public Map<String, Plan> toApiPlans(Map<String, PlanCRD> plans) {
        return plans
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> {
                    PlanCRD planCRD = v.getValue();
                    Plan plan = new Plan();
                    plan.setId(planCRD.getId());
                    plan.setName(planCRD.getName());
                    plan.setTags(planCRD.getTags());
                    plan.setSecurity(planCRD.getSecurity());
                    plan.setSelectionRule(planCRD.getSelectionRule());
                    plan.setStatus(planCRD.getStatus());
                    plan.setFlows((List<Flow>) planCRD.getFlows());

                    return plan;
                })
            );
    }

    public Map<String, NativePlan> toNativePlans(Map<String, PlanCRD> plans) {
        return plans
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> {
                    PlanCRD planCRD = v.getValue();
                    NativePlan nativePlan = new NativePlan();
                    nativePlan.setId(planCRD.getId());
                    nativePlan.setName(planCRD.getName());
                    nativePlan.setTags(planCRD.getTags());
                    nativePlan.setSecurity(planCRD.getSecurity());
                    nativePlan.setSelectionRule(planCRD.getSelectionRule());
                    nativePlan.setStatus(planCRD.getStatus());
                    nativePlan.setFlows((List<NativeFlow>) planCRD.getFlows());

                    return nativePlan;
                })
            );
    }

    public static class ApiCRDSpecBuilder {

        public ApiCRDSpecBuilder paths(List<Path> paths) {
            this.listeners.stream()
                .filter(HttpListener.class::isInstance)
                .map(HttpListener.class::cast)
                .findFirst()
                .ifPresent(listener ->
                    listener.setPaths(
                        paths
                            .stream()
                            .map(path ->
                                io.gravitee.definition.model.v4.listener.http.Path.builder()
                                    .host(path.getHost())
                                    .path(path.getPath())
                                    .overrideAccess(path.isOverrideAccess())
                                    .build()
                            )
                            .toList()
                    )
                );
            return this;
        }
    }
}
