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
package io.gravitee.rest.api.portal.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GraviteeCorsConfigurationTest {

    private static final String ENVIRONMENT_ID = "environmentId";
    private static final ParameterReferenceType ENVIRONMENT_TYPE = ParameterReferenceType.ENVIRONMENT;

    @Mock
    private ParameterService parameterService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @Mock
    private EventManager eventManager;

    private GraviteeCorsConfiguration graviteeCorsConfiguration;

    @BeforeEach
    public void beforeEach() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        lenient()
            .when(parameterService.find(eq(GraviteeContext.getExecutionContext()), any(), eq(ENVIRONMENT_ID), eq(ENVIRONMENT_TYPE)))
            .thenReturn(null);
        lenient()
            .when(
                parameterService.find(GraviteeContext.getExecutionContext(), Key.PORTAL_HTTP_CORS_MAX_AGE, ENVIRONMENT_ID, ENVIRONMENT_TYPE)
            )
            .thenReturn("10");
    }

    @Test
    void should_construct_and_initialize_fields() {
        graviteeCorsConfiguration =
            new GraviteeCorsConfiguration(parameterService, installationAccessQueryService, eventManager, ENVIRONMENT_ID);

        verify(eventManager, times(1)).subscribeForEvents(graviteeCorsConfiguration, Key.class);

        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, ENVIRONMENT_ID, ENVIRONMENT_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.PORTAL_HTTP_CORS_ALLOW_HEADERS, ENVIRONMENT_ID, ENVIRONMENT_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.PORTAL_HTTP_CORS_ALLOW_METHODS, ENVIRONMENT_ID, ENVIRONMENT_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS, ENVIRONMENT_ID, ENVIRONMENT_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.PORTAL_HTTP_CORS_MAX_AGE, ENVIRONMENT_ID, ENVIRONMENT_TYPE);

        assertThat(graviteeCorsConfiguration.getAllowedMethods()).isNotNull();
        assertThat(graviteeCorsConfiguration.getAllowedMethods()).hasSize(1);
    }

    @Test
    void should_set_fields_on_event() {
        graviteeCorsConfiguration =
            new GraviteeCorsConfiguration(parameterService, installationAccessQueryService, eventManager, ENVIRONMENT_ID);

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_HEADERS, buildParameter("header1;header2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_METHODS, buildParameter("method1;method2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS, buildParameter("exposed1")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_MAX_AGE, buildParameter("12")));

        assertThat(graviteeCorsConfiguration.getAllowedOriginPatterns()).containsOnly("origin1", "origin2");
        assertThat(graviteeCorsConfiguration.getAllowedHeaders()).containsOnly("header1", "header2");
        assertThat(graviteeCorsConfiguration.getAllowedMethods()).containsOnly("method1", "method2");
        assertThat(graviteeCorsConfiguration.getExposedHeaders()).containsOnly("exposed1");
        assertThat(graviteeCorsConfiguration.getMaxAge()).isEqualTo(12L);
    }

    @Test
    void should_not_set_fields_on_event_with_wrong_env_id() {
        graviteeCorsConfiguration =
            new GraviteeCorsConfiguration(parameterService, installationAccessQueryService, eventManager, ENVIRONMENT_ID);

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12", "ANOTHER_ORG")));

        assertThat(graviteeCorsConfiguration.getMaxAge()).isEqualTo(10L);
    }

    @Test
    void should_set_fields_on_event_with_access_point() {
        graviteeCorsConfiguration =
            new GraviteeCorsConfiguration(parameterService, installationAccessQueryService, eventManager, ENVIRONMENT_ID);
        when(installationAccessQueryService.getPortalUrls(ENVIRONMENT_ID)).thenReturn(List.of("custom-portal-url"));

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        assertEquals(Arrays.asList("origin1", "origin2", "custom-portal-url"), graviteeCorsConfiguration.getAllowedOriginPatterns());
    }

    private Parameter buildParameter(String value) {
        return buildParameter(value, ENVIRONMENT_ID);
    }

    private Parameter buildParameter(String value, String referenceId) {
        Parameter parameter = new Parameter();
        parameter.setValue(value);
        parameter.setReferenceId(referenceId);
        return parameter;
    }
}
