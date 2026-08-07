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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceResetPasswordTargetTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");

    private UserServiceImpl userService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "installationAccessQueryService", installationAccessQueryService);
    }

    @Test
    void should_build_gamma_reset_password_page_url_from_installation_config() {
        when(installationAccessQueryService.getGammaUrl("DEFAULT")).thenReturn("http://gamma.example.com/");

        String resetPageUrl = ReflectionTestUtils.invokeMethod(userService, "buildGammaResetPasswordPageUrl", "DEFAULT");

        assertThat(resetPageUrl).isEqualTo("http://gamma.example.com/reset-password");
    }

    @Test
    void should_reject_unknown_reset_target() {
        assertThatThrownBy(() -> userService.resetPasswordWithTarget(EXECUTION_CONTEXT, "user-id", "portal"))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("portal");
    }
}
