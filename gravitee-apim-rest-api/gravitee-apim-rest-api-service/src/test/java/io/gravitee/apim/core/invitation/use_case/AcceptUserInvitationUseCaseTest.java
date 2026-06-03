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

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static fixtures.core.model.BaseUserEntityFixtures.aBaseUserEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.AbstractUseCaseTest;
import inmemory.InvitationCrudServiceInMemory;
import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.invitation.exception.InvitationCanceledException;
import io.gravitee.apim.core.invitation.model.GroupInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.ApplicationInvitationAction;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.GroupInvitationAction;
import io.gravitee.apim.core.invitation.use_case.AcceptUserInvitationUseCase.UserRegistrationAction;
import io.gravitee.apim.core.user.model.EncodedPassword;
import io.gravitee.apim.core.user.model.RawPassword;
import io.gravitee.apim.infra.domain_service.invitation.AcceptInvitationDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.user.CreateUserDomainServiceImpl;
import io.gravitee.apim.infra.domain_service.user.UserPasswordServiceImpl;
import io.gravitee.apim.infra.domain_service.user.UserPortalNotificationServiceImpl;
import io.gravitee.apim.infra.domain_service.user.UserRegistrationEnabledServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PasswordValidator;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PasswordFormatInvalidException;
import io.gravitee.rest.api.service.exceptions.UserAlreadyFinalizedException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserRegistrationUnavailableException;
import io.gravitee.rest.api.service.notification.PortalHook;
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
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @Mock
    private ParameterService parameterService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private PasswordValidator passwordValidator;

    private AuditDomainService auditDomainService;
    private AcceptUserInvitationUseCase cut;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        cut = new AcceptUserInvitationUseCase(
            userCrudService,
            new CreateUserDomainServiceImpl(userCrudService),
            invitationCrudService,
            new AcceptInvitationDomainServiceImpl(membershipDomainService),
            new UserRegistrationEnabledServiceImpl(parameterService),
            new UserPasswordServiceImpl(passwordValidator),
            new UserPortalNotificationServiceImpl(notifierService),
            auditDomainService
        );
        executionContext = new ExecutionContext(ORG_ID, ENV_ID);
        invitationCrudService.reset();
        userCrudService.reset();
        auditCrudService.reset();
        membershipDomainService.reset();
    }

    @Nested
    class UserRegistrationAction_Tests {

        @Test
        void should_create_new_user_and_set_password() {
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(true);
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

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
            assertThat(userCrudService.getStoredPassword(GENERATED_UUID)).isPresent();
            verify(notifierService).trigger(eq(executionContext), eq(PortalHook.USER_REGISTERED), any());
        }

        @Test
        void should_create_new_user_without_password() {
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

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
            verify(notifierService).trigger(eq(executionContext), eq(PortalHook.USER_REGISTERED), any());
        }

        @Test
        void should_finalize_existing_user() {
            var existingUser = aBaseUserEntity(EXISTING_USER_ID, USER_EMAIL);
            userCrudService.initWith(List.of(existingUser));
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(true);
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

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
            assertThat(userCrudService.getStoredPassword(EXISTING_USER_ID)).isPresent();
        }

        @Test
        void should_throw_when_registration_is_disabled() {
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(false);

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
        }

        @Test
        void should_throw_when_existing_user_not_found() {
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(true);
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

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
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(true);
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

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
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(false);

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
            ).isInstanceOf(PasswordFormatInvalidException.class);
        }

        @Test
        void should_create_organization_audit_log() {
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    ENV_ID,
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

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
            assertThat(membershipDomainService.storage()).isNotEmpty();
            verify(notifierService).trigger(eq(executionContext), eq(PortalHook.USER_REGISTERED), any());
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
            assertThat(membershipDomainService.storage()).hasSize(2);
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
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(true);

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
            assertThat(userCrudService.getStoredPassword(EXISTING_USER_ID)).isPresent();
            assertThat(membershipDomainService.storage()).isNotEmpty();
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

    @Nested
    class ApplicationInvitationAction_Tests {

        @Test
        void should_create_new_user_and_accept_application_invitation() {
            var invitation = anApplicationInvitation("00000000-0000-0000-0000-000000000001", "app-1", USER_EMAIL, "USER");
            invitationCrudService.initWith(List.of(invitation));

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new ApplicationInvitationAction(USER_EMAIL, Optional.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(output.user().getId()).isEqualTo(GENERATED_UUID);
            assertThat(invitationCrudService.storage()).isEmpty();
            assertThat(membershipDomainService.storage())
                .hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.getReferenceType()).isEqualTo(MembershipReferenceType.APPLICATION);
                    assertThat(m.getReferenceId()).isEqualTo("app-1");
                    assertThat(m.getId()).isEqualTo(GENERATED_UUID);
                });
            verify(notifierService).trigger(eq(executionContext), eq(PortalHook.USER_REGISTERED), any());
        }

        @Test
        void should_accept_multiple_application_invitations() {
            var inv1 = anApplicationInvitation("00000000-0000-0000-0000-000000000001", "app-1", USER_EMAIL, "USER");
            var inv2 = anApplicationInvitation("00000000-0000-0000-0000-000000000002", "app-2", USER_EMAIL, "OWNER");
            invitationCrudService.initWith(List.of(inv1, inv2));

            cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new ApplicationInvitationAction(USER_EMAIL, Optional.empty()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(invitationCrudService.storage()).isEmpty();
            assertThat(membershipDomainService.storage())
                .hasSize(2)
                .allSatisfy(m -> assertThat(m.getReferenceType()).isEqualTo(MembershipReferenceType.APPLICATION));
        }

        @Test
        void should_finalize_existing_user_and_accept_application_invitation() {
            var invitation = anApplicationInvitation("00000000-0000-0000-0000-000000000001", "app-1", USER_EMAIL, "USER");
            invitationCrudService.initWith(List.of(invitation));
            var existingUser = aBaseUserEntity(EXISTING_USER_ID, USER_EMAIL);
            userCrudService.initWith(List.of(existingUser));
            when(passwordValidator.validate(RAW_PASSWORD.value())).thenReturn(true);

            var output = cut.execute(
                new AcceptUserInvitationUseCase.Input(
                    executionContext,
                    new ApplicationInvitationAction(USER_EMAIL, Optional.of(EXISTING_USER_ID)),
                    Optional.of(RAW_PASSWORD),
                    Optional.empty(),
                    Optional.empty()
                )
            );

            assertThat(output.user().getId()).isEqualTo(EXISTING_USER_ID);
            assertThat(invitationCrudService.storage()).isEmpty();
            assertThat(userCrudService.getStoredPassword(EXISTING_USER_ID)).isPresent();
            assertThat(membershipDomainService.storage())
                .hasSize(1)
                .first()
                .satisfies(m -> assertThat(m.getReferenceType()).isEqualTo(MembershipReferenceType.APPLICATION));
        }

        @Test
        void should_throw_when_no_pending_invitations() {
            assertThatThrownBy(() ->
                cut.execute(
                    new AcceptUserInvitationUseCase.Input(
                        executionContext,
                        new ApplicationInvitationAction(USER_EMAIL, Optional.empty()),
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
