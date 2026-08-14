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

import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_REGISTRATION;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_REGISTRATION_URL;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.REGISTRATION_PATH;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PasswordValidator;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.UserRegistrationPendingApprovalException;
import io.gravitee.rest.api.service.exceptions.UserStateConflictException;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.ConfigurableEnvironment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceRegistrationApprovalTest {

    private static final String ORGANIZATION = "DEFAULT";
    private static final String ENVIRONMENT = "DEFAULT";
    private static final ExecutionContext PORTAL_CONTEXT = new ExecutionContext(ORGANIZATION, ENVIRONMENT);
    private static final ExecutionContext CONSOLE_CONTEXT = new ExecutionContext(ORGANIZATION);
    private static final String USER_ID = "user-1";
    private static final String EMAIL = "user@example.com";
    private static final String JWT_SECRET = "VERYSECURE";
    private static final String PORTAL_URL = "https://portal.example.com";
    private static final String CONSOLE_URL = "https://console.example.com";
    private static final String OTHER_ENVIRONMENT = "production";
    private static final String OTHER_PORTAL_URL = "https://portal.production.example.com";

    @InjectMocks
    private UserServiceImpl userService = new UserServiceImpl();

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private EmailService emailService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private AuditService auditService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private UserMetadataService userMetadataService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private RoleService roleService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private UserConverter userConverter;

    @BeforeEach
    void setUp() {
        when(userConverter.toUser(any(NewExternalUserEntity.class))).thenCallRealMethod();
        when(userConverter.toUserEntity(any(User.class), any())).thenCallRealMethod();
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(
            environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER)
        ).thenReturn(3600);
        when(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER)).thenReturn(DEFAULT_JWT_ISSUER);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_not_send_the_registration_email_when_the_request_has_to_be_approved() throws TechnicalException {
        givenUserRegistrationEnabled(false);

        userService.register(PORTAL_CONTEXT, newExternalUser());

        verify(emailService, never()).sendAsyncEmailNotification(eq(PORTAL_CONTEXT), any());
        verify(notifierService).trigger(eq(PORTAL_CONTEXT), eq(PortalHook.USER_REGISTRATION_REQUEST), any());
    }

    @Test
    void should_create_a_pending_user_when_the_request_has_to_be_approved() throws TechnicalException {
        givenUserRegistrationEnabled(false);

        userService.register(PORTAL_CONTEXT, newExternalUser());

        verify(userRepository).create(argThat(user -> user.getStatus() == UserStatus.PENDING));
    }

    @Test
    void should_send_the_registration_email_when_registrations_are_automatically_validated() throws TechnicalException {
        givenUserRegistrationEnabled(true);

        userService.register(PORTAL_CONTEXT, newExternalUser());

        assertThat(capturedEmails()).anyMatch(this::isRegistrationEmail);
        verify(notifierService).trigger(eq(PORTAL_CONTEXT), eq(PortalHook.USER_REGISTERED), any());
    }

    @Test
    void should_send_the_registration_email_once_the_request_is_approved() throws TechnicalException {
        givenPendingUser(null);
        when(installationAccessQueryService.getPortalUrl(ENVIRONMENT)).thenReturn(PORTAL_URL);

        userService.processRegistration(CONSOLE_CONTEXT, USER_ID, true);

        EmailNotification registrationEmail = capturedEmails()
            .stream()
            .filter(this::isRegistrationEmail)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no registration email has been sent"));

        assertThat(registrationEmail.getTo()).containsExactly(EMAIL);
        assertThat((String) registrationEmail.getParams().get(PARAM_REGISTRATION_URL)).startsWith(
            PORTAL_URL + "/user/registration/confirm/"
        );
    }

    @Test
    void should_only_send_the_registration_email_once_the_request_is_approved() throws TechnicalException {
        givenPendingUser(null);
        when(installationAccessQueryService.getPortalUrl(ENVIRONMENT)).thenReturn(PORTAL_URL);

        userService.processRegistration(CONSOLE_CONTEXT, USER_ID, true);

        assertThat(capturedEmails()).hasSize(1).noneMatch(this::isRequestProcessedEmail);
    }

    @Test
    void should_not_send_the_registration_email_when_the_approved_user_already_has_a_password() throws TechnicalException {
        givenPendingUser("$2a$10$encoded");

        userService.processRegistration(CONSOLE_CONTEXT, USER_ID, true);

        assertThat(capturedEmails()).noneMatch(this::isRegistrationEmail).anyMatch(this::isRequestProcessedEmail);
    }

    @Test
    void should_not_send_the_registration_email_when_the_request_is_rejected() throws TechnicalException {
        givenPendingUser(null);

        userService.processRegistration(CONSOLE_CONTEXT, USER_ID, false);

        assertThat(capturedEmails()).noneMatch(this::isRegistrationEmail).anyMatch(this::isRequestProcessedEmail);
    }

    @Test
    void should_reject_the_finalization_of_a_registration_awaiting_approval() throws TechnicalException {
        when(
            parameterService.findAsBoolean(PORTAL_CONTEXT, Key.PORTAL_USERCREATION_ENABLED, ENVIRONMENT, ParameterReferenceType.ENVIRONMENT)
        ).thenReturn(Boolean.TRUE);
        when(passwordValidator.validate(any())).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(of(pendingUser(null)));

        RegisterUserEntity registerUserEntity = new RegisterUserEntity();
        registerUserEntity.setToken(registrationToken());
        registerUserEntity.setPassword("gh2gyf8!zjfnz");

        assertThatThrownBy(() -> userService.finalizeRegistration(PORTAL_CONTEXT, registerUserEntity)).isInstanceOf(
            UserRegistrationPendingApprovalException.class
        );

        verify(userRepository, never()).update(any());
    }

    @Test
    void should_reject_the_finalization_of_a_rejected_registration() throws TechnicalException {
        when(
            parameterService.findAsBoolean(PORTAL_CONTEXT, Key.PORTAL_USERCREATION_ENABLED, ENVIRONMENT, ParameterReferenceType.ENVIRONMENT)
        ).thenReturn(Boolean.TRUE);
        when(passwordValidator.validate(any())).thenReturn(true);
        User rejectedUser = pendingUser(null);
        rejectedUser.setStatus(UserStatus.REJECTED);
        when(userRepository.findById(USER_ID)).thenReturn(of(rejectedUser));

        RegisterUserEntity registerUserEntity = new RegisterUserEntity();
        registerUserEntity.setToken(registrationToken());
        registerUserEntity.setPassword("gh2gyf8!zjfnz");

        assertThatThrownBy(() -> userService.finalizeRegistration(PORTAL_CONTEXT, registerUserEntity)).isInstanceOf(
            UserStateConflictException.class
        );

        verify(userRepository, never()).update(any());
    }

    @Test
    void should_link_the_registration_email_to_the_portal_of_the_environment_the_user_registered_on() throws TechnicalException {
        givenPendingUser(null);
        when(
            membershipService.getReferenceIdsByMemberAndReference(MembershipMemberType.USER, USER_ID, MembershipReferenceType.ENVIRONMENT)
        ).thenReturn(Set.of(OTHER_ENVIRONMENT));
        when(installationAccessQueryService.getPortalUrl(OTHER_ENVIRONMENT)).thenReturn(OTHER_PORTAL_URL);

        userService.processRegistration(CONSOLE_CONTEXT, USER_ID, true);

        EmailNotification registrationEmail = capturedEmails()
            .stream()
            .filter(this::isRegistrationEmail)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no registration email has been sent"));

        assertThat((String) registrationEmail.getParams().get(PARAM_REGISTRATION_URL)).startsWith(
            OTHER_PORTAL_URL + "/user/registration/confirm/"
        );
    }

    @Test
    void should_link_the_registration_email_to_the_console_when_no_portal_url_is_configured() throws TechnicalException {
        givenPendingUser(null);
        when(installationAccessQueryService.getPortalUrl(ENVIRONMENT)).thenReturn(InstallationAccessQueryService.DEFAULT_PORTAL_URL);
        when(installationAccessQueryService.getConsoleUrl(ORGANIZATION)).thenReturn(CONSOLE_URL);

        userService.processRegistration(CONSOLE_CONTEXT, USER_ID, true);

        EmailNotification registrationEmail = capturedEmails()
            .stream()
            .filter(this::isRegistrationEmail)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no registration email has been sent"));

        assertThat((String) registrationEmail.getParams().get(PARAM_REGISTRATION_URL)).startsWith(CONSOLE_URL + REGISTRATION_PATH);
    }

    private void givenUserRegistrationEnabled(boolean automaticValidation) throws TechnicalException {
        when(
            parameterService.findAsBoolean(PORTAL_CONTEXT, Key.PORTAL_USERCREATION_ENABLED, ENVIRONMENT, ParameterReferenceType.ENVIRONMENT)
        ).thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                PORTAL_CONTEXT,
                Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED,
                ENVIRONMENT,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(automaticValidation);
        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());
        when(userRepository.findBySource("gravitee", EMAIL, ORGANIZATION)).thenReturn(Optional.empty());
        when(userRepository.create(any(User.class))).thenAnswer(returnsFirstArg());
        when(roleService.findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT)).thenReturn(
            List.of(RoleEntity.builder().scope(RoleScope.ORGANIZATION).name("USER").build())
        );
    }

    private void givenPendingUser(String password) throws TechnicalException {
        User user = pendingUser(password);
        when(userRepository.findById(USER_ID)).thenReturn(of(user));
        when(userRepository.update(any(User.class))).thenAnswer(returnsFirstArg());
    }

    private User pendingUser(String password) {
        User user = new User();
        user.setId(USER_ID);
        user.setOrganizationId(ORGANIZATION);
        user.setSource("gravitee");
        user.setSourceId(EMAIL);
        user.setEmail(EMAIL);
        user.setFirstname("Joe");
        user.setLastname("Bar");
        user.setPassword(password);
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(user.getCreatedAt());
        return user;
    }

    private NewExternalUserEntity newExternalUser() {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setEmail(EMAIL);
        newExternalUserEntity.setFirstname("Joe");
        newExternalUserEntity.setLastname("Bar");
        return newExternalUserEntity;
    }

    private String registrationToken() {
        return JWT.create()
            .withIssuer(DEFAULT_JWT_ISSUER)
            .withSubject(USER_ID)
            .withClaim(JWTHelper.Claims.EMAIL, EMAIL)
            .withClaim(JWTHelper.Claims.ACTION, USER_REGISTRATION.name())
            .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
            .sign(Algorithm.HMAC256(JWT_SECRET));
    }

    private boolean isRequestProcessedEmail(EmailNotification email) {
        return EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_REGISTRATION_REQUEST_PROCESSED.getLinkedHook()
            .getTemplate()
            .equals(email.getTemplate());
    }

    private boolean isRegistrationEmail(EmailNotification email) {
        return EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_REGISTRATION.getLinkedHook()
            .getTemplate()
            .equals(email.getTemplate());
    }

    private List<EmailNotification> capturedEmails() {
        ArgumentCaptor<EmailNotification> captor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailService, atLeastOnce()).sendAsyncEmailNotification(any(), captor.capture());
        return captor.getAllValues();
    }
}
