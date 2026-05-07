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
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignDefaultApiProductRoleToGroupsUpgraderTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String API_PRODUCT_USER_ROLE_ID = "api-product-user-role-id";

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

    @Test(expected = UpgraderException.class)
    public void upgrade_throws_when_environment_lookup_fails() throws UpgraderException, TechnicalException {
        when(environmentRepository.findAll()).thenThrow(new RuntimeException("boom"));
        upgrader.upgrade();
    }

    @Test
    public void should_create_default_api_product_membership_for_groups_missing_one() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));

        Role userRole = role();
        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                eq(RoleScope.API_PRODUCT),
                eq(ROLE_API_PRODUCT_USER.getName()),
                eq(ORG_ID),
                eq(RoleReferenceType.ORGANIZATION)
            )
        ).thenReturn(Optional.of(userRole));

        Group g1 = group("g1");
        Group g2 = group("g2");
        when(groupRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(g1, g2));

        // g1 has no API_PRODUCT default role; g2 already has one
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g1",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of());
        Membership g2Existing = new Membership();
        g2Existing.setReferenceId(null);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g2",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of(g2Existing));

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
    public void should_be_idempotent_when_all_groups_already_have_default_role() throws UpgraderException, TechnicalException {
        Environment env = environment();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));

        when(
            roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                eq(RoleScope.API_PRODUCT),
                eq(ROLE_API_PRODUCT_USER.getName()),
                eq(ORG_ID),
                eq(RoleReferenceType.ORGANIZATION)
            )
        ).thenReturn(Optional.of(role()));

        Group g1 = group("g1");
        when(groupRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(g1));

        Membership existing = new Membership();
        existing.setReferenceId(null);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "g1",
                MembershipMemberType.GROUP,
                MembershipReferenceType.API_PRODUCT
            )
        ).thenReturn(Set.of(existing));

        upgrader.upgrade();

        verify(membershipRepository, never()).create(any());
    }

    @Test
    public void test_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.ASSIGN_DEFAULT_API_PRODUCT_ROLE_TO_GROUPS_UPGRADER);
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

    private Role role() {
        Role role = new Role();
        role.setId(API_PRODUCT_USER_ROLE_ID);
        return role;
    }
}
