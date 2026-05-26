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
package io.gravitee.apim.infra.domain_service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserRegistrationUnavailableException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserRegistrationEnabledServiceImplTest {

    ParameterService parameterService = mock(ParameterService.class);
    UserRegistrationEnabledServiceImpl service = new UserRegistrationEnabledServiceImpl(parameterService);

    @Nested
    class CheckEnabled {

        @Test
        void should_not_throw_when_portal_registration_is_enabled() {
            var executionContext = new ExecutionContext("org-id", "env-id");
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    "env-id",
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);

            var throwable = catchThrowable(() -> service.checkEnabled(executionContext));

            assertThat(throwable).isNull();
        }

        @Test
        void should_throw_when_portal_registration_is_disabled() {
            var executionContext = new ExecutionContext("org-id", "env-id");
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    "env-id",
                    ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(false);

            var throwable = catchThrowable(() -> service.checkEnabled(executionContext));

            assertThat(throwable).isInstanceOf(UserRegistrationUnavailableException.class);
        }

        @Test
        void should_not_throw_when_console_registration_is_enabled() {
            var executionContext = new ExecutionContext("org-id");
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.CONSOLE_USERCREATION_ENABLED,
                    "org-id",
                    ParameterReferenceType.ORGANIZATION
                )
            ).thenReturn(true);

            var throwable = catchThrowable(() -> service.checkEnabled(executionContext));

            assertThat(throwable).isNull();
        }

        @Test
        void should_throw_when_console_registration_is_disabled() {
            var executionContext = new ExecutionContext("org-id");
            when(
                parameterService.findAsBoolean(
                    executionContext,
                    Key.CONSOLE_USERCREATION_ENABLED,
                    "org-id",
                    ParameterReferenceType.ORGANIZATION
                )
            ).thenReturn(false);

            var throwable = catchThrowable(() -> service.checkEnabled(executionContext));

            assertThat(throwable).isInstanceOf(UserRegistrationUnavailableException.class);
        }
    }
}
