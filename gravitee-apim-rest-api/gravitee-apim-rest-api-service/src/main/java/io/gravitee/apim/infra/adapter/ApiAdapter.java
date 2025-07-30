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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiAgentEntity;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { PlanAdapter.class })
@DecoratedWith(ApiAdapterDecorator.class)
public interface ApiAdapter {
    Logger log = LoggerFactory.getLogger(ApiAdapter.class);
    ApiAdapter INSTANCE = Mappers.getMapper(ApiAdapter.class);

    @Mapping(target = "apiDefinitionHttpV4", expression = "java(deserializeApiDefinitionV4(source))")
    @Mapping(target = "apiDefinition", expression = "java(deserializeApiDefinitionV2(source))")
    @Mapping(target = "federatedApiDefinition", expression = "java(deserializeFederatedApiDefinition(source))")
    @Mapping(target = "apiDefinitionNativeV4", expression = "java(deserializeNativeApiDefinition(source))")
    Api toCoreModel(io.gravitee.repository.management.model.Api source);

    Stream<Api> toCoreModelStream(Stream<io.gravitee.repository.management.model.Api> source);

    @Mapping(target = "definition", expression = "java(serializeApiDefinition(source))")
    io.gravitee.repository.management.model.Api toRepository(Api source);

    Stream<io.gravitee.repository.management.model.Api> toRepositoryStream(Stream<Api> source);

    @Mapping(target = "apiVersion", source = "version")
    @Mapping(target = "tags", source = "apiDefinitionHttpV4.tags")
    @Mapping(target = "listeners", source = "apiDefinitionHttpV4.listeners")
    @Mapping(target = "endpointGroups", source = "apiDefinitionHttpV4.endpointGroups")
    @Mapping(target = "analytics", source = "apiDefinitionHttpV4.analytics")
    @Mapping(target = "flowExecution", source = "apiDefinitionHttpV4.flowExecution")
    @Mapping(target = "flows", source = "apiDefinitionHttpV4.flows")
    @Mapping(target = "failover", source = "apiDefinitionHttpV4.failover")
    NewApiEntity toNewApiEntity(Api source);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "disableMembershipNotifications", expression = "java(!spec.isNotifyMembers())")
    @Mapping(target = "listeners", expression = "java((List<Listener>) spec.getListeners())")
    @Mapping(target = "endpointGroups", expression = "java((List<EndpointGroup>) spec.getEndpointGroups())")
    @Mapping(target = "flows", expression = "java((List<Flow>) spec.getFlows())")
    UpdateApiEntity toUpdateApiEntity(ApiCRDSpec spec);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(target = "id", source = "api.id")
    @Mapping(target = "name", source = "api.name")
    @Mapping(target = "definitionVersion", source = "api.definitionVersion")
    @Mapping(target = "type", source = "api.type")
    @Mapping(target = "tags", source = "api.tags")
    @Mapping(target = "apiVersion", source = "api.version")
    @Mapping(target = "lifecycleState", source = "api.apiLifecycleState")
    UpdateApiEntity toUpdateApiEntity(Api api, io.gravitee.definition.model.v4.Api apiDefinitionV4);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "listeners", expression = "java((List<Listener>) api.getListeners())")
    ApiEntity toApiEntity(ApiCRDSpec api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "listeners", expression = "java((List<Listener>) api.getApiListeners())")
    ApiEntity toApiEntity(Api api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "tags", source = "apiDefinitionNativeV4.tags")
    @Mapping(target = "listeners", source = "apiDefinitionNativeV4.listeners")
    @Mapping(target = "endpointGroups", source = "apiDefinitionNativeV4.endpointGroups")
    @Mapping(target = "flows", source = "apiDefinitionNativeV4.flows")
    @Mapping(target = "resources", source = "apiDefinitionNativeV4.resources")
    @Mapping(target = "services", source = "apiDefinitionNativeV4.services")
    @Mapping(target = "properties", source = "apiDefinitionNativeV4.properties")
    @Mapping(target = "state", source = "lifecycleState")
    @Mapping(target = "lifecycleState", source = "apiLifecycleState")
    NativeApiEntity toNativeApiEntity(Api api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "source.version", target = "apiVersion")
    @Mapping(source = "source.id", target = "id")
    @Mapping(target = "primaryOwner", source = "primaryOwnerEntity")
    @Mapping(target = "referenceId", source = "source.environmentId")
    @Mapping(target = "referenceType", constant = "ENVIRONMENT")
    @Mapping(source = "source.apiLifecycleState", target = "lifecycleState")
    FederatedApiEntity toFederatedApiEntity(io.gravitee.repository.management.model.Api source, PrimaryOwnerEntity primaryOwnerEntity);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(target = "apiVersion", source = "source.version")
    @Mapping(target = "id", source = "source.id")
    @Mapping(target = "primaryOwner", source = "primaryOwnerEntity")
    @Mapping(target = "referenceId", source = "source.environmentId")
    @Mapping(target = "referenceType", constant = "ENVIRONMENT")
    @Mapping(target = "lifecycleState", source = "source.apiLifecycleState")
    @Mapping(target = "provider", source = "agent.provider")
    @Mapping(target = "defaultInputModes", source = "agent.defaultInputModes")
    @Mapping(target = "defaultOutputModes", source = "agent.defaultOutputModes")
    @Mapping(target = "capabilities", expression = "java(capabilities(agent))")
    @Mapping(target = "skills", source = "agent.skills")
    @Mapping(target = "securitySchemes", source = "agent.securitySchemes")
    @Mapping(target = "security", source = "agent.security")
    @Mapping(target = "name", source = "source.name")
    @Mapping(target = "definitionVersion", source = "source.definitionVersion")
    @Mapping(target = "description", source = "source.description")
    @Mapping(target = "originContext", source = "originContext")
    FederatedApiAgentEntity toFederatedAgentEntity(
        io.gravitee.repository.management.model.Api source,
        FederatedAgent agent,
        PrimaryOwnerEntity primaryOwnerEntity,
        OriginContext.Integration originContext
    );

    default Collection<String> capabilities(FederatedAgent agent) {
        return agent == null || agent.getCapabilities() == null
            ? null
            : agent.getCapabilities().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
    }

    @Mapping(source = "state", target = "lifecycleState")
    @Mapping(source = "lifecycleState", target = "apiLifecycleState")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    Api fromApiEntity(GenericApiEntity apiEntity);

    default <T> T deserialize(io.gravitee.repository.management.model.Api api, Class<T> clazz) {
        if (api.getDefinition() == null) {
            log.debug(
                "The API definition is null for api id {}. This can happen when filtering the definition using ApiFieldFilter",
                api.getId()
            );
            return null;
        }
        try {
            return GraviteeJacksonMapper.getInstance().readValue(api.getDefinition(), clazz);
        } catch (IOException ioe) {
            log.error("An error occurred while deserializing V4 API definition for api id {}", api.getId(), ioe);
            return null;
        }
    }

    default io.gravitee.definition.model.v4.Api deserializeApiDefinitionV4(io.gravitee.repository.management.model.Api api) {
        return api.getDefinitionVersion() == DefinitionVersion.V4 && api.getType() != ApiType.NATIVE
            ? deserialize(api, io.gravitee.definition.model.v4.Api.class)
            : null;
    }

    default io.gravitee.definition.model.Api deserializeApiDefinitionV2(io.gravitee.repository.management.model.Api api) {
        return api.getDefinitionVersion() == DefinitionVersion.V2 || api.getDefinitionVersion() == null
            ? deserialize(api, io.gravitee.definition.model.Api.class)
            : null;
    }

    default io.gravitee.definition.model.federation.FederatedApi deserializeFederatedApiDefinition(
        io.gravitee.repository.management.model.Api api
    ) {
        return api.getDefinitionVersion() == DefinitionVersion.FEDERATED ? deserialize(api, FederatedApi.class) : null;
    }

    default NativeApi deserializeNativeApiDefinition(io.gravitee.repository.management.model.Api api) {
        return api.getDefinitionVersion() == DefinitionVersion.V4 && api.getType() == ApiType.NATIVE
            ? deserialize(api, NativeApi.class)
            : null;
    }

    default String serializeApiDefinition(Api api) {
        return switch (api.getDefinitionVersion()) {
            case V1, V2 -> serialize(api.getApiDefinition(), "V2 API");
            case V4 -> switch (api.getType()) {
                case NATIVE -> serialize(api.getApiDefinitionNativeV4(), "V4 Native API");
                case PROXY, MESSAGE -> serialize(api.getApiDefinitionHttpV4(), "V4 API");
            };
            case FEDERATED -> serialize(api.getFederatedApiDefinition(), "Federated API");
            case FEDERATED_AGENT -> serialize(api.getFederatedAgent(), "Federated Agent");
        };
    }

    default <T> String serialize(T value, String name) {
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(value);
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected error while serializing " + name + " definition", ioe);
        }
    }
}
