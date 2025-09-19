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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.model.AccessPointEvent;
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
import org.junit.After;
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
public class GraviteeCorsEnvironmentConfigurationTest {

    private static final String ENVIRONMENT_ID = "environmentId";
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
        GraviteeContext.fromExecutionContext(new ExecutionContext(null, ENVIRONMENT_ID));

        lenient()
            .when(parameterService.find(eq(GraviteeContext.getExecutionContext()), any(), eq(ENVIRONMENT_ID), eq(ENVIRONMENT_TYPE)))
            .thenReturn(null);
        eventManager = new EventManagerImpl();
        cut = new GraviteeCorsConfiguration(
            environment,
            parameterService,
            installationAccessQueryService,
            eventManager,
            ENVIRONMENT_ID,
            ENVIRONMENT_TYPE
        );
    }

    @AfterEach
    public void afterEach() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_initialize_fields_from_default_value() {
        verify(parameterService, times(1)).find(
            GraviteeContext.getExecutionContext(),
            Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN,
            ENVIRONMENT_ID,
            ENVIRONMENT_TYPE
        );
        verify(parameterService, times(1)).find(
            GraviteeContext.getExecutionContext(),
            Key.PORTAL_HTTP_CORS_ALLOW_HEADERS,
            ENVIRONMENT_ID,
            ENVIRONMENT_TYPE
        );
        verify(parameterService, times(1)).find(
            GraviteeContext.getExecutionContext(),
            Key.PORTAL_HTTP_CORS_ALLOW_METHODS,
            ENVIRONMENT_ID,
            ENVIRONMENT_TYPE
        );
        verify(parameterService, times(1)).find(
            GraviteeContext.getExecutionContext(),
            Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS,
            ENVIRONMENT_ID,
            ENVIRONMENT_TYPE
        );
        verify(parameterService, times(1)).find(
            GraviteeContext.getExecutionContext(),
            Key.PORTAL_HTTP_CORS_MAX_AGE,
            ENVIRONMENT_ID,
            ENVIRONMENT_TYPE
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
    void should_set_fields_on_parameter_event() {
        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_HEADERS, buildParameter("header1;header2")));
        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_METHODS, buildParameter("method1;method2")));
        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS, buildParameter("exposed1")));
        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_MAX_AGE, buildParameter("12")));

        assertThat(cut.getAllowedOriginPatterns()).containsOnly("origin1", "origin2");
        assertThat(cut.getAllowedHeaders()).containsOnly("header1", "header2");
        assertThat(cut.getAllowedMethods()).containsOnly("method1", "method2");
        assertThat(cut.getExposedHeaders()).containsOnly("exposed1");
        assertThat(cut.getMaxAge()).isEqualTo(12L);
    }

    @Test
    void should_not_set_fields_on_parameter_event_with_wrong_env_id() {
        eventManager.publishEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12", "ANOTHER_ORG")));
        assertThat(cut.getMaxAge()).isEqualTo(1728000L);
    }

    @Test
    void should_set_fields_on_parameter_event_from_installation() {
        when(installationAccessQueryService.getPortalUrls(ENVIRONMENT_ID)).thenReturn(List.of("custom-portal-url"));

        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        assertEquals(Arrays.asList("origin1", "origin2", "custom-portal-url"), cut.getAllowedOriginPatterns());
        verify(installationAccessQueryService, never()).getConsoleUrls(any());
    }

    @Test
    void should_set_fields_on_parameter_event_from_installation_and_organization() {
        GraviteeContext.fromExecutionContext(new ExecutionContext("organizationId", ENVIRONMENT_ID));

        when(installationAccessQueryService.getPortalUrls(ENVIRONMENT_ID)).thenReturn(List.of("custom-portal-url"));
        when(installationAccessQueryService.getConsoleUrls("organizationId")).thenReturn(List.of("custom-console-url"));

        eventManager.publishEvent(new SimpleEvent<>(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        assertEquals(Arrays.asList("origin1", "origin2", "custom-portal-url", "custom-console-url"), cut.getAllowedOriginPatterns());
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

    @Test
    void should_set_allowed_origin_on_access_points_event() {
        eventManager.publishEvent(
            new SimpleEvent<>(
                AccessPointEvent.CREATED,
                AccessPoint.builder()
                    .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
                    .referenceId(ENVIRONMENT_ID)
                    .host("origin1")
                    .target(AccessPoint.Target.PORTAL)
                    .build()
            )
        );

        assertThat(cut.getAllowedOriginPatterns()).containsOnly("*", "http://origin1");
    }

    @Test
    void should_not_set_allowed_origin_on_access_points_event_with_target() {
        eventManager.publishEvent(
            new SimpleEvent<>(
                AccessPointEvent.CREATED,
                AccessPoint.builder()
                    .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
                    .referenceId(ENVIRONMENT_ID)
                    .host("origin1")
                    .target(AccessPoint.Target.GATEWAY)
                    .build()
            )
        );

        assertThat(cut.getAllowedOriginPatterns()).containsOnly("*");
    }

    @Test
    void should_not_set_allowed_origin_on_access_point_event_with_wrong_env_id() {
        eventManager.publishEvent(
            new SimpleEvent<>(
                AccessPointEvent.CREATED,
                AccessPoint.builder()
                    .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
                    .referenceId("ANOTHER_ENV")
                    .host("origin1")
                    .target(AccessPoint.Target.PORTAL)
                    .build()
            )
        );
        assertThat(cut.getAllowedOriginPatterns()).containsOnly("*");
    }
}
