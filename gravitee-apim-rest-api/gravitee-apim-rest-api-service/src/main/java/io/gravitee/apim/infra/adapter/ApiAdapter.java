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
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import java.io.IOException;
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
    Logger LOGGER = LoggerFactory.getLogger(ApiAdapter.class);
    ApiAdapter INSTANCE = Mappers.getMapper(ApiAdapter.class);

    @Mapping(target = "apiDefinitionV4", expression = "java(deserializeApiDefinitionV4(source))")
    @Mapping(target = "apiDefinition", expression = "java(deserializeApiDefinitionV2(source))")
    @Mapping(target = "federatedApiDefinition", expression = "java(deserializeFederatedApiDefinition(source))")
    @Mapping(target = "nativeApiDefinition", expression = "java(deserializeNativeApiDefinition(source))")
    Api toCoreModel(io.gravitee.repository.management.model.Api source);

    Stream<Api> toCoreModelStream(Stream<io.gravitee.repository.management.model.Api> source);

    @Mapping(target = "definition", expression = "java(serializeApiDefinition(source))")
    io.gravitee.repository.management.model.Api toRepository(Api source);

    Stream<io.gravitee.repository.management.model.Api> toRepositoryStream(Stream<Api> source);

    @Mapping(target = "apiVersion", source = "version")
    @Mapping(target = "tags", source = "apiDefinitionV4.tags")
    @Mapping(target = "listeners", source = "apiDefinitionV4.listeners")
    @Mapping(target = "endpointGroups", source = "apiDefinitionV4.endpointGroups")
    @Mapping(target = "analytics", source = "apiDefinitionV4.analytics")
    @Mapping(target = "flowExecution", source = "apiDefinitionV4.flowExecution")
    @Mapping(target = "flows", source = "apiDefinitionV4.flows")
    NewApiEntity toNewApiEntity(Api source);

    @Mapping(target = "apiVersion", source = "version")
    io.gravitee.definition.model.v4.Api toApiDefinition(ApiCRDSpec source);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "disableMembershipNotifications", expression = "java(!spec.isNotifyMembers())")
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
    ApiEntity toApiEntity(ApiCRDSpec api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    ApiEntity toApiEntity(Api api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "source.version", target = "apiVersion")
    @Mapping(source = "source.id", target = "id")
    @Mapping(target = "primaryOwner", source = "primaryOwnerEntity")
    @Mapping(target = "referenceId", source = "source.environmentId")
    @Mapping(target = "referenceType", constant = "ENVIRONMENT")
    @Mapping(source = "source.apiLifecycleState", target = "lifecycleState")
    FederatedApiEntity toFederatedApiEntity(io.gravitee.repository.management.model.Api source, PrimaryOwnerEntity primaryOwnerEntity);

    @Mapping(source = "state", target = "lifecycleState")
    @Mapping(source = "lifecycleState", target = "apiLifecycleState")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    Api fromApiEntity(GenericApiEntity apiEntity);

    default <T> T deserialize(io.gravitee.repository.management.model.Api api, Class<T> clazz) {
        if (api.getDefinition() == null) {
            // This can happen when filtering the definition using ApiFieldFilter
            return null;
        }

        try {
            return GraviteeJacksonMapper.getInstance().readValue(api.getDefinition(), clazz);
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while deserializing V4 API definition", ioe);
            return null;
        }
    }

    default io.gravitee.definition.model.v4.Api deserializeApiDefinitionV4(io.gravitee.repository.management.model.Api api) {
        return api.getDefinitionVersion() == DefinitionVersion.V4 && api.getType() != ApiType.NATIVE
            ? deserialize(api, io.gravitee.definition.model.v4.Api.class)
            : null;
    }

    default io.gravitee.definition.model.Api deserializeApiDefinitionV2(io.gravitee.repository.management.model.Api api) {
        return api.getDefinitionVersion() != DefinitionVersion.V4 && api.getDefinitionVersion() != DefinitionVersion.FEDERATED
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
                case NATIVE -> serialize(api.getNativeApiDefinition(), "V4 Native API");
                case PROXY, MESSAGE -> serialize(api.getApiDefinitionV4(), "V4 API");
            };
            case FEDERATED -> serialize(api.getFederatedApiDefinition(), "Federated API");
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
