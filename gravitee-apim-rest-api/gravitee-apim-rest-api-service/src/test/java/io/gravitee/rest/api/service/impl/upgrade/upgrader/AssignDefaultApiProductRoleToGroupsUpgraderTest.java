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

import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AssignDefaultApiProductRoleToGroupsUpgraderTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String API_PRODUCT_USER_ROLE_ID = "api-product-user-role-id";
    private static final String API_PRODUCT_OWNER_ROLE_ID = "api-product-owner-role-id";

    @InjectMocks
    AssignDefaultApiProductRoleToGroupsUpgrader upgrader;

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    MembershipRepository membershipRepository;

    @Mock
    RoleRepository roleRepository;

    @Test
    public void upgrade_throws_when_environment_lookup_fails() throws TechnicalException {
        assertThrows(UpgraderException.class, () -> {
            when(environmentRepository.findAll()).thenThrow(new RuntimeException("boom"));
            upgrader.upgrade();
        });
    }

    @Test
    public void should_create_default_api_product_membership_for_groups_missing_one() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));
        stubApiProductRoles();

        Group g1 = group("g1");
        Group g2 = group("g2");
        when(groupRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(g1, g2));

        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g1",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of());
        Membership g2Existing = new Membership();
        g2Existing.setReferenceId(null);
        g2Existing.setRoleId(API_PRODUCT_USER_ROLE_ID);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g2",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of(g2Existing));

        when(membershipRepository.findByReferenceIdAndReferenceType("g1", MembershipReferenceType.GROUP)).thenReturn(List.of());
        when(membershipRepository.findByReferenceIdAndReferenceType("g2", MembershipReferenceType.GROUP)).thenReturn(List.of());

        upgrader.upgrade();

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository, times(1)).create(membershipCaptor.capture());
        Membership created = membershipCaptor.getValue();
        assertThat(created.getMemberId()).isEqualTo("g1");
        assertThat(created.getMemberType()).isEqualTo(MembershipMemberType.GROUP);
        assertThat(created.getReferenceType()).isEqualTo(MembershipReferenceType.API_PRODUCT);
        assertThat(created.getReferenceId()).isNull();
        assertThat(created.getRoleId()).isEqualTo(API_PRODUCT_USER_ROLE_ID);
    }

    @Test
    public void should_backfill_api_product_role_for_legacy_group_members() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));
        stubApiProductRoles();
        when(groupRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(group("g1")));

        Membership groupDefault = new Membership();
        groupDefault.setReferenceId(null);
        groupDefault.setRoleId(API_PRODUCT_USER_ROLE_ID);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g1",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of(groupDefault));

        Membership legacyApiRole = new Membership();
        legacyApiRole.setMemberId("user-1");
        legacyApiRole.setMemberType(MembershipMemberType.USER);
        legacyApiRole.setReferenceId("g1");
        legacyApiRole.setReferenceType(MembershipReferenceType.GROUP);
        legacyApiRole.setRoleId("api-user-role-id");
        when(membershipRepository.findByReferenceIdAndReferenceType("g1", MembershipReferenceType.GROUP)).thenReturn(
            List.of(legacyApiRole)
        );

        upgrader.upgrade();

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository, times(1)).create(membershipCaptor.capture());
        Membership created = membershipCaptor.getValue();
        assertThat(created.getMemberId()).isEqualTo("user-1");
        assertThat(created.getMemberType()).isEqualTo(MembershipMemberType.USER);
        assertThat(created.getReferenceType()).isEqualTo(MembershipReferenceType.GROUP);
        assertThat(created.getReferenceId()).isEqualTo("g1");
        assertThat(created.getRoleId()).isEqualTo(API_PRODUCT_USER_ROLE_ID);
    }

    @Test
    public void should_skip_member_backfill_when_api_product_role_already_exists() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));
        stubApiProductRoles();
        when(groupRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(group("g1")));

        Membership groupDefault = new Membership();
        groupDefault.setReferenceId(null);
        groupDefault.setRoleId(API_PRODUCT_USER_ROLE_ID);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g1",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of(groupDefault));

        Membership existingApiProductRole = new Membership();
        existingApiProductRole.setMemberId("user-1");
        existingApiProductRole.setMemberType(MembershipMemberType.USER);
        existingApiProductRole.setReferenceId("g1");
        existingApiProductRole.setReferenceType(MembershipReferenceType.GROUP);
        existingApiProductRole.setRoleId(API_PRODUCT_USER_ROLE_ID);
        when(membershipRepository.findByReferenceIdAndReferenceType("g1", MembershipReferenceType.GROUP)).thenReturn(
            List.of(existingApiProductRole)
        );

        upgrader.upgrade();

        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void should_skip_environment_when_api_product_user_role_is_missing() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));

        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                eq(RoleScope.API_PRODUCT),
                eq(ROLE_API_PRODUCT_USER.getName()),
                eq(ORG_ID),
                eq(RoleReferenceType.ORGANIZATION)
            )
        ).thenReturn(Optional.empty());

        upgrader.upgrade();

        verify(groupRepository, never()).findAllByEnvironment(any());
        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void should_be_idempotent_when_all_groups_and_members_already_have_roles() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));
        stubApiProductRoles();

        Group g1 = group("g1");
        when(groupRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(g1));

        Membership existing = new Membership();
        existing.setReferenceId(null);
        existing.setRoleId(API_PRODUCT_USER_ROLE_ID);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g1",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of(existing));
        when(membershipRepository.findByReferenceIdAndReferenceType("g1", MembershipReferenceType.GROUP)).thenReturn(List.of());

        upgrader.upgrade();

        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void test_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.ASSIGN_DEFAULT_API_PRODUCT_ROLE_TO_GROUPS_UPGRADER);
    }

    private void stubApiProductRoles() throws TechnicalException {
        Role userRole = role(API_PRODUCT_USER_ROLE_ID);
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                eq(RoleScope.API_PRODUCT),
                eq(ROLE_API_PRODUCT_USER.getName()),
                eq(ORG_ID),
                eq(RoleReferenceType.ORGANIZATION)
            )
        ).thenReturn(Optional.of(userRole));
        when(
            roleRepository.findByScopeAndReferenceIdAndReferenceType(RoleScope.API_PRODUCT, ORG_ID, RoleReferenceType.ORGANIZATION)
        ).thenReturn(Set.of(userRole, role(API_PRODUCT_OWNER_ROLE_ID)));
    }

    private Environment environment() {
        Environment env = new Environment();
        env.setId(ENV_ID);
        env.setOrganizationId(ORG_ID);
        return env;
    }

    private Group group(String id) {
        Group group = new Group();
        group.setId(id);
        group.setEnvironmentId(ENV_ID);
        return group;
    }

    private Role role(String id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }
}
