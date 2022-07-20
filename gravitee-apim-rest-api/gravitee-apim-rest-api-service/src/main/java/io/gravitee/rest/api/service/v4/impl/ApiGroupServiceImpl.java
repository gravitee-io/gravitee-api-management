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
package io.gravitee.rest.api.service.v4.impl;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupMemberEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiGroupService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ApiGroupServiceImpl implements ApiGroupService {

    private final ApiRepository apiRepository;
    private final ApiNotificationService apiNotificationService;
    private final GroupService groupService;
    private final MembershipService membershipService;
    private final PrimaryOwnerService primaryOwnerService;
    private final RoleService roleService;

    public ApiGroupServiceImpl(
        final ApiRepository apiRepository,
        final ApiNotificationService apiNotificationService,
        final GroupService groupService,
        final MembershipService membershipService,
        final PrimaryOwnerService primaryOwnerService,
        final RoleService roleService
    ) {
        this.apiRepository = apiRepository;
        this.apiNotificationService = apiNotificationService;
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.primaryOwnerService = primaryOwnerService;
        this.roleService = roleService;
    }

    @Override
    public void addGroup(final ExecutionContext executionContext, final String apiId, final String group) {
        try {
            log.debug("Add group {} to API {}", group, apiId);

            Optional<Api> optApi = apiRepository.findById(apiId);

            if (executionContext.hasEnvironmentId()) {
                optApi = optApi.filter(result -> result.getEnvironmentId().equals(executionContext.getEnvironmentId()));
            }

            Api api = optApi.orElseThrow(() -> new ApiNotFoundException(apiId));

            Set<String> groups = api.getGroups();
            if (groups == null) {
                groups = new HashSet<>();
                api.setGroups(groups);
            }
            groups.add(group);

            apiRepository.update(api);
            apiNotificationService.triggerUpdateNotification(executionContext, api);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to add group {} to API {}: {}", group, apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to add group " + group + " to API " + apiId, ex);
        }
    }

    @Override
    public void removeGroup(ExecutionContext executionContext, String apiId, String group) {
        try {
            log.debug("Remove group {} to API {}", group, apiId);

            Optional<Api> optApi = apiRepository.findById(apiId);

            if (executionContext.hasEnvironmentId()) {
                optApi = optApi.filter(result -> result.getEnvironmentId().equals(executionContext.getEnvironmentId()));
            }

            Api api = optApi.orElseThrow(() -> new ApiNotFoundException(apiId));
            if (api.getGroups() != null && api.getGroups().remove(group)) {
                apiRepository.update(api);
                apiNotificationService.triggerUpdateNotification(executionContext, api);
            }
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to remove group {} from API {}: {}", group, apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove group " + group + " from API " + apiId, ex);
        }
    }

    @Override
    public Map<String, List<GroupMemberEntity>> getGroupsWithMembers(final ExecutionContext executionContext, final String apiId)
        throws TechnicalManagementException {
        try {
            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            final List<String> apiGroups = api.getGroups() == null ? new ArrayList<>() : new ArrayList<>(api.getGroups());
            Set<MemberEntity> members = membershipService.getMembersByReferencesAndRole(
                executionContext,
                MembershipReferenceType.GROUP,
                apiGroups,
                null
            );

            return members
                .stream()
                .peek(member -> member.setRoles(computeMemberRoles(executionContext, api, member)))
                .collect(groupingBy(MemberEntity::getReferenceId, mapping(GroupMemberEntity::new, toList())));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error has occurred while trying to retrieve groups for API " + apiId);
        }
    }

    private List<RoleEntity> computeMemberRoles(ExecutionContext executionContext, Api api, MemberEntity member) {
        return member
            .getRoles()
            .stream()
            .map(role -> role.isApiPrimaryOwner() ? mapApiPrimaryOwnerRole(executionContext, api, member.getReferenceId()) : role)
            .collect(toList());
    }

    private RoleEntity mapApiPrimaryOwnerRole(ExecutionContext executionContext, Api api, String groupId) {
        GroupEntity memberGroup = groupService.findById(executionContext, groupId);
        String groupDefaultApiRoleName = memberGroup.getRoles() == null ? null : memberGroup.getRoles().get(RoleScope.API);

        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, api.getId());

        /*
         * If the group is not primary owner and the API, return the default group role for the API scope
         *
         * See https://github.com/gravitee-io/issues/issues/6360#issuecomment-1030610543
         */
        RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.API);

        if (memberGroup.getId().equals(primaryOwner.getId())) {
            role.setName(SystemRole.PRIMARY_OWNER.name());
        } else if (groupDefaultApiRoleName != null) {
            roleService
                .findByScopeAndName(RoleScope.API, groupDefaultApiRoleName, executionContext.getOrganizationId())
                .ifPresent(groupRole -> role.setName(groupRole.getName()));
        }

        return role;
    }
}
