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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ApiAdapter.class, PlanAdapter.class, MemberAdapter.class, MetadataAdapter.class, PageAdapter.class })
public interface GraviteeDefinitionAdapter {
    GraviteeDefinitionAdapter INSTANCE = Mappers.getMapper(GraviteeDefinitionAdapter.class);
    Logger logger = LoggerFactory.getLogger(GraviteeDefinitionAdapter.class);

    List<PageExport> mapPage(Collection<Page> source);

    @Mapping(
        target = "security",
        expression = "java(source.getPlanDefinitionHttpV4() != null ? mapPlanSecurity(source.getPlanDefinitionHttpV4().getSecurity()) : null)"
    )
    @Mapping(target = "mode", source = "planDefinitionHttpV4.mode")
    @Mapping(target = "status", source = "planDefinitionHttpV4.status")
    PlanDescriptor.V4 mapPlanV4(Plan source);

    @Mapping(target = "security", expression = "java(mapPlanSecurity(source.getPlanDefinitionNativeV4().getSecurity()))")
    @Mapping(target = "mode", source = "planDefinitionNativeV4.mode")
    @Mapping(target = "status", source = "planDefinitionNativeV4.status")
    PlanDescriptor.Native mapPlanNative(Plan source);

    @Mapping(target = "security", expression = "java(mapPlanSecurity(source.getFederatedPlanDefinition().getSecurity()))")
    @Mapping(target = "mode", source = "federatedPlanDefinition.mode")
    @Mapping(target = "status", source = "federatedPlanDefinition.status")
    @Mapping(target = "providerId", source = "federatedPlanDefinition.providerId")
    PlanDescriptor.Federated mapPlanFederated(Plan source);

    @Mapping(
        target = "security",
        expression = "java(mapPlanSecurityV2(source.getPlanDefinitionV2().getSecurity(), source.getPlanDefinitionV2().getSecurityDefinition()))"
    )
    @Mapping(target = "status", source = "planDefinitionV2.status")
    @Mapping(target = "securityDefinition", source = "planDefinitionV2.securityDefinition")
    @Mapping(target = "paths", source = "planDefinitionV2.paths")
    @Mapping(target = "flows", source = "planDefinitionV2.flows")
    PlanDescriptor.V2 mapPlanV2(Plan source);

    io.gravitee.rest.api.model.PrimaryOwnerEntity map(PrimaryOwnerEntity src);

    @Mapping(target = "id", source = "apiEntity.id")
    @Mapping(target = "apiVersion", source = "apiEntity.version")
    @Mapping(target = "type", source = "apiEntity.type")
    @Mapping(target = "state", source = "apiEntity.lifecycleState")
    @Mapping(target = "lifecycleState", source = "apiEntity.apiLifecycleState")
    @Mapping(target = "listeners", source = "apiEntity.apiDefinitionHttpV4.listeners")
    @Mapping(target = "analytics", source = "apiEntity.apiDefinitionHttpV4.analytics")
    @Mapping(target = "flowExecution", source = "apiEntity.apiDefinitionHttpV4.flowExecution")
    @Mapping(target = "flows", source = "apiEntity.apiDefinitionHttpV4.flows")
    @Mapping(target = "responseTemplates", source = "apiEntity.apiDefinitionHttpV4.responseTemplates")
    @Mapping(target = "properties", source = "apiEntity.apiDefinitionHttpV4.properties")
    @Mapping(target = "resources", source = "apiEntity.apiDefinitionHttpV4.resources")
    @Mapping(
        target = "failover",
        expression = "java(apiEntity.getApiDefinitionHttpV4() != null ? apiEntity.getApiDefinitionHttpV4().getFailover() : null)"
    )
    @Mapping(target = "endpointGroups", source = "apiEntity.apiDefinitionHttpV4.endpointGroups")
    @Mapping(target = "primaryOwner", source = "primaryOwner")
    @Mapping(target = "workflowState", source = "workflowState")
    @Mapping(target = "groups", source = "groups")
    @Mapping(target = "metadata", source = "metadata")
    ApiDescriptor.ApiDescriptorV4 mapV4(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata,
        List<Flow> flows
    );

    @Mapping(target = "id", source = "apiEntity.id")
    @Mapping(target = "apiVersion", source = "apiEntity.version")
    @Mapping(target = "state", source = "apiEntity.lifecycleState")
    @Mapping(target = "lifecycleState", source = "apiEntity.apiLifecycleState")
    @Mapping(target = "listeners", source = "apiEntity.apiDefinitionNativeV4.listeners")
    @Mapping(target = "flows", source = "apiEntity.apiDefinitionNativeV4.flows")
    @Mapping(target = "properties", source = "apiEntity.apiDefinitionNativeV4.properties")
    @Mapping(target = "resources", source = "apiEntity.apiDefinitionNativeV4.resources")
    @Mapping(target = "endpointGroups", source = "apiEntity.apiDefinitionNativeV4.endpointGroups")
    @Mapping(target = "primaryOwner", source = "primaryOwner")
    @Mapping(target = "workflowState", source = "workflowState")
    @Mapping(target = "groups", source = "groups")
    @Mapping(target = "metadata", source = "metadata")
    ApiDescriptor.Native mapNative(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata,
        List<NativeFlow> flows
    );

    @Mapping(target = "id", source = "apiEntity.id")
    @Mapping(target = "name", source = "apiEntity.name")
    @Mapping(target = "description", source = "apiEntity.description")
    @Mapping(target = "createdAt", source = "apiEntity.createdAt")
    @Mapping(target = "updatedAt", source = "apiEntity.updatedAt")
    @Mapping(target = "type", source = "apiEntity.type")
    @Mapping(target = "state", source = "apiEntity.lifecycleState")
    @Mapping(target = "lifecycleState", source = "apiEntity.apiLifecycleState")
    @Mapping(target = "providerId", source = "apiEntity.federatedApiDefinition.providerId")
    @Mapping(target = "originContext.integrationId", source = "integration.id")
    @Mapping(target = "originContext.integrationName", source = "integration.name")
    @Mapping(target = "originContext.provider", source = "integration.provider")
    @Mapping(target = "primaryOwner", source = "primaryOwner")
    @Mapping(target = "workflowState", source = "workflowState")
    @Mapping(target = "groups", source = "groups")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "originContext", source = "integration")
    ApiDescriptor.Federated mapFederated(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata,
        Integration.ApiIntegration integration
    );

    @Mapping(target = "id", source = "apiEntity.id")
    @Mapping(target = "apiVersion", source = "apiEntity.version")
    @Mapping(target = "state", source = "apiEntity.lifecycleState")
    @Mapping(target = "lifecycleState", source = "apiEntity.apiLifecycleState")
    @Mapping(target = "proxy", source = "apiEntity.apiDefinition.proxy")
    @Mapping(target = "services", source = "apiEntity.apiDefinition.services")
    @Mapping(target = "resources", source = "apiEntity.apiDefinition.resources")
    @Mapping(target = "paths", source = "apiEntity.apiDefinition.paths")
    @Mapping(target = "flows", source = "apiEntity.apiDefinition.flows")
    @Mapping(target = "properties", source = "apiEntity.apiDefinition.properties")
    @Mapping(target = "tags", source = "apiEntity.apiDefinition.tags")
    @Mapping(target = "pathMappings", source = "apiEntity.apiDefinition.pathMappings")
    @Mapping(target = "responseTemplates", source = "apiEntity.apiDefinition.responseTemplates")
    @Mapping(target = "plans", source = "apiEntity.apiDefinition.plans")
    @Mapping(target = "executionMode", source = "apiEntity.apiDefinition.executionMode")
    @Mapping(target = "primaryOwner", source = "primaryOwner")
    @Mapping(target = "workflowState", source = "workflowState")
    @Mapping(target = "groups", source = "groups")
    @Mapping(target = "metadata", source = "metadata")
    ApiDescriptor.ApiDescriptorV2 mapV2(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata,
        Collection<io.gravitee.definition.model.flow.Flow> flows
    );

    NewApiMetadata mapMetadata(Metadata source);

    default Map<String, Object> map(Collection<NewApiMetadata> sources) {
        return stream(sources).collect(Collectors.toMap(NewApiMetadata::getName, NewApiMetadata::getValue));
    }

    default Instant map(ZonedDateTime src) {
        return src == null ? null : src.toInstant();
    }

    default Map<String, io.gravitee.definition.model.Plan> map(List<io.gravitee.definition.model.Plan> value) {
        return stream(value).collect(Collectors.toMap(io.gravitee.definition.model.Plan::getId, Function.identity()));
    }

    /**
     * Map a PlanSecurity.
     * <p>
     *     This is required to ensure the PlanSecurityType as the same form as the one in the export.
     * </p>
     * @param source the source PlanSecurity
     * @return the mapped PlanSecurity
     */
    default PlanSecurity mapPlanSecurity(io.gravitee.definition.model.v4.plan.PlanSecurity source) {
        if (source == null) {
            return null;
        }
        return PlanSecurity
            .builder()
            .type(PlanSecurityType.valueOfLabel(source.getType()).name())
            .configuration(source.getConfiguration())
            .build();
    }

    default PlanSecurity mapPlanSecurityV2(String security, String definition) {
        if (security == null) {
            return null;
        }
        return PlanSecurity.builder().type(security).configuration(definition).build();
    }
}
