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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Which registration targets are refused, mirroring {@link UserServiceResetPasswordTargetTest}.
 *
 * <p>Only the rejections live here: they are the cases that never reach registration, so no fixture
 * beyond the installation lookup is needed to state them. The accepted {@code gamma} target is
 * covered end-to-end in {@code UserServiceRegistrationApprovalTest}, on the emitted email.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceRegistrationTargetTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");

    private UserServiceImpl userService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @BeforeEach
    void setUp() {
        // The holder is a thread-local other test classes in the same fork leave a principal in,
        // which would route this unauthenticated path through the permission check instead.
        SecurityContextHolder.clearContext();
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "installationAccessQueryService", installationAccessQueryService);
    }

    @Test
    void should_reject_an_unknown_registration_target() {
        assertThatThrownBy(() -> userService.registerWithTarget(EXECUTION_CONTEXT, new NewExternalUserEntity(), "portal"))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("portal");
    }

    @Test
    void should_reject_a_url_shaped_registration_target() {
        // The endpoint is anonymous and the link carries a signed token, so a caller-supplied
        // destination would exfiltrate it. Only the fixed keyword is accepted.
        assertThatThrownBy(() ->
            userService.registerWithTarget(EXECUTION_CONTEXT, new NewExternalUserEntity(), "https://attacker.example.com")
        ).isInstanceOf(ValidationDomainException.class);

        verify(installationAccessQueryService, never()).getGammaUrl(any());
    }
}
