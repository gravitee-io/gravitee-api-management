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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.analytics.model.Aggregation;
import io.gravitee.apim.core.analytics.model.Bucket;
import io.gravitee.apim.core.analytics.model.HistogramAnalytics;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.apim.core.analytics.use_case.SearchHistogramAnalyticsUseCase.Input;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchHistogramAnalyticsUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENV_ID = "environment-id";

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private SearchHistogramAnalyticsUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new SearchHistogramAnalyticsUseCase(apiCrudService, analyticsQueryService);
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        analyticsQueryService.reset();
    }

    @Test
    void shouldReturnHistogramAnalytics() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        long from = INSTANT_NOW.minus(Duration.ofHours(1)).toEpochMilli();
        long to = INSTANT_NOW.toEpochMilli();
        long interval = 60000L;
        List<Aggregation> aggregations = List.of();

        var expectedTimestamp = new Timestamp(Instant.ofEpochMilli(from), Instant.ofEpochMilli(to), Duration.ofMillis(interval));
        var expectedBuckets = List.of(new Bucket());
        analyticsQueryService.histogramAnalytics = new HistogramAnalytics(expectedTimestamp, expectedBuckets);

        var input = new Input(ApiFixtures.MY_API, from, to, interval, aggregations, Optional.empty());
        var output = useCase.execute(GraviteeContext.getExecutionContext(), input);

        assertThat(output).isNotNull();
        assertThat(output.timestamp()).isEqualTo(expectedTimestamp);
        assertThat(output.values()).isEqualTo(expectedBuckets);
    }

    @Test
    void shouldReturnHistogramAnalyticsWithQuery() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        long from = INSTANT_NOW.minus(Duration.ofHours(1)).toEpochMilli();
        long to = INSTANT_NOW.toEpochMilli();
        long interval = 60000L;
        List<Aggregation> aggregations = List.of();
        String queryString = "status:200 AND method:GET";

        var expectedTimestamp = new Timestamp(Instant.ofEpochMilli(from), Instant.ofEpochMilli(to), Duration.ofMillis(interval));
        var expectedBuckets = List.of(new Bucket());
        analyticsQueryService.histogramAnalytics = new HistogramAnalytics(expectedTimestamp, expectedBuckets);

        var input = new Input(ApiFixtures.MY_API, from, to, interval, aggregations, Optional.of(queryString));
        var output = useCase.execute(GraviteeContext.getExecutionContext(), input);

        assertThat(output).isNotNull();
        assertThat(output.timestamp()).isEqualTo(expectedTimestamp);
        assertThat(output.values()).isEqualTo(expectedBuckets);
    }

    @Test
    void shouldThrowWhenApiNotV4() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));
        var input = new Input(ApiFixtures.MY_API, 0, 0, 0, List.of(), Optional.empty());
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void shouldThrowWhenTcpProxy() {
        apiCrudService.initWith(List.of(ApiFixtures.aTcpApiV4()));
        var input = new Input(ApiFixtures.MY_API, 0, 0, 0, List.of(), Optional.empty());
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(TcpProxyNotSupportedException.class);
    }

    @Test
    void shouldThrowWhenApiNotInEnvironment() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId("definitely not" + ENV_ID).build()));
        var input = new Input(ApiFixtures.MY_API, 0, 0, 0, List.of(), Optional.empty());
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void shouldThrowWhenFromIsAfterTo() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        long from = 2000L;
        long to = 1000L;
        long interval = 100L;
        var input = new Input(ApiFixtures.MY_API, from, to, interval, List.of(), Optional.empty());
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(IllegalTimeRangeException.class);
    }
}
