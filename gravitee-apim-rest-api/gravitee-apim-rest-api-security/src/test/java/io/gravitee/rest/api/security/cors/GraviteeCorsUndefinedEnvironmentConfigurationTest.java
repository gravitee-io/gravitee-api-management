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
package io.gravitee.rest.api.security.cors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */

@ExtendWith(MockitoExtension.class)
public class GraviteeCorsUndefinedEnvironmentConfigurationTest {

    private static final ParameterReferenceType ENVIRONMENT_TYPE = ParameterReferenceType.ENVIRONMENT;

    @Mock
    private Environment environment;

    @Mock
    private ParameterService parameterService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    private EventManager eventManager;

    private GraviteeCorsConfiguration cut;

    @BeforeEach
    public void beforeEach() {
        GraviteeContext.fromExecutionContext(new ExecutionContext());

        lenient()
            .when(environment.getProperty(any(), eq(String.class), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(2));

        eventManager = new EventManagerImpl();
        cut = new GraviteeCorsConfiguration(
            environment,
            parameterService,
            installationAccessQueryService,
            eventManager,
            GraviteeCorsConfiguration.UNDEFINED_REFERENCE_ID,
            ENVIRONMENT_TYPE
        );
    }

    @AfterEach
    public void afterEach() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_initialize_fields_from_default_value() {
        verify(environment, times(1)).getProperty(
            Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN.key(),
            String.class,
            Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN.defaultValue()
        );
        verify(environment, times(1)).getProperty(
            Key.PORTAL_HTTP_CORS_ALLOW_HEADERS.key(),
            String.class,
            Key.PORTAL_HTTP_CORS_ALLOW_HEADERS.defaultValue()
        );
        verify(environment, times(1)).getProperty(
            Key.PORTAL_HTTP_CORS_ALLOW_METHODS.key(),
            String.class,
            Key.PORTAL_HTTP_CORS_ALLOW_METHODS.defaultValue()
        );
        verify(environment, times(1)).getProperty(
            Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS.key(),
            String.class,
            Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS.defaultValue()
        );
        verify(environment, times(1)).getProperty(
            Key.PORTAL_HTTP_CORS_MAX_AGE.key(),
            String.class,
            Key.PORTAL_HTTP_CORS_MAX_AGE.defaultValue()
        );

        assertThat(cut.getAllowedOriginPatterns()).containsOnly("*");
        assertThat(cut.getAllowedHeaders()).containsOnly(
            "Cache-Control",
            "Pragma",
            "Origin",
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "If-Match",
            "X-Xsrf-Token",
            "X-Recaptcha-Token"
        );
        assertThat(cut.getAllowedMethods()).containsOnly("OPTIONS", "GET", "POST", "PUT", "DELETE", "PATCH");
        assertThat(cut.getExposedHeaders()).containsOnly("ETag", "X-Xsrf-Token");
        assertThat(cut.getMaxAge()).isEqualTo(1728000L);
    }

    @Test
    void should_initialize_fields_from_default_value_and_installation() {
        when(installationAccessQueryService.getPortalUrls()).thenReturn(List.of("custom-portal-url"));
        cut = new GraviteeCorsConfiguration(
            environment,
            parameterService,
            installationAccessQueryService,
            eventManager,
            GraviteeCorsConfiguration.UNDEFINED_REFERENCE_ID,
            ENVIRONMENT_TYPE
        );

        assertEquals(Arrays.asList("*", "custom-portal-url"), cut.getAllowedOriginPatterns());
    }

    @Test
    void should_not_set_fields_on_event_with_wrong_env_id() {
        eventManager.publishEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12", "ANOTHER_ENV")));
        assertThat(cut.getMaxAge()).isEqualTo(1728000L);
    }

    private Parameter buildParameter(String value, String referenceId) {
        Parameter parameter = new Parameter();
        parameter.setValue(value);
        parameter.setReferenceId(referenceId);
        return parameter;
    }
}
