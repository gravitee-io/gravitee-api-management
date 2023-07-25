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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SystemFolderType;
import io.gravitee.rest.api.model.UpdateApiMetadataEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.v4.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiImportExportServiceImpl implements ApiImportExportService {

    private final ApiMetadataService apiMetadataService;
    private final ApiService apiServiceV4;
    private final ApiSearchService apiSearchService;
    private final MediaService mediaService;
    private final MembershipService membershipService;
    private final PageService pageService;
    private final PermissionService permissionService;
    private final PlanService planService;
    private final RoleService roleService;

    private final ApiIdsCalculatorService apiIdsCalculatorService;

    public ApiImportExportServiceImpl(
        final ApiMetadataService apiMetadataService,
        final ApiService apiServiceV4,
        final ApiSearchService apiSearchService,
        final MediaService mediaService,
        final MembershipService membershipService,
        final PageService pageService,
        final PermissionService permissionService,
        final PlanService planService,
        final RoleService roleService,
        final ApiIdsCalculatorService apiIdsCalculatorService
    ) {
        this.apiMetadataService = apiMetadataService;
        this.apiServiceV4 = apiServiceV4;
        this.apiSearchService = apiSearchService;
        this.mediaService = mediaService;
        this.membershipService = membershipService;
        this.pageService = pageService;
        this.permissionService = permissionService;
        this.planService = planService;
        this.roleService = roleService;
        this.apiIdsCalculatorService = apiIdsCalculatorService;
    }

    @Override
    public ExportApiEntity exportApi(ExecutionContext executionContext, String apiId, String userId) {
        final var apiEntity = apiSearchService.findGenericById(executionContext, apiId);
        if (apiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(apiEntity.getDefinitionVersion().getLabel());
        }

        final ExportApiEntity exportApi = new ExportApiEntity();
        exportApi.setApiEntity((ApiEntity) apiEntity);

        if (
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_MEMBER,
                apiId,
                RolePermissionAction.READ
            )
        ) {
            var members = membershipService
                .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, apiId)
                .stream()
                .filter(memberEntity -> memberEntity.getType() == MembershipMemberType.USER)
                .collect(Collectors.toSet());
            exportApi.setMembers(members);
        }

        if (
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_METADATA,
                apiId,
                RolePermissionAction.READ
            )
        ) {
            var metadataList = apiMetadataService.findAllByApi(apiId);
            exportApi.setMetadata(new HashSet<>(metadataList));
        }

        if (
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_PLAN,
                apiId,
                RolePermissionAction.READ
            )
        ) {
            var plansSet = planService.findByApi(GraviteeContext.getExecutionContext(), apiId);
            exportApi.setPlans(plansSet);
        }

        if (
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DOCUMENTATION,
                apiId,
                RolePermissionAction.READ
            )
        ) {
            var pageList = pageService.findByApi(GraviteeContext.getCurrentEnvironment(), apiId);
            exportApi.setPages(pageList);

            var apiMediaList = mediaService.findAllByApiId(apiId);
            exportApi.setApiMedia(apiMediaList);
        }

        return exportApi;
    }

    @Override
    public GenericApiEntity createFromExportedApi(final ExecutionContext executionContext, ExportApiEntity exportApiEntity, String userId) {
        GenericApiEntity apiEntity = exportApiEntity.getApiEntity();
        if (apiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(apiEntity.getDefinitionVersion().getLabel());
        }

        final ExportApiEntity exportApiWithIdsRecalculated = apiIdsCalculatorService.recalculateApiDefinitionIds(executionContext, exportApiEntity);

        ApiEntity createdApiEntity = apiServiceV4.createWithImport(executionContext, exportApiWithIdsRecalculated.getApiEntity(), userId);

        createMembers(executionContext, createdApiEntity, exportApiWithIdsRecalculated.getMembers());
        createPages(executionContext, createdApiEntity, exportApiWithIdsRecalculated.getPages());
        createPlans(executionContext, createdApiEntity, exportApiWithIdsRecalculated.getPlans());
        createMetadata(executionContext, createdApiEntity, exportApiWithIdsRecalculated.getMetadata());
        createPageAndMedia(executionContext, createdApiEntity, exportApiWithIdsRecalculated.getApiMedia());

        return createdApiEntity;
    }

    protected void createMembers(final ExecutionContext executionContext, ApiEntity createdApiEntity, Set<MemberEntity> members) {
        if (members.isEmpty()) {
            return;
        }

        // get the current PO
        RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), RoleScope.API);
        assert (poRole != null);
        String poRoleName = poRole.getName();

        for (MemberEntity member : members) {
            List<RoleEntity> rolesToImport = member
                .getRoles()
                .stream()
                .filter(role -> !role.getName().equals(poRoleName))
                .map(role -> {
                    Optional<RoleEntity> roleEntity = roleService.findByScopeAndName(
                        RoleScope.API,
                        role.getName(),
                        executionContext.getOrganizationId()
                    );
                    if (roleEntity.isPresent()) {
                        return roleEntity.get();
                    }

                    log.warn("Unable to find role '{}' to import", role.getName());
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            rolesToImport.forEach(role -> {
                try {
                    membershipService.addRoleToMemberOnReference(
                        executionContext,
                        MembershipReferenceType.API,
                        createdApiEntity.getId(),
                        MembershipMemberType.USER,
                        member.getId(),
                        role.getId()
                    );
                } catch (Exception e) {
                    log.warn(
                        "Unable to add role '{}[{}]' to member '{}[{}]' on API '{}[{}]' due to : {}",
                        role.getName(),
                        role.getId(),
                        member.getDisplayName(),
                        member.getId(),
                        createdApiEntity.getName(),
                        createdApiEntity.getId(),
                        e.getMessage()
                    );
                }
            });
        }
    }

    protected void createPages(final ExecutionContext executionContext, ApiEntity createdApiEntity, List<PageEntity> pages) {
        if (pages.isEmpty()) {
            return;
        }

        pageService.createOrUpdatePages(executionContext, pages, createdApiEntity.getId());
    }

    protected void createPlans(final ExecutionContext executionContext, ApiEntity createdApiEntity, Set<PlanEntity> plans) {
        if (plans.isEmpty()) {
            return;
        }

        plans.forEach(planEntity -> {
            planEntity.setApiId(createdApiEntity.getId());
            planService.createOrUpdatePlan(executionContext, planEntity);
        });
    }

    protected void createMetadata(final ExecutionContext executionContext, ApiEntity createdApiEntity, Set<ApiMetadataEntity> metadata) {
        metadata
            .stream()
            .map(apiMetadataEntity -> {
                UpdateApiMetadataEntity updateApiMetadataEntity = new UpdateApiMetadataEntity();
                updateApiMetadataEntity.setApiId(createdApiEntity.getId());
                updateApiMetadataEntity.setDefaultValue(apiMetadataEntity.getDefaultValue());
                updateApiMetadataEntity.setFormat(apiMetadataEntity.getFormat());
                updateApiMetadataEntity.setKey(apiMetadataEntity.getKey());
                updateApiMetadataEntity.setName(apiMetadataEntity.getName());
                updateApiMetadataEntity.setValue(apiMetadataEntity.getValue());
                return updateApiMetadataEntity;
            })
            .forEach(metadataEntity -> {
                apiMetadataService.update(executionContext, metadataEntity);
            });
    }

    protected void createPageAndMedia(
        final ExecutionContext executionContext,
        ApiEntity createdApiEntity,
        List<MediaEntity> mediaEntities
    ) {
        mediaEntities.forEach(mediaEntity -> {
            mediaService.saveApiMedia(createdApiEntity.getId(), mediaEntity);
        });

        List<PageEntity> search = pageService.search(
            executionContext.getEnvironmentId(),
            new PageQuery.Builder()
                .api(createdApiEntity.getId())
                .name(SystemFolderType.ASIDE.folderName())
                .type(PageType.SYSTEM_FOLDER)
                .build()
        );

        if (search.isEmpty()) {
            pageService.createAsideFolder(executionContext, createdApiEntity.getId());
        }
    }
}
