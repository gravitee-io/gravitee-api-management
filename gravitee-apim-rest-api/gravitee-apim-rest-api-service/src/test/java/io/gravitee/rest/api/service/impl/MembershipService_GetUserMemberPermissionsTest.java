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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MembershipService_GetUserMemberPermissionsTest {

    private static final String REFERENCE_ID = "ref-id";
    private static final String USER_ID = "user-id";
    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Node node;

    @Mock
    private CommandRepository commandRepository;

    private MembershipServiceImpl membershipService;

    @BeforeEach
    public void init() {
        membershipService = new MembershipServiceImpl(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
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
        );
        membershipService = spy(membershipService);
    }

    @Test
    public void should_cached_user_permissions() {
        MemberEntity member = new MemberEntity();
        member.setPermissions(Map.of("API", new char[] { 'C', 'R', 'U', 'D' }));

        // First call
        doReturn(member).when(membershipService).getUserMember(EXECUTION_CONTEXT, MembershipReferenceType.API, REFERENCE_ID, USER_ID);

        Map<String, char[]> permissions1 = membershipService.getUserMemberPermissions(
            EXECUTION_CONTEXT,
            MembershipReferenceType.API,
            REFERENCE_ID,
            USER_ID
        );

        assertThat(permissions1).containsKey("API");
        assertThat(permissions1.get("API")).containsExactly('C', 'R', 'U', 'D');

        // Second call - should use cache
        Map<String, char[]> permissions2 = membershipService.getUserMemberPermissions(
            EXECUTION_CONTEXT,
            MembershipReferenceType.API,
            REFERENCE_ID,
            USER_ID
        );

        assertThat(permissions2).isSameAs(permissions1);
        verify(membershipService, times(1)).getUserMember(EXECUTION_CONTEXT, MembershipReferenceType.API, REFERENCE_ID, USER_ID);
    }

    @SneakyThrows
    @Test
    public void should_cached_invalidate_cache() {
        MemberEntity member = new MemberEntity();
        member.setPermissions(Map.of("API", new char[] { 'R' }));

        doReturn(member).when(membershipService).getUserMember(EXECUTION_CONTEXT, MembershipReferenceType.API, REFERENCE_ID, USER_ID);

        // First call to fill cache
        membershipService.getUserMemberPermissions(EXECUTION_CONTEXT, MembershipReferenceType.API, REFERENCE_ID, USER_ID);

        // Invalidate cache
        membershipService.invalidateRoleCacheAndSendCommand("API", REFERENCE_ID, "USER", USER_ID, EXECUTION_CONTEXT);

        // Second call - should call getUserMember again
        membershipService.getUserMemberPermissions(EXECUTION_CONTEXT, MembershipReferenceType.API, REFERENCE_ID, USER_ID);

        verify(membershipService, times(2)).getUserMember(EXECUTION_CONTEXT, MembershipReferenceType.API, REFERENCE_ID, USER_ID);
        verify(commandRepository).create(any());
    }

    /**
     * Regression test for cross-environment permission bleed.
     *
     * <p>Bug: A user assigned to PORTAL_GROUP (which carries LE_GROUP/PORTAL_APP_CREATOR with
     * APPLICATION CREATE in DEV environment) also belongs to PORTAL_GROUP in TEST environment
     * with UE_GROUP/PORTAL_APP_READER (APPLICATION READ only). When {@code getUserMember} is
     * called for ENVIRONMENT "DEV", the returned permissions must include APPLICATION CREATE.
     *
     * <p>Root-cause: in {@code getUserMember()} the switch-statement for {@code entityGroups}
     * falls through to {@code default -> Set.of()} for ENVIRONMENT reference type, so the
     * group-membership block (which would bring in LE_GROUP's CREATE permission) is never
     * entered. As a result only the direct ENVIRONMENT "DEV" membership – which in this
     * scenario carries the READ-only UE_GROUP role that bled in from the TEST context –
     * contributes to the returned permissions, and APPLICATION CREATE is absent.
     */
    @Test
    public void should_return_correct_permissions_when_user_has_different_roles_in_different_environments() throws Exception {
        // ---- local mocks (created via mock() so strict-stubbing rules from @Mock don't apply) ----
        MembershipRepository localMembershipRepo = mock(MembershipRepository.class);
        RoleService localRoleService = mock(RoleService.class);
        UserService localUserService = mock(UserService.class);

        // Constructor order: identityService, userService, applicationRepository, eventManager,
        // primaryOwnerService, emailService, membershipRepository, roleService,
        // applicationService, applicationAlertService, apiSearchService, apiGroupService,
        // apiRepository, groupService, auditService, parameterService,
        // integrationRepository, node, objectMapper, commandRepository,
        // apiMetadataService, searchEngineService
        MembershipServiceImpl localService = new MembershipServiceImpl(
            null,
            localUserService,
            null,
            null,
            null,
            null,
            localMembershipRepo,
            localRoleService,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        String userId = "test-user-id";
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        // --- Direct membership in ENVIRONMENT "DEV" carries only UE_GROUP (READ-only) ---
        // This simulates the TEST-environment role that has bled into the DEV context due to the bug:
        // the user's PORTAL_GROUP membership in TEST (UE_GROUP, APPLICATION READ) incorrectly
        // surfaces as their direct ENVIRONMENT "DEV" permission instead of LE_GROUP (CREATE).
        io.gravitee.repository.management.model.Membership directMembership = mock(
            io.gravitee.repository.management.model.Membership.class
        );
        when(directMembership.getRoleId()).thenReturn("UE_GROUP_ROLE_ID");

        when(
            localMembershipRepo.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userId,
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.ENVIRONMENT,
                "DEV"
            )
        ).thenReturn(Set.of(directMembership));

        // UE_GROUP: APPLICATION READ only – the TEST-environment role
        RoleEntity ueGroupRole = mock(RoleEntity.class);
        when(ueGroupRole.getPermissions()).thenReturn(Map.of("APPLICATION", new char[] { 'R' }));
        when(localRoleService.findByIds(Set.of("UE_GROUP_ROLE_ID"))).thenReturn(Map.of("UE_GROUP_ROLE_ID", ueGroupRole));

        // User entity (field values can be null for this test)
        UserEntity userEntity = mock(UserEntity.class);
        when(localUserService.findById(executionContext, userId)).thenReturn(userEntity);

        // ---- Act ----
        MemberEntity result = localService.getUserMember(executionContext, MembershipReferenceType.ENVIRONMENT, "DEV", userId);

        // ---- Assert ----
        // The user belongs to PORTAL_GROUP whose LE_GROUP role in ENVIRONMENT "DEV"
        // grants APPLICATION CREATE.  That group-based CREATE must appear in the result.
        //
        // FAILS with the current buggy code because:
        //   * For ENVIRONMENT reference type, entityGroups = Set.of() (switch default branch)
        //   * The group-membership block is therefore never entered
        //   * Only the direct membership (UE_GROUP, APPLICATION READ) contributes
        //   * Result permissions: APPLICATION -> ['R'], missing 'C' (CREATE)
        assertThat(result).isNotNull();
        assertThat(result.getPermissions()).containsKey("APPLICATION");
        assertThat(result.getPermissions().get("APPLICATION")).contains('C');
    }
}
