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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.quality.ApiQualityMetricDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiQualityMetricDescriptionTest {
    @InjectMocks
    private ApiQualityMetricDescription srv = new ApiQualityMetricDescription();

    @Mock
    private ParameterService parameterService;

    @Test
    public void shouldNotBeValidWithNull() {
        when(parameterService.findAll(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH, ParameterReferenceType.ENVIRONMENT)).thenReturn(Collections.emptyList());
        ApiEntity api = mock(ApiEntity.class);
        when(api.getDescription()).thenReturn(null);

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    @Test
    public void shouldNotBeValidWithNotEnoughDefaultLength() {
        when(parameterService.findAll(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH, ParameterReferenceType.ENVIRONMENT)).thenReturn(Collections.emptyList());
        ApiEntity api = mock(ApiEntity.class);
        when(api.getDescription()).thenReturn("1234567890");

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }

    @Test
    public void shouldBeValidWithEnoughDefaultLength() {
        when(parameterService.findAll(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH, ParameterReferenceType.ENVIRONMENT)).thenReturn(Collections.emptyList());
        ApiEntity api = mock(ApiEntity.class);
        when(api.getDescription()).thenReturn("1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890");

        boolean valid = srv.isValid(api);

        assertTrue(valid);
    }

    @Test
    public void shouldBeValidWithEnoughCustomLength() {
        when(parameterService.findAll(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH, ParameterReferenceType.ENVIRONMENT)).thenReturn(Arrays.asList("3"));
        ApiEntity api = mock(ApiEntity.class);
        when(api.getDescription()).thenReturn("123");

        boolean valid = srv.isValid(api);

        assertTrue(valid);
    }

    @Test
    public void shouldNotBeValidWithNotEnoughCustomLength() {
        when(parameterService.findAll(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH, ParameterReferenceType.ENVIRONMENT)).thenReturn(Arrays.asList("3"));
        ApiEntity api = mock(ApiEntity.class);
        when(api.getDescription()).thenReturn("12");

        boolean valid = srv.isValid(api);

        assertFalse(valid);
    }
}
