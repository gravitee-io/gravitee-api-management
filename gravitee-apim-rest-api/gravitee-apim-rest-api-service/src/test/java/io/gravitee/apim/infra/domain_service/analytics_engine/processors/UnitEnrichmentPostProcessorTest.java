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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.AVG;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.COUNT;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.SUM;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.*;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Unit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnitEnrichmentPostProcessorTest {

    private final AnalyticsDefinitionQueryService definitionQueryService = mock(AnalyticsDefinitionQueryService.class);
    private final UnitEnrichmentPostProcessorImpl processor = new UnitEnrichmentPostProcessorImpl(definitionQueryService);

    @Nested
    class Measures {

        @Test
        void should_enrich_unit_for_single_metric() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));

            var response = new MeasuresResponse(
                List.of(new MetricMeasuresResponse(HTTP_REQUESTS, null, List.of(new Measure(COUNT, 42)), List.of()))
            );

            var enriched = processor.enrichUnits(response);

            assertThat(enriched.metrics()).hasSize(1);
            assertThat(enriched.metrics().get(0).unit()).isEqualTo(NUMBER);
            assertThat(enriched.metrics().get(0).name()).isEqualTo(HTTP_REQUESTS);
            assertThat(enriched.metrics().get(0).measures()).isEqualTo(List.of(new Measure(COUNT, 42)));
        }

        @Test
        void should_enrich_units_for_multiple_metrics() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));
            when(definitionQueryService.findMetric(HTTP_GATEWAY_RESPONSE_TIME)).thenReturn(
                Optional.of(metricSpec(HTTP_GATEWAY_RESPONSE_TIME, MILLISECONDS))
            );

            var response = new MeasuresResponse(
                List.of(
                    new MetricMeasuresResponse(HTTP_REQUESTS, null, List.of(new Measure(COUNT, 100)), List.of()),
                    new MetricMeasuresResponse(HTTP_GATEWAY_RESPONSE_TIME, null, List.of(new Measure(AVG, 45.6)), List.of())
                )
            );

            var enriched = processor.enrichUnits(response);

            assertThat(enriched.metrics()).hasSize(2);
            assertThat(enriched.metrics().get(0).unit()).isEqualTo(NUMBER);
            assertThat(enriched.metrics().get(1).unit()).isEqualTo(MILLISECONDS);
        }

        @Test
        void should_set_null_unit_for_unknown_metric() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.empty());

            var response = new MeasuresResponse(
                List.of(new MetricMeasuresResponse(HTTP_REQUESTS, null, List.of(new Measure(AVG, 10)), List.of()))
            );

            var enriched = processor.enrichUnits(response);

            assertThat(enriched.metrics()).hasSize(1);
            assertThat(enriched.metrics().get(0).unit()).isNull();
        }

        @Test
        void should_preserve_measures() {
            when(definitionQueryService.findMetric(HTTP_GATEWAY_RESPONSE_TIME)).thenReturn(
                Optional.of(metricSpec(HTTP_GATEWAY_RESPONSE_TIME, MILLISECONDS))
            );

            var measures = List.of(new Measure(AVG, 45.6), new Measure(COUNT, 100));
            var response = new MeasuresResponse(List.of(new MetricMeasuresResponse(HTTP_GATEWAY_RESPONSE_TIME, null, measures, List.of())));

            var enriched = processor.enrichUnits(response);

            assertThat(enriched.metrics().get(0).measures()).isEqualTo(measures);
        }
    }

    @Nested
    class Facets {

        @Test
        void should_enrich_unit_for_single_metric() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));

            var bucket = new FacetBucketResponse("api-1", "API 1", null, List.of(new Measure(COUNT, 42)));
            var response = new FacetsResponse(List.of(new MetricFacetsResponse(HTTP_REQUESTS, null, List.of(bucket), List.of())));

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics()).hasSize(1);
            assertThat(enriched.metrics().get(0).unit()).isEqualTo(NUMBER);
            assertThat(enriched.metrics().get(0).metric()).isEqualTo(HTTP_REQUESTS);
            assertThat(enriched.metrics().get(0).buckets()).isEqualTo(List.of(bucket));
        }

        @Test
        void should_enrich_units_for_multiple_metrics() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));
            when(definitionQueryService.findMetric(HTTP_RESPONSE_CONTENT_LENGTH)).thenReturn(
                Optional.of(metricSpec(HTTP_RESPONSE_CONTENT_LENGTH, BYTES))
            );

            var bucket1 = new FacetBucketResponse("api-1", "API 1", null, List.of(new Measure(COUNT, 42)));
            var bucket2 = new FacetBucketResponse("api-2", "API 2", null, List.of(new Measure(AVG, 1024)));
            var response = new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(HTTP_REQUESTS, null, List.of(bucket1), List.of()),
                    new MetricFacetsResponse(HTTP_RESPONSE_CONTENT_LENGTH, null, List.of(bucket2), List.of())
                )
            );

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics()).hasSize(2);
            assertThat(enriched.metrics().get(0).unit()).isEqualTo(NUMBER);
            assertThat(enriched.metrics().get(1).unit()).isEqualTo(BYTES);
        }

        @Test
        void should_set_null_unit_for_unknown_metric() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.empty());

            var bucket = new FacetBucketResponse("api-1", "API 1", null, List.of(new Measure(AVG, 42)));
            var response = new FacetsResponse(List.of(new MetricFacetsResponse(HTTP_REQUESTS, null, List.of(bucket), List.of())));

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics()).hasSize(1);
            assertThat(enriched.metrics().get(0).unit()).isNull();
        }

        @Test
        void should_preserve_buckets() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));

            var innerBucket = new FacetBucketResponse("200", "200", null, List.of(new Measure(COUNT, 10)));
            var bucket = new FacetBucketResponse("api-1", "API 1", List.of(innerBucket), null);
            var response = new FacetsResponse(List.of(new MetricFacetsResponse(HTTP_REQUESTS, null, List.of(bucket), List.of())));

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics().get(0).buckets()).isEqualTo(List.of(bucket));
        }
    }

    @Nested
    class TimeSeries {

        @Test
        void should_enrich_unit_for_single_metric() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));

            var timeBucket = new TimeSeriesBucketResponse(
                "2024-01-01T00:00:00.000Z",
                null,
                1234567890L,
                null,
                List.of(new Measure(COUNT, 42))
            );
            var response = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(HTTP_REQUESTS, null, List.of(timeBucket), List.of()))
            );

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics()).hasSize(1);
            assertThat(enriched.metrics().get(0).unit()).isEqualTo(NUMBER);
            assertThat(enriched.metrics().get(0).name()).isEqualTo(HTTP_REQUESTS);
            assertThat(enriched.metrics().get(0).buckets()).isEqualTo(List.of(timeBucket));
        }

        @Test
        void should_enrich_units_for_multiple_metrics() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));
            when(definitionQueryService.findMetric(HTTP_GATEWAY_RESPONSE_TIME)).thenReturn(
                Optional.of(metricSpec(HTTP_GATEWAY_RESPONSE_TIME, MILLISECONDS))
            );

            var timeBucket1 = new TimeSeriesBucketResponse(
                "2024-01-01T00:00:00.000Z",
                null,
                1234567890L,
                null,
                List.of(new Measure(COUNT, 42))
            );
            var timeBucket2 = new TimeSeriesBucketResponse(
                "2024-01-01T00:00:00.000Z",
                null,
                1234567890L,
                null,
                List.of(new Measure(AVG, 150))
            );
            var response = new TimeSeriesResponse(
                List.of(
                    new TimeSeriesMetricResponse(HTTP_REQUESTS, null, List.of(timeBucket1), List.of()),
                    new TimeSeriesMetricResponse(HTTP_GATEWAY_RESPONSE_TIME, null, List.of(timeBucket2), List.of())
                )
            );

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics()).hasSize(2);
            assertThat(enriched.metrics().get(0).unit()).isEqualTo(NUMBER);
            assertThat(enriched.metrics().get(1).unit()).isEqualTo(MILLISECONDS);
        }

        @Test
        void should_set_null_unit_for_unknown_metric() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.empty());

            var timeBucket = new TimeSeriesBucketResponse(
                "2024-01-01T00:00:00.000Z",
                null,
                1234567890L,
                null,
                List.of(new Measure(AVG, 42))
            );
            var response = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(HTTP_REQUESTS, null, List.of(timeBucket), List.of()))
            );

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics()).hasSize(1);
            assertThat(enriched.metrics().get(0).unit()).isNull();
        }

        @Test
        void should_preserve_buckets() {
            when(definitionQueryService.findMetric(HTTP_GATEWAY_RESPONSE_TIME)).thenReturn(
                Optional.of(metricSpec(HTTP_GATEWAY_RESPONSE_TIME, MILLISECONDS))
            );

            var facetBucket = new FacetBucketResponse("api-1", "API 1", null, List.of(new Measure(AVG, 120)));
            var timeBucket = new TimeSeriesBucketResponse("2024-01-01T00:00:00.000Z", null, 1234567890L, List.of(facetBucket), null);
            var response = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(HTTP_GATEWAY_RESPONSE_TIME, null, List.of(timeBucket), List.of()))
            );

            var enriched = processor.enrichUnits(response, List.of());

            assertThat(enriched.metrics().get(0).buckets()).isEqualTo(List.of(timeBucket));
        }
    }

    @Nested
    class AllUnitTypes {

        @Test
        void should_enrich_with_number_unit() {
            when(definitionQueryService.findMetric(HTTP_REQUESTS)).thenReturn(Optional.of(metricSpec(HTTP_REQUESTS, NUMBER)));

            var response = new MeasuresResponse(
                List.of(new MetricMeasuresResponse(HTTP_REQUESTS, null, List.of(new Measure(COUNT, 1)), List.of()))
            );

            assertThat(processor.enrichUnits(response).metrics().get(0).unit()).isEqualTo(NUMBER);
        }

        @Test
        void should_enrich_with_milliseconds_unit() {
            when(definitionQueryService.findMetric(HTTP_GATEWAY_RESPONSE_TIME)).thenReturn(
                Optional.of(metricSpec(HTTP_GATEWAY_RESPONSE_TIME, MILLISECONDS))
            );

            var response = new MeasuresResponse(
                List.of(new MetricMeasuresResponse(HTTP_GATEWAY_RESPONSE_TIME, null, List.of(new Measure(AVG, 50)), List.of()))
            );

            assertThat(processor.enrichUnits(response).metrics().get(0).unit()).isEqualTo(MILLISECONDS);
        }

        @Test
        void should_enrich_with_bytes_unit() {
            when(definitionQueryService.findMetric(HTTP_RESPONSE_CONTENT_LENGTH)).thenReturn(
                Optional.of(metricSpec(HTTP_RESPONSE_CONTENT_LENGTH, BYTES))
            );

            var response = new MeasuresResponse(
                List.of(new MetricMeasuresResponse(HTTP_RESPONSE_CONTENT_LENGTH, null, List.of(new Measure(AVG, 2048)), List.of()))
            );

            assertThat(processor.enrichUnits(response).metrics().get(0).unit()).isEqualTo(BYTES);
        }

        @Test
        void should_enrich_with_percent_unit() {
            when(definitionQueryService.findMetric(HTTP_ERROR_RATE)).thenReturn(Optional.of(metricSpec(HTTP_ERROR_RATE, PERCENT)));

            var response = new MeasuresResponse(
                List.of(new MetricMeasuresResponse(HTTP_ERROR_RATE, null, List.of(new Measure(AVG, 5.2)), List.of()))
            );

            assertThat(processor.enrichUnits(response).metrics().get(0).unit()).isEqualTo(PERCENT);
        }
    }

    /** COUNT is a number of requests: it never carries the unit of the value it counts, on a built-in or an additional metric. */
    @Nested
    class CountUnit {

        private static final AdditionalMetricName PRICED = new AdditionalMetricName(
            new AdditionalMetricField(AdditionalMetricField.ValueType.DOUBLE, "double_billing_price")
        );

        @BeforeEach
        void cost_is_a_currency() {
            when(definitionQueryService.findMetric(LLM_PROMPT_TOKEN_TOTAL_COST)).thenReturn(
                Optional.of(metricSpec(LLM_PROMPT_TOKEN_TOTAL_COST, CURRENCY))
            );
        }

        @Test
        void should_serve_an_entry_reading_count_only_as_a_number_of_requests() {
            var response = new MeasuresResponse(
                List.of(
                    new MetricMeasuresResponse(LLM_PROMPT_TOKEN_TOTAL_COST, null, List.of(new Measure(COUNT, 345)), List.of()),
                    new MetricMeasuresResponse(PRICED, null, List.of(new Measure(COUNT, 12)), List.of())
                )
            );

            assertThat(processor.enrichUnits(response).metrics()).extracting(MetricMeasuresResponse::unit).containsExactly(NUMBER, NUMBER);
        }

        /** The unit then applies to the other measures; COUNT stays a number of requests, as the API description says. */
        @Test
        void should_keep_the_unit_of_the_metric_on_an_entry_mixing_count_with_another_measure() {
            var response = new MeasuresResponse(
                List.of(
                    new MetricMeasuresResponse(
                        LLM_PROMPT_TOKEN_TOTAL_COST,
                        null,
                        List.of(new Measure(COUNT, 345), new Measure(SUM, 0.0083)),
                        List.of()
                    ),
                    new MetricMeasuresResponse(LLM_PROMPT_TOKEN_TOTAL_COST, null, List.of(new Measure(SUM, 0.0083)), List.of()),
                    new MetricMeasuresResponse(PRICED, null, List.of(new Measure(COUNT, 12), new Measure(AVG, 2.5)), List.of())
                )
            );

            assertThat(processor.enrichUnits(response).metrics())
                .extracting(MetricMeasuresResponse::unit)
                .containsExactly(CURRENCY, CURRENCY, null);
        }

        /** The unit follows the measures asked for, so a COUNT reads as a count even over a window without any bucket. */
        @Test
        void should_serve_facets_reading_count_only_as_a_number_of_requests_with_or_without_buckets() {
            var leaf = new FacetBucketResponse("gpt-4", "gpt-4", null, List.of(new Measure(COUNT, 3)));
            var bucket = new FacetBucketResponse("api-1", "API 1", List.of(leaf), null);
            var filled = new FacetsResponse(
                List.of(new MetricFacetsResponse(LLM_PROMPT_TOKEN_TOTAL_COST, null, List.of(bucket), List.of()))
            );
            var empty = new FacetsResponse(List.of(new MetricFacetsResponse(LLM_PROMPT_TOKEN_TOTAL_COST, null, List.of(), List.of())));

            assertThat(processor.enrichUnits(filled, requested(COUNT)).metrics().getFirst().unit()).isEqualTo(NUMBER);
            assertThat(processor.enrichUnits(empty, requested(COUNT)).metrics().getFirst().unit()).isEqualTo(NUMBER);
            assertThat(processor.enrichUnits(empty, requested(COUNT, SUM)).metrics().getFirst().unit()).isEqualTo(CURRENCY);
        }

        @Test
        void should_serve_a_time_series_reading_count_only_as_a_number_of_requests_with_or_without_buckets() {
            var leaf = new FacetBucketResponse("gpt-4", "gpt-4", null, List.of(new Measure(COUNT, 3)));
            var timeBucket = new TimeSeriesBucketResponse("2024-01-01T00:00:00.000Z", null, 1L, List.of(leaf), null);
            var filled = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(LLM_PROMPT_TOKEN_TOTAL_COST, null, List.of(timeBucket), List.of()))
            );
            var empty = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(LLM_PROMPT_TOKEN_TOTAL_COST, null, List.of(), List.of()))
            );

            assertThat(processor.enrichUnits(filled, requested(COUNT)).metrics().getFirst().unit()).isEqualTo(NUMBER);
            assertThat(processor.enrichUnits(empty, requested(COUNT)).metrics().getFirst().unit()).isEqualTo(NUMBER);
            assertThat(processor.enrichUnits(empty, requested(COUNT, SUM)).metrics().getFirst().unit()).isEqualTo(CURRENCY);
        }

        @Test
        void should_keep_the_unit_of_the_metric_on_an_entry_reading_no_measure() {
            assertThat(MetricSpec.servedUnit(CURRENCY, List.of())).isEqualTo(CURRENCY);
        }

        private static List<FacetMetricMeasuresRequest> requested(MetricSpec.Measure... measures) {
            return List.of(new FacetMetricMeasuresRequest(LLM_PROMPT_TOKEN_TOTAL_COST, List.of(measures), List.of()));
        }
    }

    private static MetricSpec metricSpec(MetricSpec.Name name, MetricSpec.Unit unit) {
        return new MetricSpec(name, name.name(), null, unit, null, null, null);
    }

    @Nested
    class IgnoredFilters {

        private static final List<FilterName> SKIPPED = List.of(FilterSpec.Name.NATIVE_CLIENT_ID);

        @Test
        void should_keep_them_on_measures() {
            var response = new MeasuresResponse(List.of(new MetricMeasuresResponse(HTTP_REQUESTS, null, List.of(), SKIPPED)));

            assertThat(processor.enrichUnits(response).metrics().get(0).ignoredFilters()).isEqualTo(SKIPPED);
        }

        @Test
        void should_keep_them_on_facets() {
            var response = new FacetsResponse(List.of(new MetricFacetsResponse(HTTP_REQUESTS, null, List.of(), SKIPPED)));

            assertThat(processor.enrichUnits(response, List.of()).metrics().get(0).ignoredFilters()).isEqualTo(SKIPPED);
        }

        @Test
        void should_keep_them_on_time_series() {
            var response = new TimeSeriesResponse(List.of(new TimeSeriesMetricResponse(HTTP_REQUESTS, null, List.of(), SKIPPED)));

            assertThat(processor.enrichUnits(response, List.of()).metrics().get(0).ignoredFilters()).isEqualTo(SKIPPED);
        }
    }
}
