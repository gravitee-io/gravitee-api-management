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

import static io.gravitee.apim.core.analytics_engine.model.FacetSpec.Name.*;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.COUNT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
class BucketNamesProcessorTest {

    static final String API_ID1 = "api-id1";
    static final String API_ID2 = "api-id2";

    static final String APPLICATION_ID1 = "app-id1";
    static final String APPLICATION_ID2 = "app-id2";

    static final String API_NAME1 = "api1";
    static final String API_NAME2 = "api2";

    static final String APPLICATION_NAME1 = "app1";
    static final String APPLICATION_NAME2 = "app2";

    static final String PLAN_ID1 = "plan-id1";
    static final String PLAN_ID2 = "plan-id2";
    static final String PLAN_NAME1 = "Keyless plan";
    static final String PLAN_NAME2 = "Gold plan";

    static final String API_PRODUCT_ID1 = "api-product-id1";
    static final String API_PRODUCT_NAME1 = "Payments";

    static final int FIXED_COUNT_VALUE = 42;
    static final long FIXED_TIMESTAMP = 1234567890L;
    static final String FIXED_TIME_KEY = "2024-01-01T00:00:00.000Z";

    final Map<String, String> apiNamesById = Map.of(API_ID1, API_NAME1, API_ID2, API_NAME2);
    final Map<String, String> applicationNamesById = Map.of(APPLICATION_ID1, APPLICATION_NAME1, APPLICATION_ID2, APPLICATION_NAME2);

    private final FilterValueNameResolver filterValueNameResolver = mock(FilterValueNameResolver.class);

    private final BucketNamesPostProcessor processor = new BucketNamesPostProcessorImpl(filterValueNameResolver);

    private AnalyticsQueryContext context;

    @BeforeEach
    void setUp() {
        context = new AnalyticsQueryContext(
            AuditInfo.builder().build(),
            new ExecutionContext("DEFAULT", "DEFAULT"),
            Set.of(API_ID1, API_ID2),
            apiNamesById,
            Map.of(),
            Map.of(),
            Optional.empty()
        );

        when(filterValueNameResolver.resolveNames(eq("DEFAULT"), eq(FilterSpec.Name.API), anyList())).thenReturn(apiNamesById);
        // The resolver names the "1" application, which the gateway reports for unidentified consumers.
        when(filterValueNameResolver.resolveNames(eq("DEFAULT"), eq(FilterSpec.Name.APPLICATION), anyList())).thenReturn(
            Map.of(APPLICATION_ID1, APPLICATION_NAME1, APPLICATION_ID2, APPLICATION_NAME2, "1", "Unknown")
        );
        when(filterValueNameResolver.resolveNames(eq("DEFAULT"), eq(FilterSpec.Name.PLAN), anyList())).thenReturn(
            Map.of(PLAN_ID1, PLAN_NAME1, PLAN_ID2, PLAN_NAME2)
        );
        when(filterValueNameResolver.resolveNames(eq("DEFAULT"), eq(FilterSpec.Name.API_PRODUCT), anyList())).thenReturn(
            Map.of(API_PRODUCT_ID1, API_PRODUCT_NAME1)
        );
    }

    @Nested
    class Facets {

        @Test
        void should_map_unnamed_facets_to_the_key() {
            var facetBucketResponse = newUnnamedBucketResponse("api.example.com");
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(HOST), response);

            var expectedResponse = facetsResponse(getBucketResponseWithDefaultName(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
            verifyNoInteractions(filterValueNameResolver);
        }

        @Test
        void should_update_plan_names() {
            var bucket1 = newUnnamedBucketResponse(PLAN_ID1);
            var bucket2 = newUnnamedBucketResponse(PLAN_ID2);
            var response = facetsResponse(bucket1, bucket2);

            var mappedResponse = processor.mapBucketNames(context, List.of(PLAN), response);

            var expectedResponse = facetsResponse(
                new FacetBucketResponse(PLAN_ID1, PLAN_NAME1, null, bucket1.measures()),
                new FacetBucketResponse(PLAN_ID2, PLAN_NAME2, null, bucket2.measures())
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_api_product_names() {
            var bucket = newUnnamedBucketResponse(API_PRODUCT_ID1);
            var response = facetsResponse(bucket);

            var mappedResponse = processor.mapBucketNames(context, List.of(API_PRODUCT), response);

            var expectedResponse = facetsResponse(new FacetBucketResponse(API_PRODUCT_ID1, API_PRODUCT_NAME1, null, bucket.measures()));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_keep_the_key_of_a_plan_that_no_longer_exists() {
            var bucket = newUnnamedBucketResponse("deleted-plan-id");
            var response = facetsResponse(bucket);

            var mappedResponse = processor.mapBucketNames(context, List.of(PLAN), response);

            var expectedResponse = facetsResponse(getBucketResponseWithDefaultName(bucket));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_api_and_plan_facets() {
            var innerBucket1 = newUnnamedBucketResponse(PLAN_ID1);
            var innerBucket2 = newUnnamedBucketResponse(PLAN_ID2);
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1, List.of(innerBucket1, innerBucket2));
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API, PLAN), response);

            var expectedResponse = facetsResponse(
                getNamedApiBucketResponse(
                    facetBucketResponse,
                    List.of(
                        new FacetBucketResponse(PLAN_ID1, PLAN_NAME1, null, innerBucket1.measures()),
                        new FacetBucketResponse(PLAN_ID2, PLAN_NAME2, null, innerBucket2.measures())
                    )
                )
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_resolve_plan_names_once_for_the_whole_bucket_tree() {
            var apiBucket1 = newUnnamedBucketResponse(API_ID1, List.of(newUnnamedBucketResponse(PLAN_ID1)));
            var apiBucket2 = newUnnamedBucketResponse(API_ID2, List.of(newUnnamedBucketResponse(PLAN_ID2)));
            var response = facetsResponse(apiBucket1, apiBucket2);

            processor.mapBucketNames(context, List.of(API, PLAN), response);

            verify(filterValueNameResolver).resolveNames(
                eq("DEFAULT"),
                eq(FilterSpec.Name.PLAN),
                argThat(ids -> Set.copyOf(ids).equals(Set.of(PLAN_ID1, PLAN_ID2)))
            );
            verifyNoMoreInteractions(filterValueNameResolver);
        }

        @Test
        void should_keep_the_ids_when_the_name_lookup_fails() {
            when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.PLAN), anyList())).thenThrow(
                new IllegalStateException("plan repository unavailable")
            );
            var bucket = newUnnamedBucketResponse(PLAN_ID1);
            var response = facetsResponse(bucket);

            var mappedResponse = processor.mapBucketNames(context, List.of(PLAN), response);

            assertThat(mappedResponse).isEqualTo(facetsResponse(getBucketResponseWithDefaultName(bucket)));
        }

        @Test
        void should_resolve_names_when_the_context_has_no_environment() {
            var contextWithoutEnvironment = new AnalyticsQueryContext(
                AuditInfo.builder().build(),
                new ExecutionContext("DEFAULT"),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            );
            when(filterValueNameResolver.resolveNames(isNull(), eq(FilterSpec.Name.PLAN), anyList())).thenReturn(
                Map.of(PLAN_ID1, PLAN_NAME1)
            );
            var bucket = newUnnamedBucketResponse(PLAN_ID1);
            var response = facetsResponse(bucket);

            var mappedResponse = processor.mapBucketNames(contextWithoutEnvironment, List.of(PLAN), response);

            assertThat(mappedResponse).isEqualTo(facetsResponse(new FacetBucketResponse(PLAN_ID1, PLAN_NAME1, null, bucket.measures())));
        }

        @Test
        void should_update_single_api_name() {
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = facetsResponse(getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_api_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(API_ID1);
            var facetBucketResponse2 = newUnnamedBucketResponse(API_ID2);
            var response = facetsResponse(facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = facetsResponse(
                getNamedApiBucketResponse(facetBucketResponse1),
                getNamedApiBucketResponse(facetBucketResponse2)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_handle_unknown_api_names() {
            var apiId = "unknown-id";
            var facetBucketResponse = newUnnamedBucketResponse(apiId);
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = facetsResponse(getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_application_name_for_id_1() {
            var facetBucketResponse = newUnnamedBucketResponse("1");
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(getNamedApplicationBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_single_application_name() {
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(getNamedApplicationBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_application_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(APPLICATION_ID1);
            var facetBucketResponse2 = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = facetsResponse(facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(
                getNamedApplicationBucketResponse(facetBucketResponse1),
                getNamedApplicationBucketResponse(facetBucketResponse2)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_handle_unknown_application_names() {
            var applicationId = "unknown-id";
            var facetBucketResponse = newUnnamedBucketResponse(applicationId);
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(getNamedApplicationBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_api_and_application_facets() {
            var innerBucket1 = newUnnamedBucketResponse(APPLICATION_ID1);
            var innerBucket2 = newUnnamedBucketResponse(APPLICATION_ID2);
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1, List.of(innerBucket1, innerBucket2));

            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API, APPLICATION), response);

            var innerNamedBucket1 = getNamedApplicationBucketResponse(innerBucket1);
            var innerNamedBucket2 = getNamedApplicationBucketResponse(innerBucket2);
            var expectedResponse = facetsResponse(
                getNamedApiBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_application_and_api_facets() {
            var innerBucket1 = newUnnamedBucketResponse(API_ID1);
            var innerBucket2 = newUnnamedBucketResponse(API_ID2);
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1, List.of(innerBucket1, innerBucket2));

            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION, API), response);

            var innerNamedBucket1 = getNamedApiBucketResponse(innerBucket1);
            var innerNamedBucket2 = getNamedApiBucketResponse(innerBucket2);
            var expectedResponse = facetsResponse(
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_application_and_http_status_facets() {
            var innerBucket1 = newUnnamedBucketResponse("200");
            var innerBucket2 = newUnnamedBucketResponse("204");
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1, List.of(innerBucket1, innerBucket2));

            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION, HTTP_STATUS), response);

            var innerNamedBucket1 = new FacetBucketResponse("200", "200 OK", null, innerBucket1.measures());
            var innerNamedBucket2 = new FacetBucketResponse("204", "204 No Content", null, innerBucket2.measures());
            var expectedResponse = facetsResponse(
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_http_status_code_group_facet_buckets() {
            var bucket1 = newStatusCodeRangeBucketResponse("200-299");
            var bucket2 = newStatusCodeRangeBucketResponse("400-499");
            var response = facetsResponse(bucket1, bucket2);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_STATUS_CODE_GROUP), response);

            var expected1 = new FacetBucketResponse("200-299", "2xx Success", null, bucket1.measures());
            var expected2 = new FacetBucketResponse("400-499", "4xx Client Error", null, bucket2.measures());
            var expectedResponse = facetsResponse(expected1, expected2);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_common_http_status_codes_to_labeled_names() {
            var bucket1 = newUnnamedBucketResponse("404");
            var bucket2 = newUnnamedBucketResponse("500");
            var response = facetsResponse(bucket1, bucket2);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_STATUS), response);

            var expected1 = new FacetBucketResponse("404", "404 Not Found", null, bucket1.measures());
            var expected2 = new FacetBucketResponse("500", "500 Internal Server Error", null, bucket2.measures());
            var expectedResponse = facetsResponse(expected1, expected2);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_fallback_to_raw_key_for_unknown_http_status() {
            var bucket = newUnnamedBucketResponse("999");
            var response = facetsResponse(bucket);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_STATUS), response);

            var expected = new FacetBucketResponse("999", "999 Unknown Status", null, bucket.measures());
            var expectedResponse = facetsResponse(expected);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_normalize_synthetic_netty_status_code_without_redundant_suffix() {
            var bucket = newUnnamedBucketResponse("418");
            var response = facetsResponse(bucket);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_STATUS), response);

            var expected = new FacetBucketResponse("418", "418 Client Error", null, bucket.measures());
            var expectedResponse = facetsResponse(expected);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_http_method_codes_to_names() {
            var bucket1 = newUnnamedBucketResponse("3");
            var bucket2 = newUnnamedBucketResponse("7");
            var response = facetsResponse(bucket1, bucket2);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_METHOD), response);

            var expected1 = new FacetBucketResponse("3", "GET", null, bucket1.measures());
            var expected2 = new FacetBucketResponse("7", "POST", null, bucket2.measures());
            var expectedResponse = facetsResponse(expected1, expected2);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_multiple_metrics() {
            var bucket1 = newUnnamedBucketResponse(API_ID1);
            var bucket2 = newUnnamedBucketResponse(API_ID2);
            var bucket3 = newUnnamedBucketResponse(API_ID2);

            var response = new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(bucket1), List.of()),
                    new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(bucket2, bucket3), List.of())
                )
            );

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(getNamedApiBucketResponse(bucket1)), List.of()),
                    new MetricFacetsResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        null,
                        List.of(getNamedApiBucketResponse(bucket2), getNamedApiBucketResponse(bucket3)),
                        List.of()
                    )
                )
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_keep_the_filters_the_search_skipped() {
            var response = facetsResponse(newUnnamedBucketResponse(API_ID1)).withIgnoredFilters(List.of(FilterSpec.Name.NATIVE_CLIENT_ID));

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            assertThat(mappedResponse.metrics().get(0).ignoredFilters()).isEqualTo(List.of(FilterSpec.Name.NATIVE_CLIENT_ID));
        }

        FacetsResponse facetsResponse(FacetBucketResponse... buckets) {
            return new FacetsResponse(
                List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, Arrays.asList(buckets), List.of()))
            );
        }
    }

    @Nested
    class TimeSeries {

        @Test
        void should_map_unnamed_time_series_to_the_key() {
            var facetBucketResponse = newUnnamedBucketResponse("api.example.com");

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(HOST), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getBucketResponseWithDefaultName(facetBucketResponse)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_plan_names() {
            var facetBucketResponse = newUnnamedBucketResponse(PLAN_ID1);

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(PLAN), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                new FacetBucketResponse(PLAN_ID1, PLAN_NAME1, null, facetBucketResponse.measures())
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_api_product_names() {
            var facetBucketResponse = newUnnamedBucketResponse(API_PRODUCT_ID1);

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API_PRODUCT), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                new FacetBucketResponse(API_PRODUCT_ID1, API_PRODUCT_NAME1, null, facetBucketResponse.measures())
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_name_nested_plans_of_every_time_bucket_with_a_single_lookup() {
            var firstBucket = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(newUnnamedBucketResponse(API_ID1, List.of(newUnnamedBucketResponse(PLAN_ID1)))),
                null
            );
            var secondBucket = new TimeSeriesBucketResponse(
                "2024-01-01T00:01:00.000Z",
                null,
                FIXED_TIMESTAMP + 60_000,
                List.of(newUnnamedBucketResponse(API_ID2, List.of(newUnnamedBucketResponse(PLAN_ID2)))),
                null
            );
            var response = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(firstBucket, secondBucket), List.of()))
            );

            var mappedResponse = processor.mapBucketNames(context, List.of(API, PLAN), response);

            var namedPlans = mappedResponse
                .metrics()
                .getFirst()
                .buckets()
                .stream()
                .flatMap(timeBucket -> timeBucket.buckets().stream())
                .flatMap(apiBucket -> apiBucket.buckets().stream())
                .map(FacetBucketResponse::name)
                .toList();
            assertThat(namedPlans).isEqualTo(List.of(PLAN_NAME1, PLAN_NAME2));
            verify(filterValueNameResolver).resolveNames(
                eq("DEFAULT"),
                eq(FilterSpec.Name.PLAN),
                argThat(ids -> Set.copyOf(ids).equals(Set.of(PLAN_ID1, PLAN_ID2)))
            );
            verifyNoMoreInteractions(filterValueNameResolver);
        }

        @Test
        void should_map_http_status_code_group_buckets() {
            var facetBucketResponse = newStatusCodeRangeBucketResponse("200-299");

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var expectedFacetsResponse = new FacetBucketResponse(
                "200-299",
                "2xx Success",
                null,
                List.of(new Measure(COUNT, FIXED_COUNT_VALUE))
            );

            var expectedResponse = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, expectedFacetsResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_STATUS_CODE_GROUP), response);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_single_api_name() {
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_api_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(API_ID1);
            var facetBucketResponse2 = newUnnamedBucketResponse(API_ID2);
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApiBucketResponse(facetBucketResponse1),
                getNamedApiBucketResponse(facetBucketResponse2)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_handle_unknown_api_names() {
            var facetBucketResponse = newUnnamedBucketResponse("unknown-id");
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_application_name_for_id_1() {
            var facetBucketResponse = newUnnamedBucketResponse("1");
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_single_application_name() {
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_application_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(APPLICATION_ID1);
            var facetBucketResponse2 = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse1),
                getNamedApplicationBucketResponse(facetBucketResponse2)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_handle_unknown_application_names() {
            String applicationId = "unknown-id";
            var facetBucketResponse = newUnnamedBucketResponse(applicationId);
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION), response);

            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_api_and_application_facets() {
            var innerBucket1 = newUnnamedBucketResponse(APPLICATION_ID1);
            var innerBucket2 = newUnnamedBucketResponse(APPLICATION_ID2);
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1, List.of(innerBucket1, innerBucket2));

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(API, APPLICATION), response);

            var innerNamedBucket1 = getNamedApplicationBucketResponse(innerBucket1);
            var innerNamedBucket2 = getNamedApplicationBucketResponse(innerBucket2);
            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApiBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_application_and_api_facets() {
            var innerBucket1 = newUnnamedBucketResponse(API_ID1);
            var innerBucket2 = newUnnamedBucketResponse(API_ID2);
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1, List.of(innerBucket1, innerBucket2));

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION, API), response);

            var innerNamedBucket1 = getNamedApiBucketResponse(innerBucket1);
            var innerNamedBucket2 = getNamedApiBucketResponse(innerBucket2);
            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_application_and_http_status_facets() {
            var innerBucket1 = newUnnamedBucketResponse("200");
            var innerBucket2 = newUnnamedBucketResponse("204");
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1, List.of(innerBucket1, innerBucket2));

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION, HTTP_STATUS), response);

            var innerNamedBucket1 = new FacetBucketResponse("200", "200 OK", null, innerBucket1.measures());
            var innerNamedBucket2 = new FacetBucketResponse("204", "204 No Content", null, innerBucket2.measures());
            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_fallback_to_raw_key_for_unknown_http_status_in_time_series() {
            var bucket = newUnnamedBucketResponse("999");
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, bucket);

            var mappedResponse = processor.mapBucketNames(context, List.of(HTTP_STATUS), response);

            var expected = new FacetBucketResponse("999", "999 Unknown Status", null, bucket.measures());
            var expectedResponse = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, expected);

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_multiple_metrics() {
            var bucket1 = newUnnamedBucketResponse(API_ID1);
            var bucket2 = newUnnamedBucketResponse(API_ID2);
            var bucket3 = newUnnamedBucketResponse(API_ID2);

            TimeSeriesBucketResponse timeSeriesBucketResponse1 = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(bucket1),
                null
            );
            TimeSeriesBucketResponse timeSeriesBucketResponse2 = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(bucket2, bucket3),
                null
            );
            var response = new TimeSeriesResponse(
                List.of(
                    new TimeSeriesMetricResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        null,
                        List.of(timeSeriesBucketResponse1, timeSeriesBucketResponse2),
                        List.of()
                    )
                )
            );

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            TimeSeriesBucketResponse namedBucketResponse1 = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(getNamedApiBucketResponse(bucket1)),
                null
            );
            TimeSeriesBucketResponse namedBucketResponse2 = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(getNamedApiBucketResponse(bucket2), getNamedApiBucketResponse(bucket3)),
                null
            );
            var expectedResponse = new TimeSeriesResponse(
                List.of(
                    new TimeSeriesMetricResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        null,
                        List.of(namedBucketResponse1, namedBucketResponse2),
                        List.of()
                    )
                )
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_keep_the_filters_the_search_skipped() {
            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, newUnnamedBucketResponse(API_ID1)).withIgnoredFilters(
                List.of(FilterSpec.Name.NATIVE_CLIENT_ID)
            );

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            assertThat(mappedResponse.metrics().get(0).ignoredFilters()).isEqualTo(List.of(FilterSpec.Name.NATIVE_CLIENT_ID));
        }

        TimeSeriesResponse timeSeriesResponse(String key, long timestamp, FacetBucketResponse... buckets) {
            List<Measure> measures = null;

            if (buckets.length == 0) {
                measures = List.of(new Measure(COUNT, FIXED_COUNT_VALUE));
            }
            TimeSeriesBucketResponse timeSeriesBucketResponse = new TimeSeriesBucketResponse(
                key,
                null,
                timestamp,
                Arrays.asList(buckets),
                measures
            );
            return new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(timeSeriesBucketResponse), List.of()))
            );
        }
    }

    @Nested
    class DbFallback {

        @Test
        void should_use_pre_loaded_api_names_and_skip_db_for_facets() {
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            var response = new FacetsResponse(
                List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(facetBucketResponse), List.of()))
            );

            processor.mapBucketNames(context, List.of(API), response);

            verify(filterValueNameResolver, never()).resolveNames(any(), eq(FilterSpec.Name.API), anyList());
        }

        @Test
        void should_load_api_names_from_db_when_not_pre_loaded_for_facets() {
            var emptyApiNamesContext = new AnalyticsQueryContext(
                AuditInfo.builder().build(),
                new ExecutionContext("DEFAULT", "DEFAULT"),
                Set.of(API_ID1, API_ID2),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            );

            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            var response = new FacetsResponse(
                List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(facetBucketResponse), List.of()))
            );

            var mappedResponse = processor.mapBucketNames(emptyApiNamesContext, List.of(API), response);

            verify(filterValueNameResolver).resolveNames(eq("DEFAULT"), eq(FilterSpec.Name.API), eq(List.of(API_ID1)));

            var expectedResponse = new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        null,
                        List.of(getNamedApiBucketResponse(facetBucketResponse)),
                        List.of()
                    )
                )
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_use_pre_loaded_application_names_and_skip_db_for_facets() {
            var contextWithApps = new AnalyticsQueryContext(
                context.auditInfo(),
                context.executionContext(),
                context.authorizedApiIds(),
                apiNamesById,
                applicationNamesById,
                Map.of(),
                Optional.empty()
            );

            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = new FacetsResponse(
                List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(facetBucketResponse), List.of()))
            );

            processor.mapBucketNames(contextWithApps, List.of(APPLICATION), response);

            verify(filterValueNameResolver, never()).resolveNames(any(), eq(FilterSpec.Name.APPLICATION), anyList());
        }

        @Test
        void should_load_api_names_from_db_when_not_pre_loaded_for_time_series() {
            var emptyApiNamesContext = new AnalyticsQueryContext(
                AuditInfo.builder().build(),
                new ExecutionContext("DEFAULT", "DEFAULT"),
                Set.of(API_ID1, API_ID2),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty()
            );

            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            TimeSeriesBucketResponse timeSeriesBucketResponse = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(facetBucketResponse),
                null
            );
            var response = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(timeSeriesBucketResponse), List.of()))
            );

            var mappedResponse = processor.mapBucketNames(emptyApiNamesContext, List.of(API), response);

            verify(filterValueNameResolver).resolveNames(eq("DEFAULT"), eq(FilterSpec.Name.API), eq(List.of(API_ID1)));

            TimeSeriesBucketResponse expectedTimeBucket = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(getNamedApiBucketResponse(facetBucketResponse)),
                null
            );
            var expectedResponse = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(expectedTimeBucket), List.of()))
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_use_pre_loaded_api_names_and_skip_db_for_time_series() {
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            TimeSeriesBucketResponse timeSeriesBucketResponse = new TimeSeriesBucketResponse(
                FIXED_TIME_KEY,
                null,
                FIXED_TIMESTAMP,
                List.of(facetBucketResponse),
                null
            );
            var response = new TimeSeriesResponse(
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(timeSeriesBucketResponse), List.of()))
            );

            processor.mapBucketNames(context, List.of(API), response);

            verify(filterValueNameResolver, never()).resolveNames(any(), eq(FilterSpec.Name.API), anyList());
        }
    }

    private FacetBucketResponse newUnnamedBucketResponse(String apiId) {
        return newUnnamedBucketResponse(apiId, null);
    }

    private FacetBucketResponse newStatusCodeRangeBucketResponse(String rangeKey) {
        List<Measure> measures = List.of(new Measure(COUNT, FIXED_COUNT_VALUE));
        return new FacetBucketResponse(rangeKey, null, null, measures);
    }

    private FacetBucketResponse newUnnamedBucketResponse(String apiId, List<FacetBucketResponse> innerBuckets) {
        List<Measure> measures = null;
        if (CollectionUtils.isEmpty(innerBuckets)) {
            measures = List.of(new Measure(COUNT, FIXED_COUNT_VALUE));
        }
        return new FacetBucketResponse(apiId, null, innerBuckets, measures);
    }

    private FacetBucketResponse getNamedApiBucketResponse(FacetBucketResponse bucketResponse) {
        return getNamedApiBucketResponse(bucketResponse, bucketResponse.buckets());
    }

    private FacetBucketResponse getNamedApiBucketResponse(FacetBucketResponse bucketResponse, List<FacetBucketResponse> innerBuckets) {
        var key = bucketResponse.key();
        var name = apiNamesById.getOrDefault(key, key);
        return new FacetBucketResponse(key, name, innerBuckets, bucketResponse.measures());
    }

    private FacetBucketResponse getNamedApplicationBucketResponse(FacetBucketResponse bucketResponse) {
        return getNamedApplicationBucketResponse(bucketResponse, bucketResponse.buckets());
    }

    private FacetBucketResponse getNamedApplicationBucketResponse(
        FacetBucketResponse bucketResponse,
        List<FacetBucketResponse> innerBuckets
    ) {
        var key = bucketResponse.key();
        var name = key.equals("1") ? "Unknown" : applicationNamesById.getOrDefault(key, key);
        return new FacetBucketResponse(key, name, innerBuckets, bucketResponse.measures());
    }

    private FacetBucketResponse getBucketResponseWithDefaultName(FacetBucketResponse bucketResponse) {
        var key = bucketResponse.key();
        return new FacetBucketResponse(key, key, bucketResponse.buckets(), bucketResponse.measures());
    }
}
