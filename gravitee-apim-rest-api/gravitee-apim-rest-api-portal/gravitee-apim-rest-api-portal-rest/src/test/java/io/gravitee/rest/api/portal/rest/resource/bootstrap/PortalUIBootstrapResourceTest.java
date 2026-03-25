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
package io.gravitee.rest.api.portal.rest.resource.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class PortalUIBootstrapResourceTest {

    @Mock
    private ParameterService parameterService;

    @Mock
    private Environment springEnvironment;

    private PortalUIBootstrapResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new PortalUIBootstrapResource();
        Field ps = PortalUIBootstrapResource.class.getDeclaredField("parameterService");
        ps.setAccessible(true);
        ps.set(resource, parameterService);
        Field env = PortalUIBootstrapResource.class.getDeclaredField("environment");
        env.setAccessible(true);
        env.set(resource, springEnvironment);
    }

    @Test
    void should_return_classic_when_portal_next_disabled() {
        when(
            parameterService.findAsBoolean(any(ExecutionContext.class), eq(Key.PORTAL_NEXT_ACCESS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        )
            .thenReturn(false);

        assertThat(resource.resolveDefaultPortal("org", "env")).isEqualTo("classic");
    }

    @Test
    void should_return_next_when_default_as_base_url_enabled() {
        when(
            parameterService.findAsBoolean(any(ExecutionContext.class), eq(Key.PORTAL_NEXT_ACCESS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        )
            .thenReturn(true);
        when(
            parameterService.findAsBoolean(
                any(ExecutionContext.class),
                eq(Key.PORTAL_NEXT_DEFAULT_AS_BASE_URL),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(true);

        assertThat(resource.resolveDefaultPortal("org", "env")).isEqualTo("next");
    }

    @Test
    void should_use_installation_default_when_db_toggle_off() {
        when(
            parameterService.findAsBoolean(any(ExecutionContext.class), eq(Key.PORTAL_NEXT_ACCESS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        )
            .thenReturn(true);
        when(
            parameterService.findAsBoolean(
                any(ExecutionContext.class),
                eq(Key.PORTAL_NEXT_DEFAULT_AS_BASE_URL),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(false);
        when(springEnvironment.getProperty("DEFAULT_PORTAL", "classic")).thenReturn("next");
        when(springEnvironment.getProperty("portal.ui.defaultPortal", "next")).thenAnswer(invocation -> invocation.getArgument(1));

        assertThat(resource.resolveDefaultPortal("org", "env")).isEqualTo("next");
    }

    @Test
    void should_fallback_to_classic_when_installation_not_next() {
        when(
            parameterService.findAsBoolean(any(ExecutionContext.class), eq(Key.PORTAL_NEXT_ACCESS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        )
            .thenReturn(true);
        when(
            parameterService.findAsBoolean(
                any(ExecutionContext.class),
                eq(Key.PORTAL_NEXT_DEFAULT_AS_BASE_URL),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(false);
        when(springEnvironment.getProperty("DEFAULT_PORTAL", "classic")).thenReturn("classic");
        when(springEnvironment.getProperty("portal.ui.defaultPortal", "classic")).thenAnswer(invocation -> invocation.getArgument(1));

        assertThat(resource.resolveDefaultPortal("org", "env")).isEqualTo("classic");
    }
}
