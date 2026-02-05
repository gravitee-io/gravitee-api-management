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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.computation;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.AnalyticsEngineFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesBucketResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Bucket;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.BucketLeaf;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CustomInterval;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Measure;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesBucket;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesBucketLeaf;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import jakarta.ws.rs.client.Entity;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsComputationResourceTest extends ApiResourceTest {

    @Autowired
    AnalyticsRepository analyticsRepository;

    @Autowired
    FilterPreProcessor filterPreProcessor;

    @Autowired
    BucketNamesPostProcessor bucketNamesPostprocessor;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/analytics";
    }

    @Nested
    class Measures {

        @BeforeEach
        void setUp() {
            var queryContext = new QueryContext(ORGANIZATION, ENVIRONMENT);
            when(
                analyticsRepository.searchHTTPMeasures(
                    eq(queryContext),
                    argThat(query -> query.metrics().getFirst().metric() == Metric.HTTP_REQUESTS)
                )
            ).thenReturn(
                new MeasuresResult(
                    List.of(
                        new MetricMeasuresResult(
                            Metric.HTTP_REQUESTS,
                            Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 42)
                        )
                    )
                )
            );

            when(
                analyticsRepository.searchMessageMeasures(
                    eq(queryContext),
                    argThat(query -> query.metrics().getFirst().metric() == Metric.MESSAGES)
                )
            ).thenReturn(
                new MeasuresResult(
                    List.of(
                        new MetricMeasuresResult(
                            Metric.MESSAGES,
                            Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 42)
                        )
                    )
                )
            );

            when(filterPreProcessor.buildFilters(any(UserContext.class))).thenAnswer(caller -> caller.getArgument(0));
        }

        @Test
        void should_fail_with_invalid_time_range() {
            var invalidRequest = aCountMeasureRequest();
            var from = invalidRequest.getTimeRange().getFrom();
            var to = invalidRequest.getTimeRange().getTo();
            invalidRequest.getTimeRange().from(to).to(from);
            var response = rootTarget().path("measures").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_invalid_measure_for_metric() {
            var invalidRequest = aCountMeasureRequest();
            var metric = invalidRequest.getMetrics().getFirst();
            metric.setMeasures(List.of(MeasureName.AVG));
            var response = rootTarget().path("measures").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_return_counts() {
            var response = rootTarget().path("measures").request().post(Entity.json(aCountMeasureRequest()));

            assertThat(response)
                .hasStatus(200)
                .asEntity(MeasuresResponse.class)
                .satisfies(measuresResponse -> {
                    assertThat(measuresResponse.getMetrics()).containsExactlyInAnyOrder(
                        new MeasuresResponseMetricsInner()
                            .name(MetricName.HTTP_REQUESTS)
                            .measures(List.of(new Measure().name(MeasureName.COUNT).value(42))),
                        new MeasuresResponseMetricsInner()
                            .name(MetricName.MESSAGES)
                            .measures(List.of(new Measure().name(MeasureName.COUNT).value(42)))
                    );
                });
        }
    }

    @Nested
    class Facets {

        @BeforeEach
        void setUp() {
            var queryContext = new QueryContext(ORGANIZATION, ENVIRONMENT);

            when(
                analyticsRepository.searchHTTPFacets(eq(queryContext), argThat(query -> query.facets().getFirst() == Facet.HTTP_STATUS))
            ).thenReturn(
                new FacetsResult(
                    List.of(
                        new MetricFacetsResult(
                            Metric.HTTP_REQUESTS,
                            List.of(
                                FacetBucketResult.ofMeasures(
                                    "100-199",
                                    Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 100)
                                ),
                                FacetBucketResult.ofMeasures(
                                    "200-299",
                                    Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 200)
                                )
                            )
                        )
                    )
                )
            );

            when(
                bucketNamesPostprocessor.mapBucketNames(
                    any(),
                    any(),
                    any(io.gravitee.apim.core.analytics_engine.model.FacetsResponse.class)
                )
            ).thenAnswer(invocation -> invocation.getArgument(2));

            when(filterPreProcessor.buildFilters(any(UserContext.class))).thenAnswer(caller -> caller.getArgument(0));
        }

        @Test
        void should_fail_with_invalid_time_range() {
            var invalidRequest = aRequestCountFacetRequest();
            var from = invalidRequest.getTimeRange().getFrom();
            var to = invalidRequest.getTimeRange().getTo();
            invalidRequest.getTimeRange().from(to).to(from);
            var response = rootTarget().path("facets").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_invalid_measure_for_metric() {
            var invalidRequest = aRequestCountFacetRequest();
            var metric = invalidRequest.getMetrics().getFirst();
            metric.setMeasures(List.of(MeasureName.AVG));
            var response = rootTarget().path("facets").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_no_facet() {
            var invalidRequest = aRequestCountFacetRequest();
            invalidRequest.setBy(List.of());
            var response = rootTarget().path("facets").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_too_much_facets() {
            var invalidRequest = aRequestCountFacetRequest();
            invalidRequest.setBy(List.of(FacetName.API, FacetName.APPLICATION, FacetName.PLAN, FacetName.HTTP_STATUS));
            var response = rootTarget().path("facets").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_incompatible_facet() {
            var invalidRequest = aRequestCountFacetRequest();
            invalidRequest.setBy(List.of(FacetName.KAFKA_TOPIC));
            var response = rootTarget().path("facets").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_return_request_counts() {
            var response = rootTarget().path("facets").request().post(Entity.json(aRequestCountFacetRequest()));

            assertThat(response)
                .hasStatus(200)
                .asEntity(FacetsResponse.class)
                .isEqualTo(
                    new FacetsResponse().metrics(
                        List.of(
                            new FacetsResponseMetricsInner()
                                .name(MetricName.HTTP_REQUESTS)
                                .buckets(List.of(expectLeafBucket("100-199", 100), expectLeafBucket("200-299", 200)))
                        )
                    )
                );
        }

        private static Bucket expectLeafBucket(String key, Number count) {
            var bucket = new Bucket();
            var leaf = new BucketLeaf().type(BucketLeaf.TypeEnum.LEAF);
            leaf.setKey(key);
            leaf.setName(key);
            leaf.setMeasures(List.of(new Measure().name(MeasureName.COUNT).value(count)));
            bucket.setActualInstance(leaf);
            return bucket;
        }
    }

    @Nested
    class TimeSeries {

        @BeforeEach
        void setUp() {
            var queryContext = new QueryContext(ORGANIZATION, ENVIRONMENT);
            var interval = Duration.ofHours(1).toMillis();

            when(analyticsRepository.searchHTTPTimeSeries(eq(queryContext), argThat(query -> query.interval() == interval))).thenReturn(
                new TimeSeriesResult(
                    List.of(
                        new MetricTimeSeriesResult(
                            Metric.HTTP_REQUESTS,
                            List.of(
                                TimeSeriesBucketResult.ofMeasures(
                                    "2025-11-07T00:00:00Z",
                                    1762473600000L,
                                    Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 100)
                                ),
                                TimeSeriesBucketResult.ofMeasures(
                                    "2025-11-08T00:00:00Z",
                                    1762560000000L,
                                    Map.of(io.gravitee.repository.analytics.engine.api.metric.Measure.COUNT, 200)
                                )
                            )
                        )
                    )
                )
            );

            when(
                bucketNamesPostprocessor.mapBucketNames(
                    any(),
                    any(),
                    any(io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse.class)
                )
            ).thenAnswer(invocation -> invocation.getArgument(2));

            when(filterPreProcessor.buildFilters(any(UserContext.class))).thenAnswer(caller -> caller.getArgument(0));
        }

        @Test
        void should_fail_with_invalid_time_range() {
            var invalidRequest = aRequestCountTimeSeries();
            var from = invalidRequest.getTimeRange().getFrom();
            var to = invalidRequest.getTimeRange().getTo();
            invalidRequest.getTimeRange().from(to).to(from);
            var response = rootTarget().path("time-series").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_invalid_measure_for_metric() {
            var invalidRequest = aRequestCountTimeSeries();
            var metric = invalidRequest.getMetrics().getFirst();
            metric.setMeasures(List.of(MeasureName.AVG));
            var response = rootTarget().path("time-series").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_too_much_facets() {
            var invalidRequest = aRequestCountTimeSeries().by(List.of(FacetName.API, FacetName.APPLICATION, FacetName.PLAN));
            var response = rootTarget().path("time-series").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_incompatible_facet() {
            var invalidRequest = aRequestCountTimeSeries().by(List.of(FacetName.KAFKA_TOPIC));
            var response = rootTarget().path("time-series").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_fail_with_negative_interval() {
            var invalidRequest = aRequestCountTimeSeries().interval(new CustomInterval(-60000L));
            var response = rootTarget().path("time-series").request().post(Entity.json(invalidRequest));

            assertThat(response).hasStatus(400);
        }

        @Test
        void should_return_request_counts() {
            var response = rootTarget().path("time-series").request().post(Entity.json(aRequestCountTimeSeries()));

            assertThat(response)
                .hasStatus(200)
                .asEntity(TimeSeriesResponse.class)
                .isEqualTo(
                    new TimeSeriesResponse().metrics(
                        List.of(
                            new TimeSeriesResponseMetricsInner()
                                .name(MetricName.HTTP_REQUESTS)
                                .buckets(
                                    List.of(
                                        expectLeafBucket("2025-11-07T00:00:00Z", 1762473600000L, 100),
                                        expectLeafBucket("2025-11-08T00:00:00Z", 1762560000000L, 200)
                                    )
                                )
                        )
                    )
                );
        }

        private static TimeSeriesBucket expectLeafBucket(String key, Long timestamp, Number count) {
            var bucket = new TimeSeriesBucket();
            var leaf = new TimeSeriesBucketLeaf().type(TimeSeriesBucketLeaf.TypeEnum.LEAF);
            leaf.setKey(OffsetDateTime.parse(key));
            leaf.setTimestamp(timestamp);
            leaf.setMeasures(List.of(new Measure().name(MeasureName.COUNT).value(count)));
            bucket.setActualInstance(leaf);
            return bucket;
        }
    }
}
