/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.security.config;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GraviteeCorsConfigurationTest {

    private final static String REFERENCE_ID = "DEFAULT";
    private final static ParameterReferenceType REFERENCE_TYPE = ParameterReferenceType.ORGANIZATION;

    @Mock
    private ParameterService parameterService;
    @Mock
    private EventManager eventManager;
    private GraviteeCorsConfiguration graviteeCorsConfiguration;

    @Before
    public void setUp() {
        reset(parameterService, eventManager);

        when(parameterService.find(Key.CONSOLE_HTTP_CORS_MAX_AGE, REFERENCE_ID, REFERENCE_TYPE)).thenReturn("10");
    }

    @Test
    public void shouldConstructAndInitializeFields() {

        when(parameterService.find(Key.CONSOLE_HTTP_CORS_ALLOW_METHODS, REFERENCE_ID, REFERENCE_TYPE)).thenReturn(null);

        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, eventManager, REFERENCE_ID);

        verify(eventManager, times(1)).subscribeForEvents(graviteeCorsConfiguration, Key.class);

        verify(parameterService, times(1)).find(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN, REFERENCE_ID, REFERENCE_TYPE);
        verify(parameterService, times(1)).find(Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS, REFERENCE_ID, REFERENCE_TYPE);
        verify(parameterService, times(1)).find(Key.CONSOLE_HTTP_CORS_ALLOW_METHODS, REFERENCE_ID, REFERENCE_TYPE);
        verify(parameterService, times(1)).find(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS, REFERENCE_ID, REFERENCE_TYPE);
        verify(parameterService, times(1)).find(Key.CONSOLE_HTTP_CORS_MAX_AGE, REFERENCE_ID, REFERENCE_TYPE);

        assertNotNull(graviteeCorsConfiguration.getAllowedMethods());
        assertEquals(1, graviteeCorsConfiguration.getAllowedMethods().size());
    }

    @Test
    public void shouldSetFieldsOnEvent() {
        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, eventManager, REFERENCE_ID);

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN, buildParameter("origin1;origin2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS, buildParameter("header1;header2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_ALLOW_METHODS, buildParameter("method1;method2")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS, buildParameter("exposed1")));
        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12")));

        assertEquals(Arrays.asList("origin1", "origin2"), graviteeCorsConfiguration.getAllowedOrigins());
        assertEquals(Arrays.asList("header1", "header2"), graviteeCorsConfiguration.getAllowedHeaders());
        assertEquals(Arrays.asList("method1", "method2"), graviteeCorsConfiguration.getAllowedMethods());
        assertEquals(singletonList("exposed1"), graviteeCorsConfiguration.getExposedHeaders());
        assertEquals(Long.valueOf(12L), graviteeCorsConfiguration.getMaxAge());
    }

    @Test
    public void shouldNotSetFieldsOnEventWithWrongOrgId() {
        graviteeCorsConfiguration = new GraviteeCorsConfiguration(parameterService, eventManager, REFERENCE_ID);

        graviteeCorsConfiguration.onEvent(new SimpleEvent<>(Key.CONSOLE_HTTP_CORS_MAX_AGE, buildParameter("12", "ANOTHER_ORG")));

        assertEquals(Long.valueOf(10L), graviteeCorsConfiguration.getMaxAge());
    }

    private Parameter buildParameter(String value) {
        return buildParameter(value, REFERENCE_ID);
    }

    private Parameter buildParameter(String value, String referenceId) {
        Parameter parameter = new Parameter();
        parameter.setValue(value);
        parameter.setReferenceId(referenceId);
        return parameter;
    }
}