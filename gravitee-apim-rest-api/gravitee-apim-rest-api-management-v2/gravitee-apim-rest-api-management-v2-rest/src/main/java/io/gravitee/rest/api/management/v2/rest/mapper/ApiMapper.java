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
import io.gravitee.apim.core.api.model.UpdateNativeApi;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.service.AbstractApiServices;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.ApiFederatedAgent;
import io.gravitee.rest.api.management.v2.rest.model.ApiLinks;
import io.gravitee.rest.api.management.v2.rest.model.ApiReview;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.BaseApi;
import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.ExposedEntrypoint;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.management.v2.rest.model.IngestedApi;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.PageCRD;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.management.v2.rest.utils.ManagementApiLinkHelper;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiAgentEntity;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    @Nullable
    default Api map(GenericApiEntity apiEntity, UriInfo uriInfo, Boolean isSynchronized) {
        GenericApi.DeploymentStateEnum state = null;

        if (isSynchronized != null) {
            state = isSynchronized ? GenericApi.DeploymentStateEnum.DEPLOYED : GenericApi.DeploymentStateEnum.NEED_REDEPLOY;
        }

        return switch (apiEntity) {
            case FederatedApiAgentEntity federatedAgent -> new io.gravitee.rest.api.management.v2.rest.model.Api(
                mapToFederatedAgent(federatedAgent, uriInfo)
            );
            case FederatedApiEntity federatedApi -> new io.gravitee.rest.api.management.v2.rest.model.Api(
                mapToFederated(federatedApi, uriInfo)
            );
            case ApiEntity asApiEntity -> new Api(mapToV4(asApiEntity, uriInfo, state));
            case NativeApiEntity asNativeApiEntity -> new Api(mapToV4(asNativeApiEntity, uriInfo, state));
            case io.gravitee.rest.api.model.api.ApiEntity legacy -> legacy.getDefinitionVersion() ==
                io.gravitee.definition.model.DefinitionVersion.V1
                ? new io.gravitee.rest.api.management.v2.rest.model.Api(mapToV1(legacy, uriInfo, state))
                : new io.gravitee.rest.api.management.v2.rest.model.Api(mapToV2(legacy, uriInfo, state));
            case null, default -> null;
        };
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

    @Mapping(target = "originContext", expression = "java(computeOriginContext(apiEntity))")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    @Mapping(target = "url", source = "apiEntity.url")
    @Mapping(target = "documentationUrl", source = "apiEntity.documentationUrl")
    @Mapping(target = "provider", source = "apiEntity.provider")
    @Mapping(target = "defaultInputModes", source = "apiEntity.defaultInputModes")
    @Mapping(target = "defaultOutputModes", source = "apiEntity.defaultOutputModes")
    @Mapping(target = "capabilities", source = "apiEntity.capabilities")
    @Mapping(target = "skills", source = "apiEntity.skills")
    @Mapping(target = "securitySchemes", source = "apiEntity.securitySchemes")
    @Mapping(target = "security", source = "apiEntity.security")
    @Mapping(target = "definitionVersion", source = "apiEntity.definitionVersion")
    ApiFederatedAgent mapToFederatedAgent(FederatedApiAgentEntity apiEntity, UriInfo uriInfo);

    @Mapping(target = "definitionContext", source = "apiEntity.originContext")
    @Mapping(target = "listeners", qualifiedByName = "fromHttpListeners")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiV4 mapToV4(ApiEntity apiEntity, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "definitionContext", source = "apiEntity.originContext")
    @Mapping(target = "listeners", qualifiedByName = "fromHttpListeners")
    ApiV4 mapToV4(ApiEntity apiEntity);

    @Mapping(target = "definitionContext", source = "apiEntity.originContext")
    @Mapping(target = "listeners", qualifiedByName = "fromNativeListeners")
    @Mapping(target = "links", expression = "java(computeApiLinks(apiEntity, uriInfo))")
    ApiV4 mapToV4(NativeApiEntity apiEntity, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "definitionContext", source = "apiEntity.originContext")
    @Mapping(target = "listeners", qualifiedByName = "fromNativeListeners")
    ApiV4 mapToV4(NativeApiEntity apiEntity);

    default ApiV4 mapToV4(io.gravitee.apim.core.api.model.Api source, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState) {
        if (ApiType.NATIVE.equals(source.getType())) {
            return mapToNativeV4(source, uriInfo, deploymentState);
        }
        return mapToHttpV4(source, uriInfo, deploymentState);
    }

    default ApiV4 mapToV4(GenericApiEntity genericApiEntity) {
        return switch (genericApiEntity) {
            case ApiEntity asApiEntity -> mapToV4(asApiEntity);
            case NativeApiEntity asNativeApiEntity -> mapToV4(asNativeApiEntity);
            case null, default -> null;
        };
    }

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
    ApiV4 mapToHttpV4(io.gravitee.apim.core.api.model.Api source, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "definitionContext", source = "source.originContext")
    @Mapping(target = "apiVersion", source = "source.version")
    @Mapping(target = "deploymentState", source = "deploymentState")
    @Mapping(target = "endpointGroups", source = "source.apiDefinitionNativeV4.endpointGroups")
    @Mapping(target = "flows", source = "source.apiDefinitionNativeV4.flows")
    @Mapping(target = "lifecycleState", source = "source.apiLifecycleState")
    @Mapping(target = "links", expression = "java(computeCoreApiLinks(source, uriInfo))")
    @Mapping(target = "listeners", source = "source.apiDefinitionNativeV4.listeners", qualifiedByName = "fromNativeListeners")
    @Mapping(target = "state", source = "source.lifecycleState")
    @Mapping(target = "analytics", source = "source.apiDefinitionNativeV4.analytics")
    ApiV4 mapToNativeV4(io.gravitee.apim.core.api.model.Api source, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

    @Mapping(target = "definitionContext", source = "source.originContext")
    @Mapping(target = "apiVersion", source = "source.version")
    @Mapping(target = "analytics", source = "source.apiDefinitionHttpV4.analytics")
    @Mapping(target = "deploymentState", source = "deploymentState")
    @Mapping(target = "endpointGroups", source = "source.apiDefinitionHttpV4.endpointGroups")
    @Mapping(target = "flowExecution", source = "source.apiDefinitionHttpV4.flowExecution")
    @Mapping(target = "flows", source = "source.flows", qualifiedByName = "mapToFlowV4List")
    @Mapping(target = "lifecycleState", source = "source.apiLifecycleState")
    @Mapping(target = "links", expression = "java(computeCoreApiLinks(source, uriInfo))")
    @Mapping(target = "listeners", source = "source.apiDefinitionHttpV4.listeners", qualifiedByName = "fromHttpListeners")
    @Mapping(target = "state", source = "source.lifecycleState")
    ApiV4 mapToV4(io.gravitee.apim.core.api.model.ApiWithFlows source, UriInfo uriInfo, GenericApi.DeploymentStateEnum deploymentState);

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
    ApiV4 mapFromHttpApiEntity(ApiEntity apiEntity);

    @Mapping(target = "listeners", qualifiedByName = "fromNativeListeners")
    @Mapping(target = "links", ignore = true)
    ApiV4 mapFromNativeApiEntity(NativeApiEntity apiEntity);

    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    ApiEntity map(ApiV4 api);

    @Mapping(target = "flows", expression = "java(mapApiV4Flows(api))")
    @Mapping(target = "listeners", expression = "java(mapApiV4Listeners(api))")
    @Mapping(target = "endpointGroups", expression = "java(mapApiV4EndpointGroups(api))")
    @Mapping(target = "services", expression = "java(mapApiV4Services(api))")
    ApiExport toApiExport(ApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toHttpListeners")
    NewHttpApi mapToNewHttpApi(CreateApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toNativeListeners")
    NewNativeApi mapToNewNativeApi(CreateApiV4 api);

    @Mapping(target = "plans", expression = "java(mapPlanCRD(crd))")
    @Mapping(target = "flows", expression = "java(mapApiCRDFlows(crd))")
    @Mapping(target = "listeners", expression = "java(mapApiCRDListeners(crd))")
    @Mapping(target = "endpointGroups", expression = "java(mapApiCRDEndpointGroups(crd))")
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

    @Mapping(target = "listeners", qualifiedByName = "toNativeListeners")
    @Mapping(target = "id", expression = "java(apiId)")
    UpdateNativeApi mapToUpdateNativeApi(UpdateApiV4 api, String apiId);

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

    default Map<String, io.gravitee.apim.core.api.model.crd.PlanCRD> mapPlanCRD(
        io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec spec
    ) {
        return spec
            .getPlans()
            .entrySet()
            .stream()
            .map(entry -> {
                String key = entry.getKey();
                var plan = entry.getValue();
                return Map.entry(key, PlanMapper.INSTANCE.fromPlanCRD(plan, spec.getType().name()));
            })
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));
    }

    default List<? extends AbstractFlow> mapApiCRDFlows(io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec spec) {
        if (CollectionUtils.isEmpty(spec.getFlows())) {
            return List.of();
        }

        if (ApiType.NATIVE.name().equalsIgnoreCase(spec.getType().name())) {
            return FlowMapper.INSTANCE.mapToNativeV4(spec.getFlows());
        } else {
            return FlowMapper.INSTANCE.mapToHttpV4(spec.getFlows());
        }
    }

    default List<? extends AbstractListener<? extends AbstractEntrypoint>> mapApiCRDListeners(
        io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec spec
    ) {
        if (CollectionUtils.isEmpty(spec.getListeners())) {
            return List.of();
        }

        if (ApiType.NATIVE.name().equalsIgnoreCase(spec.getType().name())) {
            return ListenerMapper.INSTANCE.mapToNativeListenerV4List(spec.getListeners());
        } else {
            return ListenerMapper.INSTANCE.mapToListenerEntityV4List(spec.getListeners());
        }
    }

    default List<? extends AbstractEndpointGroup<? extends AbstractEndpoint>> mapApiCRDEndpointGroups(
        io.gravitee.rest.api.management.v2.rest.model.ApiCRDSpec spec
    ) {
        if (CollectionUtils.isEmpty(spec.getEndpointGroups())) {
            return List.of();
        }

        if (ApiType.NATIVE.name().equalsIgnoreCase(spec.getType().name())) {
            return EndpointMapper.INSTANCE.mapEndpointGroupsNativeV4(spec.getEndpointGroups());
        } else {
            return EndpointMapper.INSTANCE.mapEndpointGroupsHttpV4(spec.getEndpointGroups());
        }
    }

    default List<? extends AbstractFlow> mapApiV4Flows(ApiV4 apiV4) {
        if (CollectionUtils.isEmpty(apiV4.getFlows())) {
            return List.of();
        }

        if (io.gravitee.rest.api.management.v2.rest.model.ApiType.NATIVE.equals(apiV4.getType())) {
            return FlowMapper.INSTANCE.mapToNativeV4(apiV4.getFlows());
        } else {
            return FlowMapper.INSTANCE.mapToHttpV4(apiV4.getFlows());
        }
    }

    default List<? extends AbstractListener<? extends AbstractEntrypoint>> mapApiV4Listeners(ApiV4 apiV4) {
        if (CollectionUtils.isEmpty(apiV4.getListeners())) {
            return List.of();
        }

        if (io.gravitee.rest.api.management.v2.rest.model.ApiType.NATIVE.equals(apiV4.getType())) {
            return ListenerMapper.INSTANCE.mapToNativeListenerV4List(apiV4.getListeners());
        } else {
            return ListenerMapper.INSTANCE.mapToListenerEntityV4List(apiV4.getListeners());
        }
    }

    default List<? extends AbstractEndpointGroup<? extends AbstractEndpoint>> mapApiV4EndpointGroups(ApiV4 apiV4) {
        if (CollectionUtils.isEmpty(apiV4.getEndpointGroups())) {
            return List.of();
        }

        if (io.gravitee.rest.api.management.v2.rest.model.ApiType.NATIVE.equals(apiV4.getType())) {
            return EndpointMapper.INSTANCE.mapEndpointGroupsNativeV4(apiV4.getEndpointGroups());
        } else {
            return EndpointMapper.INSTANCE.mapEndpointGroupsHttpV4(apiV4.getEndpointGroups());
        }
    }

    default AbstractApiServices mapApiV4Services(ApiV4 apiV4) {
        if (apiV4.getServices() == null) {
            return null;
        }

        if (io.gravitee.rest.api.management.v2.rest.model.ApiType.NATIVE.equals(apiV4.getType())) {
            return ServiceMapper.INSTANCE.mapToNativeApiServices(apiV4.getServices());
        } else {
            return ServiceMapper.INSTANCE.mapToApiServices(apiV4.getServices());
        }
    }

    @Named("computeOriginContext")
    default BaseOriginContext computeOriginContext(GenericApiEntity api) {
        return switch (api.getOriginContext()) {
            case OriginContext.Kubernetes kube -> {
                var ctx = new KubernetesOriginContext();
                ctx.origin(BaseOriginContext.OriginEnum.KUBERNETES);
                if (kube.mode() == OriginContext.Kubernetes.Mode.FULLY_MANAGED) {
                    ctx.mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED);
                }
                switch (kube.syncFrom().toUpperCase()) {
                    case "KUBERNETES" -> ctx.setSyncFrom(KubernetesOriginContext.SyncFromEnum.KUBERNETES);
                    case "MANAGEMENT" -> ctx.setSyncFrom(KubernetesOriginContext.SyncFromEnum.MANAGEMENT);
                }
                yield ctx;
            }
            case OriginContext.Management ignored -> new ManagementOriginContext().origin(BaseOriginContext.OriginEnum.MANAGEMENT);
            case OriginContext.Integration inte -> {
                var ctx = new IntegrationOriginContext();
                ctx.origin(BaseOriginContext.OriginEnum.INTEGRATION);
                ctx.integrationId(inte.integrationId());
                ctx.integrationName(inte.integrationName());
                ctx.provider(inte.provider());
                yield ctx;
            }
            case null -> null;
        };
    }

    BaseApi map(GenericApiEntity apiEntity);

    ReviewEntity map(ApiReview apiReview);

    IngestedApi map(io.gravitee.apim.core.api.model.Api api);

    ExposedEntrypoint map(io.gravitee.apim.core.api.model.ExposedEntrypoint entrypoint);

    List<ExposedEntrypoint> map(List<io.gravitee.apim.core.api.model.ExposedEntrypoint> entrypoints);

    @Named("mapToFlowV4List")
    default List<FlowV4> mapToFlowV4List(List<? extends AbstractFlow> flows) {
        if (flows == null) {
            return null;
        }
        return flows
            .stream()
            .map(flow -> {
                if (flow instanceof io.gravitee.definition.model.v4.flow.Flow httpFlow) {
                    return FlowMapper.INSTANCE.mapFromHttpV4(httpFlow);
                } else if (flow instanceof io.gravitee.definition.model.v4.nativeapi.NativeFlow nativeFlow) {
                    return FlowMapper.INSTANCE.mapFromNativeV4(nativeFlow);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
    }
}
