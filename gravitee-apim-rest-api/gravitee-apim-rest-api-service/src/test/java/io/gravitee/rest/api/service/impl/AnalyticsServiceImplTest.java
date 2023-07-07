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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.count.CountResponse;
import io.gravitee.repository.analytics.query.groupby.GroupByResponse;
import io.gravitee.repository.analytics.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.analytics.query.stats.StatsResponse;
import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.HitsAnalytics;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.AnalyticsCalculateException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyticsServiceImplTest {

    @InjectMocks
    private AnalyticsServiceImpl cut;

    @Mock
    private AnalyticsRepository analyticsRepository;

    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();

    @Test(expected = AnalyticsCalculateException.class)
    public void shouldThrowExceptionForStatusQuery() throws Exception {
        when(analyticsRepository.query(any())).thenThrow(new AnalyticsException());

        cut.execute(new StatsQuery());
    }

    @Test
    public void shouldExecuteStatusQueryWithNoResponse() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(null);
        StatsAnalytics execute = cut.execute(new StatsQuery());

        assertNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test
    public void shouldExecuteStatusQuery() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(new StatsResponse());
        StatsAnalytics execute = cut.execute(new StatsQuery());

        assertNotNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test(expected = AnalyticsCalculateException.class)
    public void shouldThrowExceptionForCountQuery() throws Exception {
        when(analyticsRepository.query(any())).thenThrow(new AnalyticsException());

        cut.execute(new CountQuery());
    }

    @Test
    public void shouldExecuteCountQueryWithNoResponse() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(null);
        HitsAnalytics execute = cut.execute(new CountQuery());

        assertNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test
    public void shouldExecuteCountQuery() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(new CountResponse());
        HitsAnalytics execute = cut.execute(new CountQuery());

        assertNotNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test(expected = AnalyticsCalculateException.class)
    public void shouldThrowExceptionForDateHistogramQuery() throws Exception {
        when(analyticsRepository.query(any())).thenThrow(new AnalyticsException());

        cut.execute(EXECUTION_CONTEXT, new DateHistogramQuery());
    }

    @Test
    public void shouldExecuteDateHistogramQueryWithNoResponse() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(null);
        HistogramAnalytics execute = cut.execute(EXECUTION_CONTEXT, new DateHistogramQuery());

        assertNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test
    public void shouldExecuteDateHistogramQuery() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(new DateHistogramResponse());
        HistogramAnalytics execute = cut.execute(EXECUTION_CONTEXT, new DateHistogramQuery());

        assertNotNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test(expected = AnalyticsCalculateException.class)
    public void shouldThrowExceptionForGroupByQuery() throws Exception {
        when(analyticsRepository.query(any())).thenThrow(new AnalyticsException());

        cut.execute(EXECUTION_CONTEXT, new GroupByQuery());
    }

    @Test
    public void shouldExecuteGroupByQueryWithNoResponse() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(null);
        TopHitsAnalytics execute = cut.execute(EXECUTION_CONTEXT, new GroupByQuery());

        assertNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }

    @Test
    public void shouldExecuteGroupByQuery() throws Exception {
        when(analyticsRepository.query(any())).thenReturn(new GroupByResponse());
        TopHitsAnalytics execute = cut.execute(EXECUTION_CONTEXT, new GroupByQuery());

        assertNotNull(execute);
        verify(analyticsRepository, times(1)).query(any());
    }
}
