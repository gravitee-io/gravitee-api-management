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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;

import io.gravitee.rest.api.model.AccessControlEntity;
import io.gravitee.rest.api.model.AccessControlReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Iterator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AccessControlServiceImpl extends AbstractService implements AccessControlService {

    private final Logger logger = LoggerFactory.getLogger(AccessControlServiceImpl.class);

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ApiSearchService apiSearchService;

    @Autowired
    private ApiAuthorizationService apiAuthorizationService;

    @Autowired
    private RoleService roleService;

    @Override
    public boolean canAccessApiFromPortal(ExecutionContext executionContext, GenericApiEntity genericApiEntity) {
        if (!ApiLifecycleState.PUBLISHED.equals(genericApiEntity.getLifecycleState())) {
            return false;
        } else if (PUBLIC.equals(genericApiEntity.getVisibility())) {
            return true;
        } else if (isAuthenticated()) {
            return apiAuthorizationService.canConsumeApi(executionContext, getAuthenticatedUsername(), genericApiEntity);
        }
        return false;
    }

    @Override
    public boolean canAccessApiFromPortal(ExecutionContext executionContext, String apiId) {
        GenericApiEntity genericApi = apiSearchService.findGenericById(executionContext, apiId);
        return canAccessApiFromPortal(executionContext, genericApi);
    }

    private boolean canAccessPage(final ExecutionContext executionContext, GenericApiEntity genericApiEntity, PageEntity pageEntity) {
        if (!pageEntity.isPublished()) {
            return false;
        }

        // in public mode
        if (PUBLIC.equals(pageEntity.getVisibility())) {
            return true;
        }

        // private mode
        if (!isAuthenticated()) {
            return false;
        }

        Set<AccessControlEntity> accessControls = pageEntity.getAccessControls();
        if (accessControls == null || accessControls.isEmpty()) {
            return true;
        } else {
            Set<GroupEntity> userGroups = groupService.findByUser(getAuthenticatedUsername());
            Set<RoleEntity> contextualUserRoles = getContextualUserRoles(
                executionContext,
                genericApiEntity,
                executionContext.getEnvironmentId()
            );

            return accessControls
                .stream()
                .anyMatch(acl -> {
                    if (AccessControlReferenceType.ROLE.name().equals(acl.getReferenceType())) {
                        boolean roleMatched = contextualUserRoles.stream().anyMatch(role -> role.getId().equals(acl.getReferenceId()));
                        return pageEntity.isExcludedAccessControls() != roleMatched;
                    } else if (AccessControlReferenceType.GROUP.name().equals(acl.getReferenceType())) {
                        boolean groupMatched = userGroups.stream().anyMatch(group -> group.getId().equals(acl.getReferenceId()));
                        return pageEntity.isExcludedAccessControls() != groupMatched;
                    } else {
                        logger.warn("ACL reference type [{}] not found", acl.getReferenceType());
                    }
                    return false;
                });
        }
    }

    @Override
    public boolean canAccessPageFromPortal(final ExecutionContext executionContext, PageEntity pageEntity) {
        return canAccessPageFromPortal(executionContext, null, pageEntity);
    }

    @Override
    public boolean canAccessPageFromPortal(final ExecutionContext executionContext, String apiId, PageEntity pageEntity) {
        if (PageType.SYSTEM_FOLDER.name().equals(pageEntity.getType()) || PageType.MARKDOWN_TEMPLATE.name().equals(pageEntity.getType())) {
            return false;
        }
        if (apiId != null) {
            final GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId);
            return canAccessPage(executionContext, genericApiEntity, pageEntity);
        }
        return canAccessPage(executionContext, null, pageEntity);
    }

    @Override
    public boolean canAccessApiPageFromPortal(ExecutionContext executionContext, GenericApiEntity apiEntity, PageEntity pageEntity) {
        return canAccessPage(executionContext, apiEntity, pageEntity);
    }

    @Override
    public boolean canAccessPageFromConsole(
        final ExecutionContext executionContext,
        GenericApiEntity genericApiEntity,
        PageEntity pageEntity
    ) {
        if (canAccessPage(executionContext, genericApiEntity, pageEntity)) {
            return true;
        } else {
            return canEditApiPage(executionContext, genericApiEntity);
        }
    }

    private boolean canEditApiPage(final ExecutionContext executionContext, GenericApiEntity genericApiEntity) {
        if (genericApiEntity == null) {
            return false;
        }
        boolean canEditApiPage = false;
        MemberEntity member = membershipService.getUserMember(
            executionContext,
            MembershipReferenceType.API,
            genericApiEntity.getId(),
            getAuthenticatedUsername()
        );
        if (member == null && genericApiEntity.getGroups() != null) {
            Iterator<String> groupIdIterator = genericApiEntity.getGroups().iterator();
            while (!canEditApiPage && groupIdIterator.hasNext()) {
                String groupId = groupIdIterator.next();
                member = membershipService.getUserMember(
                    executionContext,
                    MembershipReferenceType.GROUP,
                    groupId,
                    getAuthenticatedUsername()
                );
                canEditApiPage = canEditApiPage(member);
            }
        } else {
            canEditApiPage = canEditApiPage(member);
        }
        return canEditApiPage;
    }

    private boolean canEditApiPage(MemberEntity member) {
        // if not member => not displayable
        if (member == null) {
            return false;
        }

        // only members which could modify a page can see an unpublished page
        return roleService.hasPermission(
            member.getPermissions(),
            ApiPermission.DOCUMENTATION,
            new RolePermissionAction[] { RolePermissionAction.UPDATE, RolePermissionAction.CREATE, RolePermissionAction.DELETE }
        );
    }

    private Set<RoleEntity> getContextualUserRoles(
        final ExecutionContext executionContext,
        GenericApiEntity genericApiEntity,
        final String environmentId
    ) {
        if (genericApiEntity != null) {
            Set<RoleEntity> roles = this.membershipService.getRoles(
                MembershipReferenceType.API,
                genericApiEntity.getId(),
                MembershipMemberType.USER,
                getAuthenticatedUsername()
            );

            if (genericApiEntity.getGroups() != null && !genericApiEntity.getGroups().isEmpty()) {
                genericApiEntity
                    .getGroups()
                    .forEach(groupId -> {
                        MemberEntity member = membershipService.getUserMember(
                            executionContext,
                            MembershipReferenceType.GROUP,
                            groupId,
                            getAuthenticatedUsername()
                        );
                        if (member != null) {
                            roles.addAll(member.getRoles());
                        }
                    });
            }
            return roles;
        } else {
            return this.membershipService.getRoles(
                MembershipReferenceType.ENVIRONMENT,
                environmentId,
                MembershipMemberType.USER,
                getAuthenticatedUsername()
            );
        }
    }
}
