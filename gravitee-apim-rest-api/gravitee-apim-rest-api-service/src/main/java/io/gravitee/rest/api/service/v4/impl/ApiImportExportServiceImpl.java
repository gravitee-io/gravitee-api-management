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

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

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
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
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
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiImportExportServiceImpl implements ApiImportExportService {

    private final ApiMetadataService apiMetadataService;
    private final ApiSearchService apiSearchService;
    private final MediaService mediaService;
    private final MembershipService membershipService;
    private final PageService pageService;
    private final PermissionService permissionService;
    private final PlanService planService;
    private final RoleService roleService;

    public ApiImportExportServiceImpl(
        final ApiMetadataService apiMetadataService,
        final ApiSearchService apiSearchService,
        final MediaService mediaService,
        final MembershipService membershipService,
        final PageService pageService,
        final PermissionService permissionService,
        final PlanService planService,
        final RoleService roleService
    ) {
        this.apiMetadataService = apiMetadataService;
        this.apiSearchService = apiSearchService;
        this.mediaService = mediaService;
        this.membershipService = membershipService;
        this.pageService = pageService;
        this.permissionService = permissionService;
        this.planService = planService;
        this.roleService = roleService;
    }

    @Override
    public ExportApiEntity exportApi(ExecutionContext executionContext, String apiId, String userId, Set<String> excludeAdditionalData) {
        final var apiEntity = apiSearchService.findGenericById(executionContext, apiId);
        if (apiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(apiEntity.getDefinitionVersion().getLabel());
        }

        final ExportApiEntity exportApi = new ExportApiEntity();
        exportApi.setApiEntity((ApiEntity) apiEntity);

        if (!excludeAdditionalData.contains("members")) {
            var members = exportApiMembers(apiId);
            exportApi.setMembers(members);
        }

        if (!excludeAdditionalData.contains("metadata")) {
            var metadata = exportApiMetadata(executionContext, apiId);
            exportApi.setMetadata(metadata);
        }

        if (!excludeAdditionalData.contains("plans")) {
            var plans = exportApiPlans(apiId);
            exportApi.setPlans(plans);
        }

        if (!excludeAdditionalData.contains("pages")) {
            exportApiPagesAndMedia(apiId, exportApi);
        }

        if (excludeAdditionalData.contains("groups")) {
            exportApi.getApiEntity().setGroups(null);
        }

        return exportApi;
    }

    private void exportApiPagesAndMedia(String apiId, ExportApiEntity exportApi) {
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
    }

    private Set<PlanEntity> exportApiPlans(String apiId) {
        if (
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_PLAN,
                apiId,
                RolePermissionAction.READ
            )
        ) {
            return planService.findByApi(GraviteeContext.getExecutionContext(), apiId);
        }
        return null;
    }

    private Set<ApiMetadataEntity> exportApiMetadata(ExecutionContext executionContext, String apiId) {
        if (permissionService.hasPermission(executionContext, RolePermission.API_METADATA, apiId, RolePermissionAction.READ)) {
            return new HashSet<>(apiMetadataService.findAllByApi(executionContext, apiId));
        }
        return null;
    }

    private Set<MemberEntity> exportApiMembers(String apiId) {
        if (
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_MEMBER,
                apiId,
                RolePermissionAction.READ
            )
        ) {
            return membershipService
                .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, apiId)
                .stream()
                .filter(memberEntity -> memberEntity.getType() == MembershipMemberType.USER)
                .collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public void createMembers(final ExecutionContext executionContext, String apiId, Set<MemberEntity> members) {
        if (members.isEmpty()) {
            return;
        }

        // get the current PO
        RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), RoleScope.API);
        assert (poRole != null);
        String poRoleName = poRole.getName();

        var defaultApiRole = roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), RoleScope.API);
        assert (!isEmpty(defaultApiRole));

        for (MemberEntity member : members) {
            if (isEmpty(member.getRoles())) {
                log.warn("There is no role associated with this member. Default role will be applied");
                member.setRoles(defaultApiRole);
            }
            List<RoleEntity> rolesToImport = member
                .getRoles()
                .stream()
                .filter(role -> !role.getName().equals(poRoleName))
                .map(role -> {
                    try {
                        UUID id = UUID.fromString(role.getName());
                        RoleEntity roleEntity = roleService.findById(id.toString());
                        if (roleEntity != null) {
                            return roleEntity;
                        }
                    } catch (IllegalArgumentException exception) {
                        Optional<RoleEntity> roleEntity = roleService.findByScopeAndName(
                            RoleScope.API,
                            role.getName(),
                            executionContext.getOrganizationId()
                        );

                        if (roleEntity.isPresent()) {
                            return roleEntity.get();
                        }
                    }

                    log.warn("Unable to find role '{}' to import", role.getName());
                    return defaultApiRole.iterator().next();
                })
                .filter(Objects::nonNull)
                .toList();

            rolesToImport.forEach(role -> {
                try {
                    membershipService.deleteReferenceMember(
                        executionContext,
                        MembershipReferenceType.API,
                        apiId,
                        MembershipMemberType.USER,
                        member.getId()
                    );
                    membershipService.addRoleToMemberOnReference(
                        executionContext,
                        MembershipReferenceType.API,
                        apiId,
                        MembershipMemberType.USER,
                        member.getId(),
                        role.getId()
                    );
                } catch (Exception e) {
                    log.warn(
                        "Unable to add role '{}[{}]' to member '{}[{}]' on API '[{}]' due to : {}",
                        role.getName(),
                        role.getId(),
                        member.getDisplayName(),
                        member.getId(),
                        apiId,
                        e.getMessage()
                    );
                }
            });
        }
        log.debug("Members successfully created for imported api {}", apiId);
    }

    @Override
    public void createPageAndMedia(final ExecutionContext executionContext, String apiId, List<MediaEntity> mediaEntities) {
        mediaEntities.forEach(mediaEntity -> {
            try {
                mediaService.saveApiMedia(executionContext, apiId, mediaEntity);
            } catch (Exception e) {
                log.warn(
                    "Unable to create api media {} for imported API {}' due to : {}",
                    mediaEntity.getFileName(),
                    apiId,
                    e.getMessage()
                );
            }
        });

        List<PageEntity> search = pageService.search(
            executionContext.getEnvironmentId(),
            new PageQuery.Builder().api(apiId).name(SystemFolderType.ASIDE.folderName()).type(PageType.SYSTEM_FOLDER).build()
        );

        if (search.isEmpty()) {
            pageService.createAsideFolder(executionContext, apiId);
        }
        log.debug("Media successfully created for imported api {}", apiId);
    }
}
