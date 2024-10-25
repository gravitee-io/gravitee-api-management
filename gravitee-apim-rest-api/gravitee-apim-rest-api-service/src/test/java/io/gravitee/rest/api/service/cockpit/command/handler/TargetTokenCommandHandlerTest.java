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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ORGANIZATION_USER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_FEDERATION_AGENT;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.cockpit.api.command.v1.targettoken.TargetTokenCommand;
import io.gravitee.cockpit.api.command.v1.targettoken.TargetTokenCommandPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.TokenEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TargetTokenCommandHandlerTest {

    private static final String ORGANISATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String COMMAND_ID = "command-id";
    private static final String USER_CREATION_FAILED_MESSAGE = "Failed to create user.";
    private static final String ORG_ROLE_ASSIGNMENT_FAILED_MESSAGE = "Failed to assign organization role.";
    private static final String ENV_ROLE_ASSIGNMENT_FAILED_MESSAGE = "Failed to assign environment role.";

    @Mock
    private UserService userService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private TargetTokenCommandHandler targetTokenCommandHandler;

    private TargetTokenCommand command;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(USER_ID);
    }

    @Test
    void shouldGenerateTokenSuccessfullyForGKO() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(SystemRole.ADMIN.name()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.of(new RoleEntity()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), eq(ROLE_ENVIRONMENT_API_PUBLISHER.getName()), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.of(new RoleEntity()));

        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setToken("token gko");
        when(tokenService.create(any(), any(), eq(USER_ID))).thenReturn(tokenEntity);

        targetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> CommandStatus.SUCCEEDED.equals(reply.getCommandStatus()))
            .assertValue(reply -> "token gko".equals(reply.getTargetToken()));
    }

    @Test
    void shouldGenerateTokenSuccessfullyForFederation() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.FEDERATION));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(DEFAULT_ROLE_ORGANIZATION_USER.getName()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.of(new RoleEntity()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), eq(ROLE_ENVIRONMENT_FEDERATION_AGENT.getName()), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.of(new RoleEntity()));

        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setToken("token federation");
        when(tokenService.create(any(), any(), eq(USER_ID))).thenReturn(tokenEntity);

        targetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> CommandStatus.SUCCEEDED.equals(reply.getCommandStatus()))
            .assertValue(reply -> "token federation".equals(reply.getTargetToken()));
    }

    @Test
    void shouldReturnErrorWhenUserCreationFails() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenThrow(new RuntimeException("User creation failed"));

        targetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertValue(reply -> CommandStatus.ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> reply.getErrorDetails().contains(USER_CREATION_FAILED_MESSAGE));
    }

    @Test
    void shouldReturnErrorWhenOrganizationRoleAssignmentFails() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(SystemRole.ADMIN.name()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.empty());

        targetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertValue(reply -> CommandStatus.ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> reply.getErrorDetails().contains(ORG_ROLE_ASSIGNMENT_FAILED_MESSAGE));
    }

    @Test
    void shouldRollbackUserCreationWhenOrganizationRoleAssignmentFails() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(SystemRole.ADMIN.name()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.empty());

        targetTokenCommandHandler.handle(command).test().awaitDone(2, SECONDS);

        verify(userService, times(1)).delete(any(), eq(USER_ID));
    }

    @Test
    void shouldReturnErrorWhenEnvironmentRoleAssignmentFails() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(SystemRole.ADMIN.name()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.of(new RoleEntity()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), eq(ROLE_ENVIRONMENT_API_PUBLISHER.getName()), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.empty());

        targetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertValue(reply -> CommandStatus.ERROR.equals(reply.getCommandStatus()))
            .assertValue(reply -> reply.getErrorDetails().contains(ENV_ROLE_ASSIGNMENT_FAILED_MESSAGE));
    }

    @Test
    void shouldRollbackUserCreationAndMembershipAssignmentWhenEnvironmentRoleAssignmentFails() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(SystemRole.ADMIN.name()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.of(new RoleEntity()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), eq(ROLE_ENVIRONMENT_API_PUBLISHER.getName()), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.empty());

        targetTokenCommandHandler.handle(command).test().awaitDone(2, SECONDS);

        verify(userService, times(1)).delete(any(), eq(USER_ID));
        verify(membershipService, times(1)).removeMemberMemberships(any(), eq(MembershipMemberType.USER), eq(USER_ID));
    }

    @Test
    void shouldRollbackUserCreationAndMembershipAssignmentWhenTokenCreationFails() {
        command = new TargetTokenCommand(generatePayload(TargetTokenCommandPayload.Scope.GKO));

        when(userService.create(any(), any(), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(eq(RoleScope.ORGANIZATION), eq(SystemRole.ADMIN.name()), eq(ORGANISATION_ID)))
            .thenReturn(Optional.of(new RoleEntity()));
        when(roleService.findByScopeAndName(eq(RoleScope.ENVIRONMENT), eq(ROLE_ENVIRONMENT_API_PUBLISHER.getName()), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.of(new RoleEntity()));
        when(tokenService.create(any(), any(), anyString())).thenThrow(new RuntimeException("Token creation failed"));

        targetTokenCommandHandler.handle(command).test().awaitDone(2, SECONDS);

        verify(userService, times(1)).delete(any(), eq(USER_ID));
        verify(membershipService, times(1)).removeMemberMemberships(any(), eq(MembershipMemberType.USER), eq(USER_ID));
    }

    private static TargetTokenCommandPayload generatePayload(TargetTokenCommandPayload.Scope scope) {
        return TargetTokenCommandPayload
            .builder()
            .organizationId(ORGANISATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .id(COMMAND_ID)
            .scope(scope)
            .build();
    }
}
