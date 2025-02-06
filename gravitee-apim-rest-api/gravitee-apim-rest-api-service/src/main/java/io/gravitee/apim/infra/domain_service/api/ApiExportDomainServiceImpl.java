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
package io.gravitee.apim.infra.domain_service.api;

import static io.gravitee.apim.core.api.domain_service.ApiExportDomainService.Excludable.GROUPS;
import static io.gravitee.apim.core.api.domain_service.ApiExportDomainService.Excludable.MEMBERS;
import static io.gravitee.apim.core.api.domain_service.ApiExportDomainService.Excludable.METADATA;
import static io.gravitee.apim.core.api.domain_service.ApiExportDomainService.Excludable.PAGES_MEDIA;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_DOCUMENTATION;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_MEMBER;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_PLAN;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.infra.adapter.GraviteeDefinitionAdapter;
import io.gravitee.apim.infra.adapter.MemberAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiExportDomainServiceImpl implements ApiExportDomainService {

    private final PermissionService permissionService;
    private final MediaService mediaService;

    private final WorkflowCrudService workflowCrudService;
    private final MembershipCrudService membershipCrudService;
    private final MetadataCrudService metadataCrudService;
    private final PageQueryService pageQueryService;
    private final ApiCrudService apiCrudService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final PlanCrudService planCrudService;

    @Override
    public GraviteeDefinition export(String apiId, AuditInfo auditInfo, Collection<Excludable> excluded) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        var apiPrimaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(auditInfo.organizationId(), apiId);
        var api1 = apiCrudService.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

        var members = !excluded.contains(MEMBERS) ? exportApiMembers(apiId) : null;
        var metadata = !excluded.contains(METADATA) ? exportApiMetadata(executionContext, apiId) : null;

        var pages = !excluded.contains(PAGES_MEDIA) ? exportApiPages(apiId) : null;
        var medias = !excluded.contains(PAGES_MEDIA) ? exportApiMedia(apiId) : null;

        var workflowState = stream(workflowCrudService.findByApiId(apiId))
            .map(Workflow::getState)
            .map(Enum::name)
            .map(WorkflowState::valueOf)
            .findFirst()
            .orElse(null);

        var plans = !excluded.contains(Excludable.PLANS) ? exportApiPlansV4(apiId) : null;
        if (api1.getDefinitionVersion() == DefinitionVersion.V4 && api1.getApiDefinitionHttpV4() != null) {
            var api = exportV4(api1, apiPrimaryOwner, metadata, workflowState, excluded);
            return new GraviteeDefinition.V4(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
        } else if (api1.getDefinitionVersion() == DefinitionVersion.V4 && api1.getApiDefinitionNativeV4() != null) {
            var api = exportNative(api1, apiPrimaryOwner, metadata, workflowState, excluded);
            return new GraviteeDefinition.Native(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
        } else if (api1.getDefinitionVersion() == DefinitionVersion.FEDERATED && api1.getFederatedApiDefinition() != null) {
            var api = exportFederated(api1, apiPrimaryOwner, metadata, workflowState, excluded);
            return new GraviteeDefinition.Federated(api, members, metadata, pages, plans, medias, api1.getPicture(), api1.getBackground());
        } else {
            throw new ApiDefinitionVersionNotSupportedException(
                api1.getDefinitionVersion() != null ? api1.getDefinitionVersion().getLabel() : null
            );
        }
    }

    private ApiDescriptor.ApiDescriptorV4 exportV4(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        @Nullable Collection<NewApiMetadata> metadata,
        @Nullable WorkflowState workflowState,
        Collection<Excludable> excluded
    ) {
        var groups = excluded.contains(GROUPS) ? null : apiEntity.getGroups();
        return GraviteeDefinitionAdapter.INSTANCE.mapV4(apiEntity, primaryOwner, workflowState, groups, metadata);
    }

    private ApiDescriptor.ApiDescriptorNative exportNative(
        Api nativeApi,
        PrimaryOwnerEntity primaryOwner,
        @Nullable Collection<NewApiMetadata> metadata,
        @Nullable WorkflowState workflowState,
        Collection<Excludable> excluded
    ) {
        var groups = excluded.contains(GROUPS) ? null : nativeApi.getGroups();
        return GraviteeDefinitionAdapter.INSTANCE.mapNative(nativeApi, primaryOwner, workflowState, groups, metadata);
    }

    private ApiDescriptor.ApiDescriptorFederated exportFederated(
        Api apiEntity,
        PrimaryOwnerEntity primaryOwner,
        @Nullable Set<NewApiMetadata> metadata,
        @Nullable WorkflowState workflowState,
        Collection<Excludable> excluded
    ) {
        var groups = excluded.contains(GROUPS) ? null : apiEntity.getGroups();
        return GraviteeDefinitionAdapter.INSTANCE.mapFederated(apiEntity, primaryOwner, workflowState, groups, metadata);
    }

    private Set<ApiMember> exportApiMembers(String apiId) {
        return permissionService.hasPermission(GraviteeContext.getExecutionContext(), API_MEMBER, apiId, READ)
            ? stream(membershipCrudService.findByApiId(apiId))
                .filter(memberEntity -> memberEntity.getMemberType() == Membership.Type.USER)
                .map(MemberAdapter.INSTANCE::toApiMember)
                .collect(Collectors.toSet())
            : null;
    }

    private Set<NewApiMetadata> exportApiMetadata(ExecutionContext executionContext, String apiId) {
        return permissionService.hasPermission(executionContext, RolePermission.API_METADATA, apiId, READ)
            ? GraviteeDefinitionAdapter.INSTANCE.mapMetadata(metadataCrudService.findByApiId(apiId))
            : null;
    }

    private List<PageExport> exportApiPages(String apiId) {
        return permissionService.hasPermission(GraviteeContext.getExecutionContext(), API_DOCUMENTATION, apiId, READ)
            ? GraviteeDefinitionAdapter.INSTANCE.mapPage(pageQueryService.searchByApiId(apiId))
            : null;
    }

    private List<Media> exportApiMedia(String apiId) {
        return permissionService.hasPermission(GraviteeContext.getExecutionContext(), API_DOCUMENTATION, apiId, READ)
            ? GraviteeDefinitionAdapter.INSTANCE.mapMedia(mediaService.findAllByApiId(apiId))
            : null;
    }

    private Collection<PlanDescriptor.PlanDescriptorV4> exportApiPlansV4(String apiId) {
        return permissionService.hasPermission(GraviteeContext.getExecutionContext(), API_PLAN, apiId, READ)
            ? GraviteeDefinitionAdapter.INSTANCE.mapPlanV4(planCrudService.findByApiId(apiId))
            : null;
    }
}
