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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.utils.ManagementApiLinkHelper;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.v4.api.*;
import jakarta.ws.rs.core.UriInfo;
import java.util.*;
import java.util.function.Function;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
    uses = {
        AnalyticsMapper.class,
        DateMapper.class,
        DefinitionContextMapper.class,
        EndpointMapper.class,
        EntrypointMapper.class,
        FlowMapper.class,
        ListenerMapper.class,
        PlanMapper.class,
        PropertiesMapper.class,
        ResourceMapper.class,
        ResponseTemplateMapper.class,
        RuleMapper.class,
        ServiceMapper.class,
        CorsMapper.class,
    }
)
public interface ApiMapper {
    Logger logger = LoggerFactory.getLogger(ApiMapper.class);
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    // Api
    default Api map(GenericApiEntity apiEntity, UriInfo uriInfo, Boolean isSynchronized) {
        GenericApi.DeploymentStateEnum state = null;

        if (isSynchronized != null) {
            state = isSynchronized ? GenericApi.DeploymentStateEnum.DEPLOYED : GenericApi.DeploymentStateEnum.NEED_REDEPLOY;
        }

        if (apiEntity == null) {
            return null;
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V4) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(this.mapToV4((ApiEntity) apiEntity, uriInfo, state));
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V2) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(
                this.mapToV2((io.gravitee.rest.api.model.api.ApiEntity) apiEntity, uriInfo, state)
            );
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V1) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(
                this.mapToV1((io.gravitee.rest.api.model.api.ApiEntity) apiEntity, uriInfo, state)
            );
        }
        return null;
    }

    default List<Api> map(List<GenericApiEntity> apiEntities, UriInfo uriInfo, Function<GenericApiEntity, Boolean> isSynchronized) {
        var result = new ArrayList<Api>();
        apiEntities.forEach(api -> {
            try {
                result.add(this.map(api, uriInfo, isSynchronized.apply(api)));
            } catch (Exception e) {
                // Ignore APIs throwing conversion issues in the list
                // As v4 was out there in alpha version, we still want to build the list event if some APIs cannot be converted
                logger.error("Unable to convert API {}", api.getId(), e);
            }
        });
        return result;
    }

    @Mapping(target = "listeners", qualifiedByName = "fromListeners")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiV4 mapToV4(ApiEntity apiEntity, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiV2 mapToV2(io.gravitee.rest.api.model.api.ApiEntity apiEntity, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    io.gravitee.rest.api.management.v2.rest.model.ApiV1 mapToV1(
        io.gravitee.rest.api.model.api.ApiEntity apiEntity,
        UriInfo uriInfo,
        GenericApi.DeploymentStateEnum deploymentState
    );

    @Mapping(target = "listeners", qualifiedByName = "fromListeners")
    @Mapping(target = "links", ignore = true)
    ApiV4 map(ApiEntity apiEntity);

    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    ApiEntity map(ApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    NewApiEntity map(CreateApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    @Mapping(target = "plans", qualifiedByName = "toPlanEntity")
    io.gravitee.apim.core.api.model.ApiCRD map(io.gravitee.rest.api.management.v2.rest.model.ApiCRD crd);

    // UpdateApi
    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    @Mapping(target = "id", expression = "java(apiId)")
    UpdateApiEntity map(UpdateApiV4 updateApi, String apiId);

    @Mapping(target = "version", source = "apiVersion")
    @Mapping(target = "graviteeDefinitionVersion", source = "definitionVersion", qualifiedByName = "mapFromDefinitionVersion")
    io.gravitee.rest.api.model.api.UpdateApiEntity map(UpdateApiV2 updateApi);

    // DefinitionVersion
    io.gravitee.definition.model.DefinitionVersion mapDefinitionVersion(DefinitionVersion definitionVersion);

    @Named("mapFromDefinitionVersion")
    default String mapFromDefinitionVersion(DefinitionVersion definitionVersion) {
        if (Objects.isNull(definitionVersion)) {
            return null;
        }
        return io.gravitee.definition.model.DefinitionVersion.valueOf(definitionVersion.name()).getLabel();
    }

    @Named("computeApiLinks")
    default ApiLinks computeApiLinks(GenericApiEntity api, UriInfo uriInfo) {
        return new ApiLinks()
            .pictureUrl(ManagementApiLinkHelper.apiPictureURL(uriInfo.getBaseUriBuilder(), api))
            .backgroundUrl(ManagementApiLinkHelper.apiBackgroundURL(uriInfo.getBaseUriBuilder(), api));
    }

    BaseApi map(GenericApiEntity apiEntity);

    ReviewEntity map(ApiReview apiReview);
}
