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
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    List<Media> mapMedia(Collection<MediaEntity> src);

    @Mapping(target = "security", expression = "java(mapPlanSecurity(source.getPlanDefinitionHttpV4().getSecurity()))")
    @Mapping(target = "mode", source = "planDefinitionHttpV4.mode")
    @Mapping(target = "status", source = "planDefinitionHttpV4.status")
    PlanDescriptor.PlanDescriptorV4 mapPlanV4(Plan source);

    @Mapping(target = "security", expression = "java(mapPlanSecurity(source.getPlanDefinitionNativeV4().getSecurity()))")
    @Mapping(target = "mode", source = "planDefinitionNativeV4.mode")
    @Mapping(target = "status", source = "planDefinitionNativeV4.status")
    PlanDescriptor.PlanDescriptorV4 mapPlanNative(Plan source);

    @Nullable
    default Collection<PlanDescriptor.PlanDescriptorV4> mapPlanV4(Collection<Plan> src) {
        return src == null
            ? null
            : src.stream().map(plan -> plan.getPlanDefinitionV4() != null ? mapPlanV4(plan) : mapPlanNative(plan)).toList();
    }

    io.gravitee.rest.api.model.PrimaryOwnerEntity map(PrimaryOwnerEntity src);

    @Mapping(target = "id", source = "apiEntity.id")
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
    ApiDescriptor.ApiDescriptorV4 mapV4(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata
    );

    @Mapping(target = "id", source = "apiEntity.id")
    @Mapping(target = "state", source = "apiEntity.lifecycleState")
    @Mapping(target = "lifecycleState", source = "apiEntity.apiLifecycleState")
    @Mapping(target = "listeners", source = "apiEntity.apiDefinitionNativeV4.listeners")
    @Mapping(target = "flows", source = "apiEntity.apiDefinitionNativeV4.flows")
    @Mapping(target = "properties", source = "apiEntity.apiDefinitionNativeV4.properties")
    @Mapping(target = "resources", source = "apiEntity.apiDefinitionNativeV4.resources")
    @Mapping(target = "endpointGroups", source = "apiEntity.apiDefinitionNativeV4.endpointGroups")
    ApiDescriptor.ApiDescriptorNative mapNative(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata
    );

    @Mapping(target = "id", source = "apiEntity.id")
    @Mapping(target = "type", source = "apiEntity.type")
    @Mapping(target = "providerId", source = "apiEntity.federatedApiDefinition.providerId")
    @Mapping(target = "lifecycleState", source = "apiEntity.apiLifecycleState")
    ApiDescriptor.ApiDescriptorFederated mapFederated(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        WorkflowState workflowState,
        Set<String> groups,
        Collection<NewApiMetadata> metadata
    );

    Set<NewApiMetadata> mapMetadata(Collection<Metadata> source);

    default Map<String, Object> map(Collection<NewApiMetadata> sources) {
        return stream(sources).collect(Collectors.toMap(NewApiMetadata::getName, NewApiMetadata::getValue));
    }

    default Instant map(ZonedDateTime src) {
        return src == null ? null : src.toInstant();
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
}
