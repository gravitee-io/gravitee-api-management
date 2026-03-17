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
package io.gravitee.apim.core.user.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.model.UserGroupEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserGroupsUseCase {

    private final MembershipQueryService membershipQueryService;
    private final GroupQueryService groupQueryService;
    private final RoleQueryService roleQueryService;
    private final EnvironmentCrudService environmentCrudService;

    public Output execute(Input input) {
        var memberships = membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(input.userId, Membership.Type.USER, Membership.ReferenceType.GROUP)
            .stream()
            .toList();

        if (memberships.isEmpty()) {
            return new Output(List.of(), 0);
        }

        Set<String> groupIds = memberships.stream().map(Membership::getReferenceId).collect(Collectors.toSet());

        var groupPage = groupQueryService.searchByIds(groupIds, input.environmentId, new PageableImpl(input.page, input.perPage));

        // Build role map: roleId -> Role (resolved in one pass)
        Set<String> roleIds = memberships.stream().map(Membership::getRoleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Role> rolesById = roleQueryService.findByIds(roleIds).stream().collect(Collectors.toMap(Role::getId, r -> r));

        // Build roles per group: groupId -> (scope -> roleName)
        Map<String, Map<String, String>> rolesByGroupId = new HashMap<>();
        for (Membership m : memberships) {
            Role role = rolesById.get(m.getRoleId());
            if (role != null) {
                rolesByGroupId.computeIfAbsent(m.getReferenceId(), k -> new HashMap<>()).put(role.getScope().name(), role.getName());
            }
        }

        // Resolve environment names in one pass
        Set<String> environmentIds = groupPage
            .getContent()
            .stream()
            .map(Group::getEnvironmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<String, String> environmentNames = resolveEnvironmentNames(environmentIds);

        List<UserGroupEntity> data = groupPage
            .getContent()
            .stream()
            .map(group -> {
                String envId = group.getEnvironmentId();
                return UserGroupEntity.builder()
                    .id(group.getId())
                    .name(group.getName())
                    .environmentId(envId)
                    .environmentName(envId != null ? environmentNames.get(envId) : null)
                    .roles(rolesByGroupId.getOrDefault(group.getId(), Map.of()))
                    .build();
            })
            .toList();

        return new Output(data, groupPage.getTotalElements());
    }

    private Map<String, String> resolveEnvironmentNames(Set<String> environmentIds) {
        Map<String, String> result = new HashMap<>();
        for (String envId : environmentIds) {
            try {
                result.put(envId, environmentCrudService.get(envId).getName());
            } catch (Exception e) {
                // Skip environments that cannot be resolved
            }
        }
        return result;
    }

    public record Input(String userId, String environmentId, int page, int perPage) {}

    public record Output(List<UserGroupEntity> data, long totalCount) {}
}
