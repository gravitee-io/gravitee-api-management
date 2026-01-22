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
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Map;
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

        when(node.id()).thenReturn("node-id");
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
}
