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

import static java.util.stream.Collectors.toMap;

import io.gravitee.apim.core.api.model.NewHttpApi;
import io.gravitee.apim.core.api.model.NewNativeApi;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.ApiLinks;
import io.gravitee.rest.api.management.v2.rest.model.ApiReview;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.BaseApi;
import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.management.v2.rest.model.IngestedApi;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.PageCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanCRD;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.management.v2.rest.utils.ManagementApiLinkHelper;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
    uses = {
        AnalyticsMapper.class,
        DateMapper.class,
        DefinitionContextMapper.class,
        OriginContextMapper.class,
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
        ConfigurationSerializationMapper.class,
    }
)
public interface ApiMapper {
    Logger logger = LoggerFactory.getLogger(ApiMapper.class);
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    // Api
    default Api map(io.gravitee.apim.core.api.model.Api api, UriInfo uriInfo, Boolean isSynchronized) {
        GenericApi.DeploymentStateEnum state = null;

        if (isSynchronized != null) {
            state = isSynchronized ? GenericApi.DeploymentStateEnum.DEPLOYED : GenericApi.DeploymentStateEnum.NEED_REDEPLOY;
        }

        if (api != null && api.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V4) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(this.mapToV4(api, uriInfo, state));
        }
        return null;
    }

    default Api map(GenericApiEntity apiEntity, UriInfo uriInfo, Boolean isSynchronized) {
        GenericApi.DeploymentStateEnum state = null;

        if (isSynchronized != null) {
            state = isSynchronized ? GenericApi.DeploymentStateEnum.DEPLOYED : GenericApi.DeploymentStateEnum.NEED_REDEPLOY;
        }

        if (apiEntity == null) {
            return null;
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.FEDERATED) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(this.mapToFederated((FederatedApiEntity) apiEntity, uriInfo));
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

    @Mapping(target = "originContext", expression = "java(computeOriginContext(apiEntity))")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiFederated mapToFederated(FederatedApiEntity apiEntity, UriInfo uriInfo);

    @Mapping(target = "definitionContext", source = "apiEntity.originContext")
    @Mapping(target = "listeners", qualifiedByName = "fromHttpListeners")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiV4 mapToV4(ApiEntity apiEntity, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "definitionContext", source = "source.originContext")
    @Mapping(target = "apiVersion", source = "source.version")
    @Mapping(target = "analytics", source = "source.apiDefinitionHttpV4.analytics")
    @Mapping(target = "deploymentState", source = "deploymentState")
    @Mapping(target = "endpointGroups", source = "source.apiDefinitionHttpV4.endpointGroups")
    @Mapping(target = "flowExecution", source = "source.apiDefinitionHttpV4.flowExecution")
    @Mapping(target = "flows", source = "source.apiDefinitionHttpV4.flows")
    @Mapping(target = "lifecycleState", source = "source.apiLifecycleState")
    @Mapping(target = "links", expression = "java(computeCoreApiLinks(source, uriInfo))")
    @Mapping(target = "listeners", source = "source.apiDefinitionHttpV4.listeners", qualifiedByName = "fromHttpListeners")
    @Mapping(target = "state", source = "source.lifecycleState")
    ApiV4 mapToV4(io.gravitee.apim.core.api.model.Api source, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "definitionContext", source = "apiEntity.originContext")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiV2 mapToV2(io.gravitee.rest.api.model.api.ApiEntity apiEntity, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    io.gravitee.rest.api.management.v2.rest.model.ApiV1 mapToV1(
        io.gravitee.rest.api.model.api.ApiEntity apiEntity,
        UriInfo uriInfo,
        GenericApi.DeploymentStateEnum deploymentState
    );

    @Mapping(target = "listeners", qualifiedByName = "fromHttpListeners")
    @Mapping(target = "links", ignore = true)
    ApiV4 map(ApiEntity apiEntity);

    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    ApiEntity map(ApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    ApiExport toApiExport(ApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    NewHttpApi mapToNewHttpApi(CreateApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toNativeListeners")
    NewNativeApi mapToNewNativeApi(CreateApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    @Mapping(target = "plans", qualifiedByName = "mapPlanCRD")
    ApiCRDSpec map(io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec crd);

    @Mapping(target = "source.configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "source.configurationMap", source = "source.configuration", qualifiedByName = "convertToMapConfiguration")
    io.gravitee.apim.core.api.model.crd.PageCRD map(PageCRD crd);

    Page map(io.gravitee.apim.core.api.model.crd.PageCRD crd);

    io.gravitee.apim.core.api.model.crd.PageCRD map(Page crd);

    // UpdateApi
    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    @Mapping(target = "id", expression = "java(apiId)")
    UpdateApiEntity map(UpdateApiV4 updateApi, String apiId);

    @Mapping(target = "id", expression = "java(apiId)")
    UpdateApiEntity map(UpdateApiFederated updateApi, String apiId);

    @Mapping(source = "updateApiFederated.lifecycleState", target = "apiLifecycleState")
    @Mapping(source = "updateApiFederated.apiVersion", target = "version")
    @Mapping(source = "apiId", target = "id")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    io.gravitee.apim.core.api.model.Api mapToApiCore(UpdateApiFederated updateApiFederated, String apiId);

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

    @Named("computeCoreApiLinks")
    default ApiLinks computeCoreApiLinks(io.gravitee.apim.core.api.model.Api api, UriInfo uriInfo) {
        return new ApiLinks()
            .pictureUrl(ManagementApiLinkHelper.apiPictureURL(uriInfo.getBaseUriBuilder(), api))
            .backgroundUrl(ManagementApiLinkHelper.apiBackgroundURL(uriInfo.getBaseUriBuilder(), api));
    }

    @Named("mapPlanCRD")
    default Map<String, io.gravitee.apim.core.api.model.crd.PlanCRD> mapPlanCRD(Map<String, PlanCRD> plans) {
        return plans
            .entrySet()
            .stream()
            .map(entry -> {
                var key = entry.getKey();
                var plan = entry.getValue();
                return Map.entry(key, PlanMapper.INSTANCE.fromPlanCRD(plan));
            })
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Named("computeOriginContext")
    default BaseOriginContext computeOriginContext(GenericApiEntity api) {
        if (api.getOriginContext() == null) {
            return null;
        }
        if (api.getOriginContext() instanceof OriginContext.Kubernetes kube) {
            var ctx = new KubernetesOriginContext();
            ctx.origin(BaseOriginContext.OriginEnum.KUBERNETES);
            if (kube.mode() == OriginContext.Kubernetes.Mode.FULLY_MANAGED) {
                ctx.mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED);
            }
            switch (kube.syncFrom().toUpperCase()) {
                case "KUBERNETES" -> ctx.setSyncFrom(KubernetesOriginContext.SyncFromEnum.KUBERNETES);
                case "MANAGEMENT" -> ctx.setSyncFrom(KubernetesOriginContext.SyncFromEnum.MANAGEMENT);
            }
            return ctx;
        } else if (api.getOriginContext() instanceof OriginContext.Management) {
            return new ManagementOriginContext().origin(BaseOriginContext.OriginEnum.MANAGEMENT);
        } else if (api.getOriginContext() instanceof OriginContext.Integration inte) {
            var ctx = new IntegrationOriginContext();
            ctx.origin(BaseOriginContext.OriginEnum.INTEGRATION);
            ctx.integrationId(inte.integrationId());
            if (inte instanceof FederatedApiEntity.OriginContextView view) {
                ctx.provider(view.provider());
                ctx.integrationName(view.integrationName());
            }
            return ctx;
        }
        return null;
    }

    BaseApi map(GenericApiEntity apiEntity);

    ReviewEntity map(ApiReview apiReview);

    IngestedApi map(io.gravitee.apim.core.api.model.Api api);
}
