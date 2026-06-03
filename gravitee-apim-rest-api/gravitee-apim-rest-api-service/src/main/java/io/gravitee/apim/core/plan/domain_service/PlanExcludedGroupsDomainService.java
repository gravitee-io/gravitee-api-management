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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates plan excluded groups and resolves whether a user can access an API Product plan
 * based on group memberships, mirroring the rules used for API plan access control.
 */
@DomainService
@RequiredArgsConstructor
public class PlanExcludedGroupsDomainService {

    private final GroupQueryService groupQueryService;
    private final MembershipQueryService membershipQueryService;
    private final RoleQueryService roleQueryService;

    public void validateExcludedGroups(String environmentId, List<String> excludedGroups) {
        if (excludedGroups == null || excludedGroups.isEmpty()) {
            return;
        }

        Set<String> validGroupIds = groupQueryService
            .findByIds(new HashSet<>(excludedGroups))
            .stream()
            .filter(group -> environmentId.equals(group.getEnvironmentId()))
            .map(Group::getId)
            .collect(Collectors.toSet());

        excludedGroups.forEach(excludedGroupId -> {
            if (!validGroupIds.contains(excludedGroupId)) {
                throw new GroupNotFoundException(excludedGroupId);
            }
        });
    }

    public boolean isUserAuthorizedToAccessApiProductPlan(ApiProduct apiProduct, List<String> excludedGroups, String username) {
        if (username == null) {
            return excludedGroups == null || excludedGroups.isEmpty();
        }

        if (excludedGroups == null || excludedGroups.isEmpty()) {
            return true;
        }

        if (isDirectApiProductMember(apiProduct.getId(), username)) {
            return true;
        }

        Set<String> groupsWithApiProductRole = findUserGroupsWithApiProductRole(username);

        if (apiProduct.getGroups() != null && !apiProduct.getGroups().isEmpty()) {
            Set<String> authorizedGroups = new HashSet<>(apiProduct.getGroups());
            authorizedGroups.removeAll(excludedGroups);
            return authorizedGroups.stream().anyMatch(groupsWithApiProductRole::contains);
        }

        return excludedGroups.stream().noneMatch(groupsWithApiProductRole::contains);
    }

    private boolean isDirectApiProductMember(String apiProductId, String username) {
        return membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(username, Membership.Type.USER, Membership.ReferenceType.API_PRODUCT)
            .stream()
            .anyMatch(membership -> apiProductId.equals(membership.getReferenceId()));
    }

    private Set<String> findUserGroupsWithApiProductRole(String username) {
        Collection<Membership> groupMemberships = membershipQueryService.findGroupsThatUserBelongsTo(username);
        Set<String> roleIds = groupMemberships.stream().map(Membership::getRoleId).filter(Objects::nonNull).collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return Set.of();
        }

        Set<String> apiProductRoleIds = roleQueryService
            .findByIds(roleIds)
            .stream()
            .filter(role -> Role.Scope.API_PRODUCT.equals(role.getScope()))
            .map(Role::getId)
            .collect(Collectors.toSet());

        return groupMemberships
            .stream()
            .filter(membership -> apiProductRoleIds.contains(membership.getRoleId()))
            .map(Membership::getReferenceId)
            .collect(Collectors.toSet());
    }
}
