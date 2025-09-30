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
package io.gravitee.apim.core.api.model.factory;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewHttpApi;
import io.gravitee.apim.core.api.model.NewNativeApi;
import io.gravitee.apim.core.api.model.UpdateNativeApi;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.stream.Collectors;

public class ApiModelFactory {

    private ApiModelFactory() {}

    public static Api fromNewHttpApi(NewHttpApi newHttpApi, String environmentId) {
        var id = UuidString.generateRandom();
        var now = TimeProvider.now();
        return newHttpApi
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .apiDefinitionHttpV4(newHttpApi.toApiDefinitionBuilder().id(id).build())
            .lifecycleState(Api.LifecycleState.STOPPED)
            .build();
    }

    public static Api fromNewNativeApi(NewNativeApi newNativeApi, String environmentId) {
        var id = UuidString.generateRandom();
        var now = TimeProvider.now();
        return newNativeApi
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .apiDefinitionNativeV4(newNativeApi.toNativeApiDefinitionBuilder().id(id).build())
            .lifecycleState(Api.LifecycleState.STOPPED)
            .build();
    }

    public static Api fromCrd(ApiCRDSpec crd, String environmentId) {
        var id = crd.getId() != null ? crd.getId() : UuidString.generateRandom();
        var now = TimeProvider.now();
        Api api = crd
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .visibility(Api.Visibility.valueOf(crd.getVisibility()))
            .disableMembershipNotifications(!crd.isNotifyMembers())
            .build();

        var definition = api.isNative() ? crd.toNativeApiDefinitionBuilder().id(id).build() : crd.toApiDefinitionBuilder().id(id).build();
        api.setApiDefinitionValue(definition);

        return api;
    }

    public static UpdateNativeApi toUpdateNativeApi(ApiCRDSpec spec) {
        return UpdateNativeApi.builder()
            .id(spec.getId())
            .name(spec.getName())
            .description(spec.getDescription())
            .visibility(Api.Visibility.valueOf(spec.getVisibility()))
            .resources(spec.getResources())
            .disableMembershipNotifications(!spec.isNotifyMembers())
            .endpointGroups(spec.getEndpointGroups() != null ? (List<NativeEndpointGroup>) spec.getEndpointGroups() : null)
            .apiVersion(spec.getVersion())
            .definitionVersion(DefinitionVersion.valueOf(spec.getDefinitionVersion()))
            .categories(spec.getCategories())
            .groups(spec.getGroups())
            .labels(spec.getLabels() != null ? spec.getLabels().stream().collect(Collectors.toList()) : null)
            .lifecycleState(Api.ApiLifecycleState.valueOf(spec.getLifecycleState()))
            .services(spec.getServices() != null ? new NativeApiServices(spec.getServices().getDynamicProperty()) : null)
            .tags(spec.getTags())
            .listeners(spec.getListeners() != null ? (List<NativeListener>) spec.getListeners() : null)
            .flows(spec.getFlows() != null ? (List<NativeFlow>) spec.getFlows() : null)
            .properties(spec.getProperties())
            .build();
    }

    public static Api fromApiExport(ApiExport api, String environmentId) {
        var id = api.getId() != null ? api.getId() : UuidString.generateRandom();
        var now = TimeProvider.now();
        var apiExport = api
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .definitionVersion(api.getDefinitionVersion())
            .type(api.getType())
            .createdAt(now)
            .updatedAt(now)
            .lifecycleState(Api.LifecycleState.STOPPED)
            .apiLifecycleState(Api.ApiLifecycleState.CREATED)
            .visibility(api.getVisibility() == null ? Api.Visibility.PRIVATE : Api.Visibility.valueOf(api.getVisibility().name()));

        if (api.getType() == ApiType.NATIVE) {
            return apiExport.apiDefinitionNativeV4(api.toNativeApiDefinitionBuilder().id(id).build()).build();
        }

        return apiExport.apiDefinitionHttpV4(api.toApiDefinitionBuilder().id(id).build()).build();
    }

    public static Api fromIntegration(IntegrationApi integrationApi, Integration integration) {
        var id = generateFederatedApiId(integrationApi, integration);
        var now = TimeProvider.now();
        var defaultVersion = "0.0.0";
        var version = integrationApi.version() != null ? integrationApi.version() : defaultVersion;
        return Api.builder()
            .id(id)
            .version(version)
            .definitionVersion(DefinitionVersion.FEDERATED)
            .name(integrationApi.name())
            .description(integrationApi.description())
            .createdAt(now)
            .updatedAt(now)
            .environmentId(integration.environmentId())
            .lifecycleState(null)
            .originContext(new OriginContext.Integration(integration.id(), integration.name(), integration.provider()))
            .federatedApiDefinition(integrationApi.toFederatedApiDefinitionBuilder().id(id).build())
            .build();
    }

    public static Api fromIngestionJob(IntegrationApi integrationApi, AsyncJob job, Integration integration) {
        var id = generateFederatedApiId(integrationApi, job);
        var now = TimeProvider.now();
        var defaultVersion = "0.0.0";
        var version = integrationApi.version() != null ? integrationApi.version() : defaultVersion;
        return Api.builder()
            .id(id)
            .version(version)
            .definitionVersion(DefinitionVersion.FEDERATED)
            .name(integrationApi.name())
            .description(integrationApi.description())
            .createdAt(now)
            .updatedAt(now)
            .environmentId(job.getEnvironmentId())
            .lifecycleState(null)
            .originContext(new OriginContext.Integration(job.getSourceId(), integration.name(), integration.provider()))
            .federatedApiDefinition(integrationApi.toFederatedApiDefinitionBuilder().id(id).build())
            .build();
    }

    /**
     * Generate the Federated API identifier.
     *
     * <p>
     *     The id is not randomly generated it is based on
     *     <ul>
     *         <li>environment id</li>
     *         <li>integration id</li>
     *         <li>external API unique id</li>
     *     </ul>
     * </p>
     * <p>
     *     The combination should produce a unique id that can be recreated to check if a specific API has been already
     *     ingested so we can ignore it.
     * </p>
     * @param integrationApi The external API
     * @param integration The integration
     * @return The generated id
     */
    public static String generateFederatedApiId(IntegrationApi integrationApi, Integration integration) {
        return generateFederatedApiId(integration.environmentId(), integration.id(), integrationApi);
    }

    public static String generateFederatedApiId(String environmentId, String integrationId, IntegrationApi integrationApi) {
        return UuidString.generateForEnvironment(environmentId, integrationId, integrationApi.uniqueId());
    }

    /**
     * Generate the Federated API identifier.
     *
     * <p>
     *     The id is not randomly generated it is based on
     *     <ul>
     *         <li>environment id</li>
     *         <li>integration id</li>
     *         <li>external API unique id</li>
     *     </ul>
     * </p>
     * <p>
     *     The combination should produce a unique id that can be recreated to check if a specific API has been already
     *     ingested so we can ignore it.
     * </p>
     * @param integrationApi The external API
     * @param job The job that ingested the API
     * @return The generated id
     */
    public static String generateFederatedApiId(IntegrationApi integrationApi, AsyncJob job) {
        return UuidString.generateForEnvironment(job.getEnvironmentId(), job.getSourceId(), integrationApi.uniqueId());
    }
}
