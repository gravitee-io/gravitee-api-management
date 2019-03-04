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
package io.gravitee.management.service;

import io.gravitee.management.model.ApiQualityMetricsEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.service.exceptions.ApiQualityMetricsDisableException;
import io.gravitee.management.service.impl.QualityMetricsServiceImpl;
import io.gravitee.management.service.quality.ApiQualityMetricLoader;
import io.gravitee.management.service.quality.ApiQualityMetricLogo;
import io.gravitee.management.service.quality.ApiQualityMetricViews;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class QualityMetricsService_GetApiMetricsTest {

    @InjectMocks
    private QualityMetricsService srv = new QualityMetricsServiceImpl();

    @Mock
    ParameterService parameterService;

    @Mock
    ApiQualityMetricLoader apiQualityMetricLoader;

    @Mock
    ApiQualityMetricLogo apiQualityMetricLogo;

    @Mock
    ApiQualityMetricViews apiQualityMetricViews;

    @Before
    public void setup() {
        when(apiQualityMetricLoader.getApiQualityMetrics()).thenReturn(Arrays.asList(apiQualityMetricLogo, apiQualityMetricViews));
        when(apiQualityMetricLogo.getWeightKey()).thenReturn(Key.API_QUALITY_METRICS_LOGO_WEIGHT);
        when(apiQualityMetricViews.getWeightKey()).thenReturn(Key.API_QUALITY_METRICS_VIEWS_WEIGHT);
    }

    @Test(expected = ApiQualityMetricsDisableException.class)
    public void shouldThrowExceptionIfDisabled() {
        when(parameterService.findAsBoolean(Key.API_QUALITY_METRICS_ENABLED)).thenReturn(Boolean.FALSE);
        ApiEntity api = mock(ApiEntity.class);

        srv.getMetrics(api);

        fail();
    }

    @Test
    public void shouldReturnEmptyEntityWithoutConfiguration() {
        when(parameterService.findAsBoolean(Key.API_QUALITY_METRICS_ENABLED)).thenReturn(Boolean.TRUE);
        when(parameterService.findAll(anyList(), any())).thenReturn(Collections.emptyMap());
        ApiEntity api = mock(ApiEntity.class);

        ApiQualityMetricsEntity metrics = srv.getMetrics(api);

        assertEquals(1, metrics.getScore(), 0);
        assertTrue(metrics.getMetricsPassed().isEmpty());
    }

    @Test
    public void shouldScore50Percent() {
        when(parameterService.findAsBoolean(Key.API_QUALITY_METRICS_ENABLED)).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_VIEWS_WEIGHT.key(), singletonList(1));
        when(parameterService.findAll(anyList(), any())).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(apiQualityMetricLogo.isValid(any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricViews.isValid(any())).thenReturn(Boolean.FALSE);

        ApiQualityMetricsEntity metrics = srv.getMetrics(api);

        assertEquals(0.5, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertFalse(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_VIEWS_WEIGHT.key()));
    }

    @Test
    public void shouldScore100Percent() {
        when(parameterService.findAsBoolean(Key.API_QUALITY_METRICS_ENABLED)).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_VIEWS_WEIGHT.key(), singletonList(1));
        when(parameterService.findAll(anyList(), any())).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(apiQualityMetricLogo.isValid(any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricViews.isValid(any())).thenReturn(Boolean.TRUE);

        ApiQualityMetricsEntity metrics = srv.getMetrics(api);

        assertEquals(1, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_VIEWS_WEIGHT.key()));
    }

    @Test
    public void shouldScore33Percent() {
        when(parameterService.findAsBoolean(Key.API_QUALITY_METRICS_ENABLED)).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_VIEWS_WEIGHT.key(), singletonList(2));
        when(parameterService.findAll(anyList(), any())).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(apiQualityMetricLogo.isValid(any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricViews.isValid(any())).thenReturn(Boolean.FALSE);

        ApiQualityMetricsEntity metrics = srv.getMetrics(api);

        assertEquals(0.33, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertFalse(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_VIEWS_WEIGHT.key()));
    }
}
