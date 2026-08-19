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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression tests ensuring that ENVIRONMENT scoped permissions inherited from a group membership are
 * resolved, and that permissions of one environment are isolated from the ones of another environment.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class PermissionIsolationRegressionTest {

    private static final String DEV_ENV_ID = "dev";
    private static final String TEST_ENV_ID = "test";
    private static final String USER_ID = "user-1";
    private static final String GROUP_ID = "le-group";
    private static final String TEST_GROUP_ID = "le-group-test";
    private static final String ROLE_ADMIN_ID = "role-admin";
    private static final String ROLE_READER_ID = "role-reader";
    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private Node node;

    @Mock
    private CommandRepository commandRepository;

    private MembershipServiceImpl membershipService;

    @BeforeEach
    public void init() {
        membershipService = spy(
            new MembershipServiceImpl(
                null,
                userService,
                null,
                null,
                null,
                null,
                membershipRepository,
                roleService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                node,
                objectMapper,
                commandRepository,
                null,
                null
            )
        );

        lenient().when(userService.findById(any(), any())).thenReturn(user());
    }

    @SneakyThrows
    @Test
    public void should_include_create_permission_from_portal_group_for_dev_environment() {
        givenGroupMembershipOnEnvironment(DEV_ENV_ID, GROUP_ID, ROLE_ADMIN_ID);
        givenRoles(adminRole());

        MemberEntity member = membershipService.getUserMember(EXECUTION_CONTEXT, MembershipReferenceType.ENVIRONMENT, DEV_ENV_ID, USER_ID);

        assertThat(member).isNotNull();
        assertThat(member.getPermissions()).containsKey("ENVIRONMENT");
        assertThat(member.getPermissions().get("ENVIRONMENT")).contains('C');
    }

    @SneakyThrows
    @Test
    public void should_include_create_permission_when_test_environment_is_evaluated_first() {
        givenGroupMembershipOnEnvironment(TEST_ENV_ID, TEST_GROUP_ID, ROLE_READER_ID);
        givenGroupMembershipOnEnvironment(DEV_ENV_ID, GROUP_ID, ROLE_ADMIN_ID);
        givenRoles(adminRole(), readerRole());

        // TEST environment is evaluated first: read only
        MemberEntity testMember = membershipService.getUserMember(
            EXECUTION_CONTEXT,
            MembershipReferenceType.ENVIRONMENT,
            TEST_ENV_ID,
            USER_ID
        );

        assertThat(testMember).isNotNull();
        assertThat(testMember.getPermissions().get("ENVIRONMENT")).containsExactly('R');

        // DEV environment must still grant CREATE
        MemberEntity devMember = membershipService.getUserMember(
            EXECUTION_CONTEXT,
            MembershipReferenceType.ENVIRONMENT,
            DEV_ENV_ID,
            USER_ID
        );

        assertThat(devMember).isNotNull();
        assertThat(devMember.getPermissions().get("ENVIRONMENT")).contains('C');
    }

    @SneakyThrows
    @Test
    public void should_preserve_dev_create_permission_after_test_cache_invalidation() {
        givenGroupMembershipOnEnvironment(DEV_ENV_ID, GROUP_ID, ROLE_ADMIN_ID);
        givenRoles(adminRole());

        Map<String, char[]> devPermissions = membershipService.getUserMemberPermissions(
            EXECUTION_CONTEXT,
            MembershipReferenceType.ENVIRONMENT,
            DEV_ENV_ID,
            USER_ID
        );
        assertThat(devPermissions.get("ENVIRONMENT")).contains('C');

        // Invalidating the TEST environment cache must not impact the DEV environment
        membershipService.invalidateRoleCacheAndSendCommand("ENVIRONMENT", TEST_ENV_ID, "USER", USER_ID, EXECUTION_CONTEXT);

        Map<String, char[]> devPermissionsAfterInvalidation = membershipService.getUserMemberPermissions(
            EXECUTION_CONTEXT,
            MembershipReferenceType.ENVIRONMENT,
            DEV_ENV_ID,
            USER_ID
        );

        assertThat(devPermissionsAfterInvalidation).containsKey("ENVIRONMENT");
        assertThat(devPermissionsAfterInvalidation.get("ENVIRONMENT")).contains('C');
    }

    @SneakyThrows
    @Test
    public void should_not_contaminate_dev_permissions_during_concurrent_test_role_change() {
        givenGroupMembershipOnEnvironment(DEV_ENV_ID, GROUP_ID, ROLE_ADMIN_ID);
        givenGroupMembershipOnEnvironment(TEST_ENV_ID, TEST_GROUP_ID, ROLE_READER_ID);
        givenRoles(adminRole(), readerRole());

        Map<String, char[]> devPermissions = membershipService.getUserMemberPermissions(
            EXECUTION_CONTEXT,
            MembershipReferenceType.ENVIRONMENT,
            DEV_ENV_ID,
            USER_ID
        );
        Map<String, char[]> testPermissions = membershipService.getUserMemberPermissions(
            EXECUTION_CONTEXT,
            MembershipReferenceType.ENVIRONMENT,
            TEST_ENV_ID,
            USER_ID
        );

        assertThat(devPermissions.get("ENVIRONMENT")).contains('C');
        assertThat(testPermissions.get("ENVIRONMENT")).containsExactly('R');
    }

    /**
     * The user has no direct membership on the given environment, but belongs to a group which holds an
     * ENVIRONMENT scoped role on it.
     */
    @SneakyThrows
    private void givenGroupMembershipOnEnvironment(String environmentId, String groupId, String roleId) {
        lenient()
            .when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    USER_ID,
                    io.gravitee.repository.management.model.MembershipMemberType.USER,
                    io.gravitee.repository.management.model.MembershipReferenceType.ENVIRONMENT,
                    environmentId
                )
            )
            .thenReturn(Set.of());

        lenient()
            .when(
                membershipRepository.findByReferenceIdAndReferenceType(
                    environmentId,
                    io.gravitee.repository.management.model.MembershipReferenceType.ENVIRONMENT
                )
            )
            .thenReturn(
                List.of(
                    // A direct USER membership on the environment must be ignored when resolving groups
                    Membership.builder()
                        .id("membership-" + environmentId + "-other-user")
                        .memberId("another-user")
                        .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                        .referenceId(environmentId)
                        .referenceType(io.gravitee.repository.management.model.MembershipReferenceType.ENVIRONMENT)
                        .roleId(roleId)
                        .build(),
                    Membership.builder()
                        .id("membership-" + environmentId)
                        .memberId(groupId)
                        .memberType(io.gravitee.repository.management.model.MembershipMemberType.GROUP)
                        .referenceId(environmentId)
                        .referenceType(io.gravitee.repository.management.model.MembershipReferenceType.ENVIRONMENT)
                        .roleId(roleId)
                        .build()
                )
            );

        lenient()
            .when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIds(
                    USER_ID,
                    io.gravitee.repository.management.model.MembershipMemberType.USER,
                    io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                    Set.of(groupId)
                )
            )
            .thenReturn(
                Set.of(
                    Membership.builder()
                        .id("group-membership-" + environmentId)
                        .memberId(USER_ID)
                        .memberType(io.gravitee.repository.management.model.MembershipMemberType.USER)
                        .referenceId(groupId)
                        .referenceType(io.gravitee.repository.management.model.MembershipReferenceType.GROUP)
                        .roleId(roleId)
                        .build()
                )
            );
    }

    private void givenRoles(RoleEntity... roles) {
        for (RoleEntity role : roles) {
            lenient().when(roleService.findByIds(Set.of(role.getId()))).thenReturn(Map.of(role.getId(), role));
        }
    }

    private static RoleEntity adminRole() {
        return RoleEntity.builder()
            .id(ROLE_ADMIN_ID)
            .name("ADMIN")
            .scope(RoleScope.ENVIRONMENT)
            .permissions(Map.of("ENVIRONMENT", new char[] { 'C', 'R', 'U', 'D' }))
            .build();
    }

    private static RoleEntity readerRole() {
        return RoleEntity.builder()
            .id(ROLE_READER_ID)
            .name("READER")
            .scope(RoleScope.ENVIRONMENT)
            .permissions(Map.of("ENVIRONMENT", new char[] { 'R' }))
            .build();
    }

    private static UserEntity user() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        userEntity.setFirstname("John");
        userEntity.setLastname("Doe");
        userEntity.setEmail("john.doe@gravitee.io");
        userEntity.setCreatedAt(new Date());
        userEntity.setUpdatedAt(new Date());
        return userEntity;
    }
}
