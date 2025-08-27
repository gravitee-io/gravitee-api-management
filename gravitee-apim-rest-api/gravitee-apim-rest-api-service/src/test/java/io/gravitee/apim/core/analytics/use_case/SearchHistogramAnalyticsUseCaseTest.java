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

import static fixtures.core.model.ApiFixtures.MY_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.analytics.model.Aggregation;
import io.gravitee.apim.core.analytics.model.EventAnalytics;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final AnalyticsMetadataProvider provider = new AnalyticsMetadataProvider() {
        @Override
        public boolean appliesTo(Field field) {
            return field == Field.API;
        }

        @Override
        public Map<String, String> provide(String key, String environmentId) {
            return Map.of("name", "api-" + key);
        }
    };

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
        useCase = new SearchHistogramAnalyticsUseCase(apiCrudService, analyticsQueryService, List.of(provider));
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
        var expectedBuckets = List.of((HistogramAnalytics.Bucket) new HistogramAnalytics.CountBucket("name", "field", Map.of()));
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
        var expectedBuckets = List.of((HistogramAnalytics.Bucket) new HistogramAnalytics.MetricBucket("name", "field", List.of()));
        analyticsQueryService.histogramAnalytics = new HistogramAnalytics(expectedTimestamp, expectedBuckets);

        var input = new Input(ApiFixtures.MY_API, from, to, interval, aggregations, Optional.of(queryString));
        var output = useCase.execute(GraviteeContext.getExecutionContext(), input);

        assertThat(output).isNotNull();
        assertThat(output.timestamp()).isEqualTo(expectedTimestamp);
        assertThat(output.values()).isEqualTo(expectedBuckets);
    }

    @Test
    void shouldReturnTopValueHitsForANativeAPI() {
        apiCrudService.initWith(List.of(ApiFixtures.aNativeApi()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        Map<String, Map<String, Long>> analytics = new HashMap<>();
        analytics.put("downstream-active-connections_latest", Map.of("downstream-active-connections", 1L));
        analytics.put("upstream-active-connections_latest", Map.of("upstream-active-connections", 1L));
        analyticsQueryService.eventAnalytics = new EventAnalytics(analytics);
        long from = INSTANT_NOW.minus(Duration.ofHours(1)).toEpochMilli();
        long to = INSTANT_NOW.toEpochMilli();
        long interval = 60000L;
        var input = new SearchHistogramAnalyticsUseCase.Input(MY_API, from, to, interval, List.of(), Optional.empty());

        var result = useCase.execute(GraviteeContext.getExecutionContext(), input);

        List<HistogramAnalytics.Bucket> buckets = result.values();
        assertFalse(buckets.isEmpty());
        assertEquals(2, buckets.size());
        HistogramAnalytics.MetricBucket bucket0 = (HistogramAnalytics.MetricBucket) buckets.getFirst();
        assertEquals("downstream-active-connections", bucket0.getField());
        assertEquals("downstream-active-connections_latest", bucket0.getName());
        assertEquals(1L, bucket0.getValues().getFirst());
        HistogramAnalytics.MetricBucket bucket1 = (HistogramAnalytics.MetricBucket) buckets.get(1);
        assertEquals("upstream-active-connections", bucket1.getField());
        assertEquals("upstream-active-connections_latest", bucket1.getName());
        assertEquals(1L, bucket1.getValues().getFirst());
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

    @Test
    void shouldReturnMetadataForApiIdFieldAggregation() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        long from = 1000L;
        long to = 2000L;
        long interval = 100L;
        var input = new Input(
            ApiFixtures.MY_API,
            from,
            to,
            interval,
            List.of(new Aggregation("api-id", Aggregation.AggregationType.FIELD)),
            Optional.empty()
        );
        var expectedTimestamp = new Timestamp(Instant.ofEpochMilli(from), Instant.ofEpochMilli(to), Duration.ofMillis(interval));
        var expectedBuckets = List.of(
            (HistogramAnalytics.Bucket) new HistogramAnalytics.CountBucket("name", "api-id", Map.of("aid1", List.of(), "aid2", List.of()))
        );
        analyticsQueryService.histogramAnalytics = new HistogramAnalytics(expectedTimestamp, expectedBuckets);
        var result = useCase.execute(GraviteeContext.getExecutionContext(), input);
        assertThat(result.metadata()).isNotNull();
        assertThat(result.metadata().containsKey("name")).isTrue();
        assertThat(result.metadata().get("name")).containsKeys("aid1", "aid2");
        assertThat(result.metadata().get("name").get("aid1")).containsKey("name");
        assertThat(result.metadata().get("name").get("aid1").get("name")).isEqualTo("api-aid1");
    }
}
