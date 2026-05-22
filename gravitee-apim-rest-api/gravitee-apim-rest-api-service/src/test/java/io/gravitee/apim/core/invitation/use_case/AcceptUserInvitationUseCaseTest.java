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
package io.gravitee.apim.core.invitation.use_case;

import static fixtures.core.model.BaseUserEntityFixtures.aBaseUserEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.AbstractUseCaseTest;
import inmemory.InvitationCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.invitation.domain_service.AcceptInvitationDomainService;
import io.gravitee.apim.core.invitation.exception.InvitationCanceledException;
import io.gravitee.apim.core.invitation.model.GroupInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.GroupInvitationAction;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.UserRegistrationAction;
import io.gravitee.apim.core.user.domain_service.CreateUserDomainService;
import io.gravitee.apim.core.user.model.EncodedPassword;
import io.gravitee.apim.core.user.model.RawPassword;
import io.gravitee.apim.core.user.service_provider.UserPasswordService;
import io.gravitee.apim.core.user.service_provider.UserPortalNotificationService;
import io.gravitee.apim.core.user.service_provider.UserRegistrationEnabledService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserAlreadyFinalizedException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserRegistrationUnavailableException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AcceptUserInvitationUseCaseTest extends AbstractUseCaseTest {

    private static final String USER_EMAIL = "user@example.com";
    private static final String EXISTING_USER_ID = "existing-user-id";
    private static final RawPassword RAW_PASSWORD = new RawPassword("Password123!");
    private static final EncodedPassword ENCODED_PASSWORD = new EncodedPassword("$2a$encoded");

    private final InvitationCrudServiceInMemory invitationCrudService = new InvitationCrudServiceInMemory();

    @Mock
    private AcceptInvitationDomainService acceptInvitationDomainService;

    @Mock
    private UserRegistrationEnabledService userRegistrationEnabledService;

    @Mock
    private UserPasswordService userPasswordService;

    @Mock
    private UserPortalNotificationService userPortalNotificationService;

    @Mock
    private CreateUserDomainService createUserDomainService;

    private AuditDomainService auditDomainService;
    private AcceptUserInvitationUseCase cut;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        cut = new AcceptUserInvitationUseCase(
            userCrudService,
            createUserDomainService,
            invitationCrudService,
            acceptInvitationDomainService,
            userRegistrationEnabledService,
            userPasswordService,
            userPortalNotificationService,
            auditDomainService
        );
        executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        invitationCrudService.reset();
        userCrudService.reset();
        auditCrudService.reset();
    }

    @Nested
    class UserRegistrationAction_Tests {

        @Test
        void should_create_new_user_and_set_password() {
            var createdUser = aBaseUserEntity(GENERATED_UUID, USER_EMAIL);
            when(createUserDomainService.createExternalUser(eq(executionContext), eq(USER_EMAIL), any(), any())).thenReturn(createdUser);
            when(userPasswordService.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new UserRegistrationAction(USER_EMAIL, Optional.empty()),
                    Optional.of(RAW_PASSWORD),
                    Optional.of("John"),
                    Optional.of("Doe")
                )
            );

            assertThat(output.user().getId()).isEqualTo(GENERATED_UUID);
            assertThat(output.user().getUpdatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            assertThat(userCrudService.getStoredPassword(GENERATED_UUID)).contains(ENCODED_PASSWORD);
            verify(userRegistrationEnabledService).checkEnabled(executionContext);
            verify(userPasswordService).validate(RAW_PASSWORD);
            verify(userPortalNotificationService).triggerUserRegistered(executionContext, output.user());
        }

        @Test
        void should_create_new_user_without_password() {
            var createdUser = aBaseUserEntity(GENERATED_UUID, USER_EMAIL);
            when(createUserDomainService.createExternalUser(eq(executionContext), eq(USER_EMAIL), any(), any())).thenReturn(createdUser);

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new UserRegistrationAction(USER_EMAIL, Optional.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(output.user().getId()).isEqualTo(GENERATED_UUID);
            assertThat(userCrudService.getStoredPassword(GENERATED_UUID)).isEmpty();
            verify(userPasswordService, never()).validate(any());
            verify(userPortalNotificationService).triggerUserRegistered(executionContext, output.user());
        }

        @Test
        void should_finalize_existing_user() {
            var existingUser = aBaseUserEntity(EXISTING_USER_ID, USER_EMAIL);
            userCrudService.initWith(List.of(existingUser));
            when(userPasswordService.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new UserRegistrationAction(USER_EMAIL, Optional.of(EXISTING_USER_ID)),
                    Optional.of(RAW_PASSWORD),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(output.user().getId()).isEqualTo(EXISTING_USER_ID);
            assertThat(userCrudService.getStoredPassword(EXISTING_USER_ID)).contains(ENCODED_PASSWORD);
            verify(createUserDomainService, never()).createExternalUser(any(), any(), any(), any());
        }

        @Test
        void should_throw_when_registration_is_disabled() {
            doThrow(new UserRegistrationUnavailableException()).when(userRegistrationEnabledService).checkEnabled(executionContext);

            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new UserRegistrationAction(USER_EMAIL, Optional.empty()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
            ).isInstanceOf(UserRegistrationUnavailableException.class);

            verify(createUserDomainService, never()).createExternalUser(any(), any(), any(), any());
            verify(userPortalNotificationService, never()).triggerUserRegistered(any(), any());
        }

        @Test
        void should_throw_when_existing_user_not_found() {
            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new UserRegistrationAction(USER_EMAIL, Optional.of("nonexistent-id")),
                        Optional.of(RAW_PASSWORD),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
            ).isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void should_throw_when_user_already_finalized() {
            var existingUser = aBaseUserEntity(EXISTING_USER_ID, USER_EMAIL);
            userCrudService.initWith(List.of(existingUser));
            userCrudService.updateAndSetPassword(existingUser, ENCODED_PASSWORD);

            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new UserRegistrationAction(USER_EMAIL, Optional.of(EXISTING_USER_ID)),
                        Optional.of(RAW_PASSWORD),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
            ).isInstanceOf(UserAlreadyFinalizedException.class);
        }

        @Test
        void should_throw_when_password_is_invalid() {
            doThrow(new IllegalArgumentException("Password too weak")).when(userPasswordService).validate(RAW_PASSWORD);

            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new UserRegistrationAction(USER_EMAIL, Optional.empty()),
                        Optional.of(RAW_PASSWORD),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password too weak");

            verify(userRegistrationEnabledService, never()).checkEnabled(any());
            verify(createUserDomainService, never()).createExternalUser(any(), any(), any(), any());
        }

        @Test
        void should_create_organization_audit_log() {
            var createdUser = aBaseUserEntity(GENERATED_UUID, USER_EMAIL);
            when(createUserDomainService.createExternalUser(any(), any(), any(), any())).thenReturn(createdUser);

            cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new UserRegistrationAction(USER_EMAIL, Optional.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .containsExactly(
                    AuditEntity.builder()
                        .id(GENERATED_UUID)
                        .organizationId(ORG_ID)
                        .referenceType(AuditEntity.AuditReferenceType.ORGANIZATION)
                        .referenceId(ORG_ID)
                        .user(GENERATED_UUID)
                        .properties(Map.of(AuditProperties.USER.name(), GENERATED_UUID))
                        .event("USER_CREATED")
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }
    }

    @Nested
    class GroupInvitationAction_Tests {

        @Test
        void should_create_new_user_and_accept_invitations() {
            var invitation = new GroupInvitation(
                InvitationId.of("00000000-0000-0000-0000-000000000001"),
                "group-1",
                "GROUP",
                USER_EMAIL,
                "USER",
                null
            );
            invitationCrudService.initWith(List.of(invitation));
            var createdUser = aBaseUserEntity(GENERATED_UUID, USER_EMAIL);
            when(createUserDomainService.createExternalUser(eq(executionContext), eq(USER_EMAIL), any(), any())).thenReturn(createdUser);

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new GroupInvitationAction(USER_EMAIL, Optional.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(output.user().getId()).isEqualTo(GENERATED_UUID);
            assertThat(invitationCrudService.storage()).isEmpty();
            verify(acceptInvitationDomainService).addMember(executionContext, invitation, GENERATED_UUID);
            verify(userPortalNotificationService).triggerUserRegistered(executionContext, output.user());
        }

        @Test
        void should_accept_multiple_invitations() {
            var inv1 = new GroupInvitation(
                InvitationId.of("00000000-0000-0000-0000-000000000001"),
                "group-1",
                "GROUP",
                USER_EMAIL,
                "USER",
                null
            );
            var inv2 = new GroupInvitation(
                InvitationId.of("00000000-0000-0000-0000-000000000002"),
                "group-2",
                "GROUP",
                USER_EMAIL,
                "ADMIN",
                null
            );
            invitationCrudService.initWith(List.of(inv1, inv2));
            var createdUser = aBaseUserEntity(GENERATED_UUID, USER_EMAIL);
            when(createUserDomainService.createExternalUser(any(), any(), any(), any())).thenReturn(createdUser);

            cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new GroupInvitationAction(USER_EMAIL, Optional.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(invitationCrudService.storage()).isEmpty();
            verify(acceptInvitationDomainService).addMember(executionContext, inv1, GENERATED_UUID);
            verify(acceptInvitationDomainService).addMember(executionContext, inv2, GENERATED_UUID);
        }

        @Test
        void should_finalize_existing_user_and_accept_invitations() {
            var invitation = new GroupInvitation(
                InvitationId.of("00000000-0000-0000-0000-000000000001"),
                "group-1",
                "GROUP",
                USER_EMAIL,
                "USER",
                null
            );
            invitationCrudService.initWith(List.of(invitation));
            var existingUser = aBaseUserEntity(EXISTING_USER_ID, USER_EMAIL);
            userCrudService.initWith(List.of(existingUser));
            when(userPasswordService.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new GroupInvitationAction(USER_EMAIL, Optional.of(EXISTING_USER_ID)),
                    Optional.of(RAW_PASSWORD),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(output.user().getId()).isEqualTo(EXISTING_USER_ID);
            assertThat(invitationCrudService.storage()).isEmpty();
            assertThat(userCrudService.getStoredPassword(EXISTING_USER_ID)).contains(ENCODED_PASSWORD);
            verify(acceptInvitationDomainService).addMember(executionContext, invitation, EXISTING_USER_ID);
            verify(createUserDomainService, never()).createExternalUser(any(), any(), any(), any());
        }

        @Test
        void should_throw_when_no_pending_invitations() {
            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new GroupInvitationAction(USER_EMAIL, Optional.empty()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
            )
                .isInstanceOf(InvitationCanceledException.class)
                .hasMessageContaining("user@example.com");

            verify(createUserDomainService, never()).createExternalUser(any(), any(), any(), any());
            verify(userPortalNotificationService, never()).triggerUserRegistered(any(), any());
        }

        @Test
        void should_not_process_invitations_for_different_email() {
            var invitation = new GroupInvitation(
                InvitationId.of("00000000-0000-0000-0000-000000000001"),
                "group-1",
                "GROUP",
                "other@example.com",
                "USER",
                null
            );
            invitationCrudService.initWith(List.of(invitation));

            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new GroupInvitationAction(USER_EMAIL, Optional.empty()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
            )
                .isInstanceOf(InvitationCanceledException.class)
                .hasMessageContaining("user@example.com");
        }
    }
}
