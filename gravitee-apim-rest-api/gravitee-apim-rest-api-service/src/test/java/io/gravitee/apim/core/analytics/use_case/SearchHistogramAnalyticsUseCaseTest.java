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
package io.gravitee.apim.core.analytics.use_case;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics.model.Aggregation;
import io.gravitee.apim.core.analytics.model.Bucket;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchHistogramAnalyticsUseCaseTest {

    private ApiCrudService apiCrudService;
    private AnalyticsQueryService analyticsQueryService;
    private SearchHistogramAnalyticsUseCase useCase;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        apiCrudService = mock(ApiCrudService.class);
        analyticsQueryService = mock(AnalyticsQueryService.class);
        useCase = new SearchHistogramAnalyticsUseCase(apiCrudService, analyticsQueryService);
        executionContext = mock(ExecutionContext.class);
    }

    @Test
    void shouldReturnHistogramAnalytics() {
        String apiId = "api-1";
        long from = 1000L;
        long to = 2000L;
        long interval = 100L;
        List<Aggregation> aggregations = List.of();

        Api api = mock(Api.class);
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);

        // Properly mock apiDefinitionHttpV4 and its isTcpProxy() method
        io.gravitee.definition.model.v4.Api apiDefinitionHttpV4 = mock(io.gravitee.definition.model.v4.Api.class);
        when(apiDefinitionHttpV4.isTcpProxy()).thenReturn(false);
        when(api.getApiDefinitionHttpV4()).thenReturn(apiDefinitionHttpV4);

        when(api.belongsToEnvironment(anyString())).thenReturn(true);
        when(api.getEnvironmentId()).thenReturn("env-1");
        when(api.getId()).thenReturn(apiId);

        when(apiCrudService.get(apiId)).thenReturn(api);

        List<Bucket> buckets = List.of(new Bucket());
        var histogramAnalytics = mock(io.gravitee.apim.core.analytics.model.HistogramAnalytics.class);
        when(histogramAnalytics.getValues()).thenReturn(buckets);

        when(analyticsQueryService.searchHistogramAnalytics(any(), any())).thenReturn(Optional.of(histogramAnalytics));

        var input = new SearchHistogramAnalyticsUseCase.Input(apiId, from, to, interval, aggregations);
        var output = useCase.execute(executionContext, input);

        assertNotNull(output);
        assertEquals(
            new Timestamp(java.time.Instant.ofEpochMilli(from), java.time.Instant.ofEpochMilli(to), java.time.Duration.ofMillis(interval)),
            output.timestamp()
        );
        assertEquals(buckets, output.values());
    }

    @Test
    void shouldThrowWhenApiNotV4() {
        String apiId = "api-2";
        Api api = mock(Api.class);
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);
        when(apiCrudService.get(apiId)).thenReturn(api);

        var input = new SearchHistogramAnalyticsUseCase.Input(apiId, 0, 0, 0, List.of());

        assertThrows(ApiInvalidDefinitionVersionException.class, () -> useCase.execute(executionContext, input));
    }

    @Test
    void shouldThrowWhenTcpProxy() {
        String apiId = "api-3";
        Api api = mock(Api.class);
        when(api.getId()).thenReturn(apiId);
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        var httpV4 = mock(io.gravitee.definition.model.v4.Api.class);
        when(httpV4.isTcpProxy()).thenReturn(true);
        when(api.getApiDefinitionHttpV4()).thenReturn(httpV4);
        when(apiCrudService.get(apiId)).thenReturn(api);

        var input = new SearchHistogramAnalyticsUseCase.Input(apiId, 0, 0, 0, List.of());

        assertThrows(TcpProxyNotSupportedException.class, () -> useCase.execute(executionContext, input));
    }

    @Test
    void shouldThrowWhenApiNotInEnvironment() {
        String apiId = "api-4";
        Api api = mock(Api.class);
        when(api.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);

        // Properly mock apiDefinitionHttpV4 and its isTcpProxy() method
        io.gravitee.definition.model.v4.Api apiDefinitionHttpV4 = mock(io.gravitee.definition.model.v4.Api.class);
        when(apiDefinitionHttpV4.isTcpProxy()).thenReturn(false);
        when(api.getApiDefinitionHttpV4()).thenReturn(apiDefinitionHttpV4);

        when(api.belongsToEnvironment(anyString())).thenReturn(false);
        when(api.getEnvironmentId()).thenReturn("env-2");
        when(api.getId()).thenReturn(apiId);

        when(apiCrudService.get(apiId)).thenReturn(api);

        var input = new SearchHistogramAnalyticsUseCase.Input(apiId, 0, 0, 0, List.of());

        assertThrows(ApiNotFoundException.class, () -> useCase.execute(executionContext, input));
    }
}
