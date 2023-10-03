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
package io.gravitee.rest.api.management.v2.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
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

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class GraviteeCorsConfigurationTest {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final ParameterReferenceType ORGANIZATION_TYPE = ParameterReferenceType.ORGANIZATION;

    @Mock
    private ParameterService parameterService;

    @Mock
    private AccessPointQueryService accessPointQueryService;

    @Mock
    private EventManager eventManager;

    private GraviteeCorsConfiguration graviteeCorsConfiguration;

    @BeforeEach
    public void beforeEach() {
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);

        lenient()
            .when(parameterService.find(eq(GraviteeContext.getExecutionContext()), any(), eq(ORGANIZATION_ID), eq(ORGANIZATION_TYPE)))
            .thenReturn(null);
        lenient()
            .when(
                parameterService.find(
                    GraviteeContext.getExecutionContext(),
                    Key.CONSOLE_HTTP_CORS_MAX_AGE,
                    ORGANIZATION_ID,
                    ORGANIZATION_TYPE
                )
            )
            .thenReturn("10");
    }

    @Test
    void should_construct_and_initialize_fields() {
        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, accessPointQueryService, eventManager, ORGANIZATION_ID);

        verify(eventManager, times(1)).subscribeForEvents(graviteeCorsConfiguration, Key.class);

        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN, ORGANIZATION_ID, ORGANIZATION_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS, ORGANIZATION_ID, ORGANIZATION_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.CONSOLE_HTTP_CORS_ALLOW_METHODS, ORGANIZATION_ID, ORGANIZATION_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS, ORGANIZATION_ID, ORGANIZATION_TYPE);
        verify(parameterService, times(1))
            .find(GraviteeContext.getExecutionContext(), Key.CONSOLE_HTTP_CORS_MAX_AGE, ORGANIZATION_ID, ORGANIZATION_TYPE);

        assertThat(graviteeCorsConfiguration.getAllowedMethods()).isNotNull();
        assertThat(graviteeCorsConfiguration.getAllowedMethods()).hasSize(1);
    }

    @Test
    void should_set_fields_on_event() {
        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, accessPointQueryService, eventManager, ORGANIZATION_ID);

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS, buildParameter("header1;header2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_METHODS, buildParameter("method1;method2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS, buildParameter("exposed1")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12")));

        assertThat(graviteeCorsConfiguration.getAllowedOriginPatterns()).containsOnly("origin1", "origin2");
        assertThat(graviteeCorsConfiguration.getAllowedHeaders()).containsOnly("header1", "header2");
        assertThat(graviteeCorsConfiguration.getAllowedMethods()).containsOnly("method1", "method2");
        assertThat(graviteeCorsConfiguration.getExposedHeaders()).containsOnly("exposed1");
        assertThat(graviteeCorsConfiguration.getMaxAge()).isEqualTo(12L);
    }

    @Test
    void should_not_set_fields_on_event_with_wrong_env_id() {
        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, accessPointQueryService, eventManager, ORGANIZATION_ID);

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12", "ANOTHER_ORG")));

        assertThat(graviteeCorsConfiguration.getMaxAge()).isEqualTo(10L);
    }

    @Test
    void should_set_fields_on_event_with_access_point() {
        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, accessPointQueryService, eventManager, ORGANIZATION_ID);
        when(accessPointQueryService.getConsoleUrls(ORGANIZATION_ID, true)).thenReturn(List.of("custom-console-url"));

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        assertEquals(Arrays.asList("origin1", "origin2", "custom-console-url"), graviteeCorsConfiguration.getAllowedOriginPatterns());
    }

    private Parameter buildParameter(String value) {
        return buildParameter(value, ORGANIZATION_ID);
    }

    private Parameter buildParameter(String value, String referenceId) {
        Parameter parameter = new Parameter();
        parameter.setValue(value);
        parameter.setReferenceId(referenceId);
        return parameter;
    }
}
