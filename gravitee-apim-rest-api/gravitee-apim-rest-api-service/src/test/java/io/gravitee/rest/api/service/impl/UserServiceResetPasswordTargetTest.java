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

import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.PARAM_REGISTRATION_URL;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceResetPasswordTargetTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");
    private static final String JWT_SECRET = "test-jwt-secret-key-min-32-characters";

    private UserServiceImpl userService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserConverter userConverter;

    @Mock
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "installationAccessQueryService", installationAccessQueryService);
        ReflectionTestUtils.setField(userService, "environment", environment);
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
        ReflectionTestUtils.setField(userService, "auditService", auditService);
        ReflectionTestUtils.setField(userService, "notifierService", notifierService);
        ReflectionTestUtils.setField(userService, "emailService", emailService);
        ReflectionTestUtils.setField(userService, "userConverter", userConverter);
    }

    @Test
    void should_build_gamma_reset_password_page_url_from_installation_config() {
        when(installationAccessQueryService.getGammaUrl("DEFAULT")).thenReturn("http://gamma.example.com/");

        String resetPageUrl = ReflectionTestUtils.invokeMethod(userService, "buildGammaResetPasswordPageUrl", "DEFAULT");

        assertThat(resetPageUrl).isEqualTo("http://gamma.example.com/reset-password");
    }

    @Test
    void should_reject_reset_when_gamma_url_is_not_configured() {
        when(installationAccessQueryService.getGammaUrl("DEFAULT")).thenReturn(null);

        assertThatThrownBy(() -> userService.resetPasswordWithTarget(EXECUTION_CONTEXT, "user-id", "gamma"))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Gamma URL is not configured");
    }

    @Test
    void should_reject_unknown_reset_target() {
        assertThatThrownBy(() -> userService.resetPasswordWithTarget(EXECUTION_CONTEXT, "user-id", "portal"))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("portal");
    }

    @Test
    void should_reset_password_with_gamma_target_and_build_registration_url() throws TechnicalException {
        when(installationAccessQueryService.getGammaUrl("DEFAULT")).thenReturn("http://gamma.example.com");
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(
            environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER)
        ).thenReturn(3600);
        when(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER)).thenReturn(DEFAULT_JWT_ISSUER);
        when(user.getId()).thenReturn("user-1");
        when(user.getSource()).thenReturn("gravitee");
        when(user.getOrganizationId()).thenReturn("DEFAULT");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getIsServiceAccount()).thenReturn(false);
        when(userRepository.findById("user-1")).thenReturn(of(user));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user-1");
        userEntity.setEmail("user@example.com");
        when(userConverter.toUserEntity(eq(user), any())).thenReturn(userEntity);
        when(auditService.search(eq(EXECUTION_CONTEXT), any())).thenReturn(mock(MetadataPage.class));

        userService.resetPasswordWithTarget(EXECUTION_CONTEXT, "user-1", "gamma");

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notifierService).trigger(eq(EXECUTION_CONTEXT), eq(PortalHook.PASSWORD_RESET), paramsCaptor.capture());

        String registrationUrl = (String) paramsCaptor.getValue().get(PARAM_REGISTRATION_URL);
        assertThat(registrationUrl).startsWith("http://gamma.example.com/reset-password/");
        assertThat(registrationUrl.substring("http://gamma.example.com/reset-password/".length())).isNotBlank();
    }
}
