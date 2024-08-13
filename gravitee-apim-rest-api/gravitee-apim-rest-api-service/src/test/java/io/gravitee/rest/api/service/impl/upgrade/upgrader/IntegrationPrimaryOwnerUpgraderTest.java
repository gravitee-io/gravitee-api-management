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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationPrimaryOwnerUpgraderTest {

    private static final String ORG_ID = "DEFAULT";
    private static final String INTEGRATION_PRIMARY_OWNER_ROLE_ID = "integration-role-id";
    private static final String API_PRIMARY_OWNER_ROLE_ID = "api-role-id";
    private static final String MEMBER_ID = "member-id";
    private static final String INTEGRATION_ID = "integration-id";
    private static final String API_ID = "api-id";
    private static final int PAGE_SIZE = 100;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private IntegrationRepository integrationRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleRepository roleRepository;

    @Captor
    private ArgumentCaptor<Membership> membershipCaptor;

    @InjectMocks
    private IntegrationPrimaryOwnerUpgrader upgrader;

    @Test
    public void upgrade_should_failed_because_of_exception() throws TechnicalException {
        when(environmentRepository.findAll()).thenThrow(new RuntimeException());

        assertThat(upgrader.upgrade()).isFalse();

        verify(environmentRepository, times(1)).findAll();
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    public void upgrade_should_set_integration_primary_owner_based_on_associated_api_ownership() throws TechnicalException {
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        when(integrationRepository.findAllByEnvironment(any(), any())).thenReturn(new Page<>(List.of(integration()), 0, 1, 1));
        when(apiRepository.search(any(), any())).thenReturn(List.of(api()));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.ofNullable(role(API_PRIMARY_OWNER_ROLE_ID)));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.INTEGRATION,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.ofNullable(role(INTEGRATION_PRIMARY_OWNER_ROLE_ID)));
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, List.of(API_ID), API_PRIMARY_OWNER_ROLE_ID))
            .thenReturn(Set.of(membership()));

        boolean success = upgrader.upgrade();

        assertThat(success).isTrue();

        verify(membershipRepository).create(membershipCaptor.capture());

        Membership membership = membershipCaptor.getValue();
        assertThat(membership.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(membership.getMemberType()).isEqualTo(MembershipMemberType.USER);
        assertThat(membership.getReferenceId()).isEqualTo(INTEGRATION_ID);
        assertThat(membership.getReferenceType()).isEqualTo(MembershipReferenceType.INTEGRATION);
        assertThat(membership.getRoleId()).isEqualTo(INTEGRATION_PRIMARY_OWNER_ROLE_ID);
    }

    @Test
    public void upgrade_should_delete_all_integrations_with_no_associated_apis() throws TechnicalException {
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        when(integrationRepository.findAllByEnvironment(any(), any())).thenReturn(new Page<>(List.of(integration()), 0, 1, 1));
        when(apiRepository.search(any(), any())).thenReturn(List.of());

        boolean success = upgrader.upgrade();

        assertThat(success).isTrue();
        verify(integrationRepository, times(1)).delete(INTEGRATION_ID);
    }

    @Test
    public void upgrade_should_handle_list_of_integrations_with_multiple_pages() throws TechnicalException {
        List<Integration> integrations = new ArrayList<>();
        for (int i = 0; i < 102; i++) {
            integrations.add(new Integration().toBuilder().id(INTEGRATION_ID).build());
        }
        Pageable pageablePage0 = new PageableBuilder().pageNumber(0).pageSize(PAGE_SIZE).build();
        Pageable pageablePage1 = new PageableBuilder().pageNumber(1).pageSize(PAGE_SIZE).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        when(integrationRepository.findAllByEnvironment(ORG_ID, pageablePage0))
            .thenReturn(new Page<>(integrations.subList(0, 100), 0, 100, 102));
        when(integrationRepository.findAllByEnvironment(ORG_ID, pageablePage1))
            .thenReturn(new Page<>(integrations.subList(100, 102), 0, 2, 102));
        when(apiRepository.search(any(), any())).thenReturn(List.of(api()));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.ofNullable(role(API_PRIMARY_OWNER_ROLE_ID)));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.INTEGRATION,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.ofNullable(role(INTEGRATION_PRIMARY_OWNER_ROLE_ID)));
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, List.of(API_ID), API_PRIMARY_OWNER_ROLE_ID))
            .thenReturn(Set.of(membership()));

        boolean success = upgrader.upgrade();
        assertThat(success).isTrue();

        verify(membershipRepository, times(102)).create(any());
    }

    @Test
    public void upgrade_should_fail_if_no_primary_owner_role_exist_for_integration() throws TechnicalException {
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        when(integrationRepository.findAllByEnvironment(any(), any())).thenReturn(new Page<>(List.of(integration()), 0, 1, 1));
        when(apiRepository.search(any(), any())).thenReturn(List.of(api()));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.ofNullable(role(API_PRIMARY_OWNER_ROLE_ID)));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.INTEGRATION,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isFalse();
        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void upgrade_should_fail_if_no_primary_owner_found_for_api() throws TechnicalException {
        when(environmentRepository.findAll()).thenReturn(Set.of(environment()));
        when(integrationRepository.findAllByEnvironment(any(), any())).thenReturn(new Page<>(List.of(integration()), 0, 1, 1));
        when(apiRepository.search(any(), any())).thenReturn(List.of(api()));
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name(),
                ORG_ID,
                RoleReferenceType.ORGANIZATION
            )
        )
            .thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isFalse();
        verify(membershipRepository, never()).create(any());
    }

    private static Integration integration() {
        return new Integration().toBuilder().id(INTEGRATION_ID).build();
    }

    private static Api api() {
        return new Api().withId(API_ID).toBuilder().build();
    }

    private static Role role(String id) {
        return Role.builder().id(id).build();
    }

    private static Membership membership() {
        return Membership.builder().memberId(MEMBER_ID).memberType(MembershipMemberType.USER).build();
    }

    private static Environment environment() {
        return Environment.DEFAULT;
    }
}
