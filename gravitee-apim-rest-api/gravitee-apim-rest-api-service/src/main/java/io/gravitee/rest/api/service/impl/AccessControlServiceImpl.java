/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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
    private ApiRepository apiRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RoleService roleService;

    @Override
    public boolean canAccessApiFromPortal(ExecutionContext executionContext, ApiEntity apiEntity) {
        if (PUBLIC.equals(apiEntity.getVisibility())) {
            return true;
        } else if (isAuthenticated()) {
            return getAccessibleApiIdsFromPortal(executionContext, getAuthenticatedUser().getUsername())
                .contains(apiEntity.getId());
        }
        return false;
    }

    private Set<String> getAccessibleApiIdsFromPortal(ExecutionContext executionContext, String userId) {
        try {
            Stream<String> userApiIds = Stream.concat(streamUserApiIds(userId), streamUserGroupApiIds(executionContext, userId));
            return Stream.concat(userApiIds, streamUserSubscriptionApiIds(executionContext, userId)).collect(toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private Stream<String> streamUserApiIds(String userId) {
        return membershipService
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.API)
            .stream()
            .map(MembershipEntity::getReferenceId);
    }

    private Stream<String> streamUserGroupApiIds(ExecutionContext executionContext, String userId) {
        List<String> groupIds = membershipService
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP)
            .stream()
            .map(MembershipEntity::getReferenceId)
            .collect(toList());

        ApiCriteria apiQuery = new ApiCriteria.Builder().groups(groupIds).environmentId(executionContext.getEnvironmentId()).build();
        return apiRepository.search(apiQuery, ApiFieldFilter.defaultFields()).stream().map(Api::getId);
    }

    private Stream<String> streamUserSubscriptionApiIds(ExecutionContext executionContext, String userId) throws TechnicalException {
        Set<String> userApplicationIds = membershipService.getReferenceIdsByMemberAndReference(
            MembershipMemberType.USER,
            userId,
            MembershipReferenceType.APPLICATION
        );

        Set<String> applicationIds = new HashSet<>(userApplicationIds);

        Set<String> userGroupIds = membershipService.getReferenceIdsByMemberAndReference(
            MembershipMemberType.USER,
            userId,
            MembershipReferenceType.GROUP
        );

        ApplicationCriteria appQuery = new ApplicationCriteria.Builder()
            .groups(userGroupIds)
            .environmentIds(executionContext.getEnvironmentId())
            .status(ApplicationStatus.ACTIVE)
            .build();

        Set<String> userGroupApplicationIds = applicationRepository.searchIds(appQuery);

        applicationIds.addAll(userGroupApplicationIds);

        if (!applicationIds.isEmpty()) {
            final SubscriptionCriteria query = new SubscriptionCriteria.Builder().applications(applicationIds).build();
            List<Subscription> subscriptions = subscriptionRepository.search(query);
            List<String> subscriptionIds = subscriptions.stream().map(Subscription::getApi).collect(toList());
            return apiRepository
                .search(
                    new ApiCriteria.Builder().ids(subscriptionIds).build(),
                    ApiFieldFilter.defaultFields()
                )
                .stream()
                .map(Api::getId);
        }

        return Stream.of();
    }

    @Override
    public boolean canAccessApiFromPortal(ExecutionContext executionContext, String apiId) {
        try {
            Api api = apiRepository.findById(apiId)
                .orElseThrow(() ->  new ApiNotFoundException(apiId));

            ApiEntity apiEntity = new ApiEntity();
            apiEntity.setVisibility(Visibility.valueOf(api.getVisibility().name()));
            apiEntity.setId(api.getId());
            return canAccessApiFromPortal(executionContext, apiEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }

    }

    private boolean canAccessPage(final ExecutionContext executionContext, ApiEntity apiEntity, PageEntity pageEntity) {
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
            Set<RoleEntity> contextualUserRoles = getContextualUserRoles(executionContext, apiEntity, executionContext.getEnvironmentId());

            return accessControls
                .stream()
                .anyMatch(acl -> {
                    if (AccessControlReferenceType.ROLE.name().equals(acl.getReferenceType())) {
                        boolean roleMatched = contextualUserRoles.stream().anyMatch(role -> role.getId().equals(acl.getReferenceId()));
                        return pageEntity.isExcludedAccessControls() ? !roleMatched : roleMatched;
                    } else if (AccessControlReferenceType.GROUP.name().equals(acl.getReferenceType())) {
                        boolean groupMatched = userGroups.stream().anyMatch(group -> group.getId().equals(acl.getReferenceId()));
                        return pageEntity.isExcludedAccessControls() ? !groupMatched : groupMatched;
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
        try {
            if (PageType.SYSTEM_FOLDER.name().equals(pageEntity.getType()) || PageType.MARKDOWN_TEMPLATE.name().equals(pageEntity.getType())) {
                return false;
            }
            if (apiId != null) {
                final Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(api.getId());
                apiEntity.setGroups(api.getGroups());
                return canAccessPage(executionContext, apiEntity, pageEntity);
            }
            return canAccessPage(executionContext, null, pageEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    @Override
    public boolean canAccessPageFromConsole(final ExecutionContext executionContext, ApiEntity apiEntity, PageEntity pageEntity) {
        if (canAccessPage(executionContext, apiEntity, pageEntity)) {
            return true;
        } else {
            return canEditApiPage(executionContext, apiEntity);
        }
    }

    private boolean canEditApiPage(final ExecutionContext executionContext, ApiEntity api) {
        if (api == null) {
            return false;
        }
        boolean canEditApiPage = false;
        MemberEntity member = membershipService.getUserMember(
            executionContext,
            MembershipReferenceType.API,
            api.getId(),
            getAuthenticatedUsername()
        );
        if (member == null && api.getGroups() != null) {
            Iterator<String> groupIdIterator = api.getGroups().iterator();
            while (!canEditApiPage && groupIdIterator.hasNext()) {
                String groupId = groupIdIterator.next();
                member =
                    membershipService.getUserMember(executionContext, MembershipReferenceType.GROUP, groupId, getAuthenticatedUsername());
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

    private Set<RoleEntity> getContextualUserRoles(final ExecutionContext executionContext, ApiEntity api, final String environmentId) {
        if (api != null) {
            Set<RoleEntity> roles =
                this.membershipService.getRoles(
                        MembershipReferenceType.API,
                        api.getId(),
                        MembershipMemberType.USER,
                        getAuthenticatedUsername()
                    );

            if (api.getGroups() != null && !api.getGroups().isEmpty()) {
                api
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
