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

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.ApiQualityMetricsEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.quality.ApiQualityRuleEntity;
import io.gravitee.rest.api.model.quality.QualityRuleEntity;
import io.gravitee.rest.api.model.quality.QualityRuleReferenceType;
import io.gravitee.rest.api.service.ApiQualityRuleService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.QualityMetricsService;
import io.gravitee.rest.api.service.QualityRuleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiQualityMetricsDisableException;
import io.gravitee.rest.api.service.quality.ApiQualityMetricCategories;
import io.gravitee.rest.api.service.quality.ApiQualityMetricLoader;
import io.gravitee.rest.api.service.quality.ApiQualityMetricLogo;
import java.util.*;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class QualityMetricsServiceTest {

    @InjectMocks
    private QualityMetricsService srv = new QualityMetricsServiceImpl();

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiQualityMetricLoader apiQualityMetricLoader;

    @Mock
    private ApiQualityMetricLogo apiQualityMetricLogo;

    @Mock
    private ApiQualityMetricCategories apiQualityMetricCategories;

    @Mock
    private QualityRuleService qualityRuleService;

    @Mock
    private ApiQualityRuleService apiQualityRuleService;

    @Before
    public void setup() {
        when(apiQualityMetricLoader.getApiQualityMetrics()).thenReturn(Arrays.asList(apiQualityMetricLogo, apiQualityMetricCategories));
        when(apiQualityMetricLogo.getWeightKey()).thenReturn(Key.API_QUALITY_METRICS_LOGO_WEIGHT);
        when(apiQualityMetricCategories.getWeightKey()).thenReturn(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT);
    }

    @Test(expected = ApiQualityMetricsDisableException.class)
    public void shouldThrowExceptionIfDisabled() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.FALSE);
        ApiEntity api = mock(ApiEntity.class);

        srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        fail();
    }

    @Test
    public void shouldReturnEmptyEntityWithoutConfiguration() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.TRUE);
        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                anyList(),
                any(Function.class),
                any(ParameterReferenceType.class)
            )
        ).thenReturn(Collections.emptyMap());
        ApiEntity api = mock(ApiEntity.class);

        ApiQualityMetricsEntity metrics = srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        assertEquals(1, metrics.getScore(), 0);
        assertTrue(metrics.getMetricsPassed().isEmpty());
    }

    @Test
    public void shouldScore50Percent() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key(), singletonList(1));
        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                anyList(),
                any(Function.class),
                any(ParameterReferenceType.class)
            )
        ).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(apiQualityMetricLogo.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricCategories.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.FALSE);

        ApiQualityMetricsEntity metrics = srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        assertEquals(0.5, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertFalse(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key()));
    }

    @Test
    public void shouldScore100Percent() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key(), singletonList(1));
        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                anyList(),
                any(Function.class),
                any(ParameterReferenceType.class)
            )
        ).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(apiQualityMetricLogo.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricCategories.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);

        ApiQualityMetricsEntity metrics = srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        assertEquals(1, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key()));
    }

    @Test
    public void shouldScore33Percent() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key(), singletonList(2));
        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                anyList(),
                any(Function.class),
                any(ParameterReferenceType.class)
            )
        ).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(apiQualityMetricLogo.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricCategories.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.FALSE);

        ApiQualityMetricsEntity metrics = srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        assertEquals(0.33, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertFalse(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key()));
    }

    @Test
    public void shouldScore100PercentWithManualRules() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key(), singletonList(1));
        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                anyList(),
                any(Function.class),
                any(ParameterReferenceType.class)
            )
        ).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn("apiID");
        when(apiQualityMetricLogo.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricCategories.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);

        final QualityRuleEntity qualityRule = mock(QualityRuleEntity.class);
        when(qualityRule.getId()).thenReturn("1");
        when(qualityRule.getWeight()).thenReturn(1);
        when(qualityRuleService.findByReference(QualityRuleReferenceType.ENVIRONMENT, GraviteeContext.getCurrentEnvironment())).thenReturn(
            singletonList(qualityRule)
        );

        final ApiQualityRuleEntity apiQualityRule = mock(ApiQualityRuleEntity.class);
        when(apiQualityRule.getApi()).thenReturn("apiID");
        when(apiQualityRule.getQualityRule()).thenReturn("1");
        when(apiQualityRule.isChecked()).thenReturn(true);
        when(apiQualityRuleService.findByApi("apiID")).thenReturn(singletonList(apiQualityRule));

        ApiQualityMetricsEntity metrics = srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        assertEquals(1, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key()));
        assertTrue(metrics.getMetricsPassed().get("1"));
    }

    @Test
    public void shouldScore50PercentWithManualRules() {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_QUALITY_METRICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(Boolean.TRUE);
        Map<String, List<Object>> map = new HashMap<>();
        map.put(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key(), singletonList(1));
        map.put(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key(), singletonList(1));
        when(
            parameterService.findAll(
                eq(GraviteeContext.getExecutionContext()),
                anyList(),
                any(Function.class),
                any(ParameterReferenceType.class)
            )
        ).thenReturn(map);
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn("apiID");
        when(apiQualityMetricLogo.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);
        when(apiQualityMetricCategories.isValid(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(Boolean.TRUE);

        final QualityRuleEntity qualityRule = mock(QualityRuleEntity.class);
        when(qualityRule.getId()).thenReturn("1");
        when(qualityRule.getWeight()).thenReturn(2);
        when(qualityRuleService.findByReference(QualityRuleReferenceType.ENVIRONMENT, GraviteeContext.getCurrentEnvironment())).thenReturn(
            singletonList(qualityRule)
        );

        final ApiQualityRuleEntity apiQualityRule = mock(ApiQualityRuleEntity.class);
        when(apiQualityRule.getApi()).thenReturn("apiID");
        when(apiQualityRule.getQualityRule()).thenReturn("1");
        when(apiQualityRule.isChecked()).thenReturn(false);
        when(apiQualityRuleService.findByApi("apiID")).thenReturn(singletonList(apiQualityRule));

        ApiQualityMetricsEntity metrics = srv.getMetrics(GraviteeContext.getExecutionContext(), api);

        assertEquals(0.5, metrics.getScore(), 0);
        assertFalse(metrics.getMetricsPassed().isEmpty());
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_LOGO_WEIGHT.key()));
        assertTrue(metrics.getMetricsPassed().get(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT.key()));
        assertFalse(metrics.getMetricsPassed().get("1"));
    }
}
