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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiPrimaryOwnerRemovalUpgrader implements Upgrader {

    private final RoleRepository roleRepository;

    private final ApiRepository apiRepository;

    private final MembershipRepository membershipRepository;

    private final OrganizationRepository organizationRepository;

    private final EnvironmentRepository environmentRepository;

    private final UserRepository userRepository;

    private final GroupRepository groupRepository;

    @Value("${services.api-primary-owner-default:}")
    private String defaultPrimaryOwnerId;

    public ApiPrimaryOwnerRemovalUpgrader(
        @Lazy RoleRepository roleRepository,
        @Lazy ApiRepository apiRepository,
        @Lazy MembershipRepository membershipRepository,
        @Lazy OrganizationRepository organizationRepository,
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy UserRepository userRepository,
        @Lazy GroupRepository groupRepository
    ) {
        this.roleRepository = roleRepository;
        this.apiRepository = apiRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.environmentRepository = environmentRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            Set<Organization> organizations = organizationRepository.findAll();
            for (Organization org : organizations) {
                if (!checkOrganization(org.getId())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to fix APIs Primary Owner removal", e);
            return false;
        }
    }

    private boolean checkOrganization(String organizationId) throws TechnicalException {
        String apiPrimaryOwnerRoleId = findApiPrimaryOwnerRoleId(organizationId);
        List<String> environmentIds = findEnvironmentIds(organizationId);
        ArrayList<String> corruptedApiIds = new ArrayList<>();

        int page = 0;
        int size = 100;
        Pageable pageable = new PageableBuilder().pageNumber(page).pageSize(size).build();
        Sortable sortable = new SortableBuilder().field("updated_at").order(Order.DESC).build();

        List<String> apiIds = apiRepository
            .searchIds(List.of(new ApiCriteria.Builder().environments(environmentIds).build()), pageable, sortable)
            .getContent();

        while (!apiIds.isEmpty()) {
            corruptedApiIds.addAll(findCorruptedApiIds(apiPrimaryOwnerRoleId, apiIds));
            pageable = new PageableBuilder().pageNumber(page++).pageSize(size).build();
            apiIds = apiRepository
                .searchIds(List.of(new ApiCriteria.Builder().environments(environmentIds).build()), pageable, sortable)
                .getContent();
        }

        if (!corruptedApiIds.isEmpty()) {
            if (isEmpty(defaultPrimaryOwnerId)) {
                warn(corruptedApiIds);
                return false;
            }

            fix(corruptedApiIds, apiPrimaryOwnerRoleId);
        }

        return true;
    }

    private void warn(List<String> apiIds) {
        log.warn("");
        log.warn("##############################################################");
        log.warn("#                           WARNING                          #");
        log.warn("##############################################################");
        log.warn("");
        log.warn("The following APIs do not have a Primary Owner:");
        log.warn("");
        apiIds.forEach(log::warn);
        log.warn("");
        log.warn("Please edit the services.api-primary-owner-default property of your configuration file to fix this");
        log.warn("This value must refer to a valid user or group ID");
        log.warn("");
        log.warn("##############################################################");
        log.warn("");
    }

    private void fix(List<String> apiIds, String apiPrimaryOwnerRoleId) throws TechnicalException {
        log.info("Attempting to fix APIs without a Primary Owner from configuration");
        Membership membership = prepareMembership(apiPrimaryOwnerRoleId);
        for (String apiId : apiIds) {
            membership.setId(UuidString.generateRandom());
            membership.setReferenceId(apiId);
            membershipRepository.create(membership);
        }
        String memberType = membership.getMemberType().name().toLowerCase();
        log.info("APIs without a Primary Owner has been associated with {} {}", memberType, defaultPrimaryOwnerId);
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_PRIMARY_OWNER_REMOVAL_UPGRADER;
    }

    private String findApiPrimaryOwnerRoleId(String organizationId) throws TechnicalException {
        return roleRepository
            .findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                organizationId,
                RoleReferenceType.ORGANIZATION
            )
            .map(Role::getId)
            .orElseThrow(() -> new TechnicalException("Unable to find API Primary Owner role for organization " + organizationId));
    }

    private List<String> findEnvironmentIds(String organizationId) throws TechnicalException {
        return environmentRepository.findByOrganization(organizationId).stream().map(Environment::getId).collect(toList());
    }

    private List<String> findCorruptedApiIds(String apiPrimaryOwnerRoleId, List<String> apiIds) throws TechnicalException {
        List<String> apiIdWithPrimaryOwner = membershipRepository
            .findByReferencesAndRoleId(MembershipReferenceType.API, apiIds, apiPrimaryOwnerRoleId)
            .stream()
            .map(Membership::getReferenceId)
            .collect(toList());
        List<String> corruptedApiIds = new ArrayList<>(apiIds);
        corruptedApiIds.removeAll(apiIdWithPrimaryOwner);
        return corruptedApiIds;
    }

    private Membership prepareMembership(String poRoleId) throws TechnicalException {
        Optional<User> optUser = userRepository.findById(defaultPrimaryOwnerId);
        if (optUser.isPresent()) {
            User user = optUser.get();
            return membership(user.getId(), MembershipMemberType.USER, poRoleId);
        }

        return groupRepository
            .findById(defaultPrimaryOwnerId)
            .map(group -> membership(group.getId(), MembershipMemberType.GROUP, poRoleId))
            .orElseThrow(() -> new TechnicalException("Unable to find a user or group with id " + defaultPrimaryOwnerId));
    }

    private static Membership membership(String memberId, MembershipMemberType memberType, String roleId) {
        return new Membership(null, memberId, memberType, null, MembershipReferenceType.API, roleId);
    }
}
