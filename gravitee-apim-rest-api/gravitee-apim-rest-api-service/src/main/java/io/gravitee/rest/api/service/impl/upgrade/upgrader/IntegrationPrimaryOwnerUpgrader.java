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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.INTEGRATION_PRIMARY_OWNER_UPGRADER;

import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IntegrationPrimaryOwnerUpgrader implements Upgrader {

    private static final int PAGE_SIZE = 100;
    private static final String DEFAULT_SOURCE = "system";

    private final EnvironmentRepository environmentRepository;
    private IntegrationRepository integrationRepository;
    private final ApiRepository apiRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public IntegrationPrimaryOwnerUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy IntegrationRepository integrationRepository,
        @Lazy ApiRepository apiRepository,
        @Lazy MembershipRepository membershipRepository,
        @Lazy RoleRepository roleRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.integrationRepository = integrationRepository;
        this.apiRepository = apiRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            environmentRepository
                .findAll()
                .forEach(environment -> {
                    ExecutionContext executionContext = new ExecutionContext(environment);
                    try {
                        setPrimaryOwnersForIntegrations(executionContext);
                    } catch (TechnicalException e) {
                        log.error("An error occurs while updating primary owner for integrations", e);
                    }
                });
        } catch (Exception e) {
            log.error("failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    private void setPrimaryOwnersForIntegrations(ExecutionContext executionContext) throws TechnicalException {
        log.info("Start to set primary owners for integrations");

        int handledIntegrations = 0;
        Page<Integration> integrationPage;
        do {
            integrationPage =
                integrationRepository.findAllByEnvironment(
                    executionContext.getEnvironmentId(),
                    new PageableBuilder().pageNumber(handledIntegrations / PAGE_SIZE).pageSize(PAGE_SIZE).build()
                );
            handledIntegrations += (int) integrationPage.getPageElements();

            setPrimaryOwnersForIntegrations(integrationPage, executionContext);
        } while (handledIntegrations < integrationPage.getTotalElements());

        log.info("Finish to set primary owners for integrations");
    }

    private void setPrimaryOwnersForIntegrations(Page<Integration> integrationPage, ExecutionContext executionContext) {
        log.info("Migrating page: {} of integrations", integrationPage.getPageNumber());

        List<String> integrationsIdsWithNoApis = new ArrayList<>();

        integrationPage
            .getContent()
            .stream()
            .map(Integration::getId)
            .forEach(id ->
                findAssociatedApis(id)
                    .findFirst()
                    .ifPresentOrElse(
                        api -> setPrimaryOwnerForIntegration(api, id, executionContext),
                        () -> integrationsIdsWithNoApis.add(id)
                    )
            );

        if (!integrationsIdsWithNoApis.isEmpty()) {
            log.info("Integrations with no associated APIs have been found and will be deleted");
            deleteIntegrationWithNoApiIngested(integrationsIdsWithNoApis);
        }
    }

    private void deleteIntegrationWithNoApiIngested(List<String> integrationIds) {
        log.info("List of Integrations to delete: {}", integrationIds);
        integrationIds.forEach(id -> {
            try {
                integrationRepository.delete(id);
            } catch (TechnicalException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setPrimaryOwnerForIntegration(Api api, String integrationId, ExecutionContext executionContext) {
        log.info("Set primary owner for integration {} inherited from API {}", integrationId, api.getId());
        try {
            var apiPrimaryOwnerRoleId = findPrimaryOwnerRoleId(executionContext.getOrganizationId(), RoleScope.API);
            var integrationPrimaryOwnerRoleId = findPrimaryOwnerRoleId(executionContext.getOrganizationId(), RoleScope.INTEGRATION);
            var membership = findPrimaryOwnerMembership(apiPrimaryOwnerRoleId, List.of(api.getId()))
                .map(m -> integrationMembership(m.getMemberId(), m.getMemberType(), integrationPrimaryOwnerRoleId, integrationId))
                .orElseThrow(() -> new TechnicalException("No membership found"));
            membershipRepository.create(membership);
        } catch (TechnicalException e) {
            throw new RuntimeException(e);
        }
    }

    private String findPrimaryOwnerRoleId(String organizationId, RoleScope roleScope) throws TechnicalException {
        return roleRepository
            .findByScopeAndNameAndReferenceIdAndReferenceType(
                roleScope,
                SystemRole.PRIMARY_OWNER.name(),
                organizationId,
                RoleReferenceType.ORGANIZATION
            )
            .map(Role::getId)
            .orElseThrow(() ->
                new TechnicalException("Unable to find " + roleScope + " Primary Owner role for organization: " + organizationId)
            );
    }

    private Stream<Api> findAssociatedApis(String integrationId) {
        var searchCriteria = new ApiCriteria.Builder().integrationId(integrationId).build();
        var fieldFilter = new io.gravitee.repository.management.api.search.ApiFieldFilter.Builder()
            .excludeDefinition()
            .excludePicture()
            .build();

        return apiRepository.search(searchCriteria, fieldFilter).stream();
    }

    private Optional<Membership> findPrimaryOwnerMembership(String apiPrimaryOwnerRoleId, List<String> apiIds) throws TechnicalException {
        return membershipRepository
            .findByReferencesAndRoleId(MembershipReferenceType.API, apiIds, apiPrimaryOwnerRoleId)
            .stream()
            .findFirst();
    }

    private static Membership integrationMembership(String memberId, MembershipMemberType memberType, String roleId, String integrationId) {
        var membership = new Membership(
            UuidString.generateRandom(),
            memberId,
            memberType,
            integrationId,
            MembershipReferenceType.INTEGRATION,
            roleId
        );
        var now = new Date();
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        membership.setSource(DEFAULT_SOURCE);
        return membership;
    }

    @Override
    public int getOrder() {
        return INTEGRATION_PRIMARY_OWNER_UPGRADER;
    }
}
