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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.ApplicationService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    static final int FIXED_COUNT_VALUE = 42;
    static final long FIXED_TIMESTAMP = 1234567890L;
    static final String FIXED_TIME_KEY = "2024-01-01T00:00:00.000Z";

    final Map<String, String> applicationNamesById = Map.of(APPLICATION_ID1, APPLICATION_NAME1, APPLICATION_ID2, APPLICATION_NAME2);

    private final ApplicationService applicationService = mock(ApplicationService.class);

    private final BucketNamesPostProcessor processor = new BucketNamesPostProcessorImpl(applicationService);

    private UserContext context;

    @BeforeEach
    void setUp() {
        var apiNamesById = Map.of(API_ID1, API_NAME1, API_ID2, API_NAME2);

        context = new UserContext(AuditInfo.builder().build()).withApiNamesById(apiNamesById);

        when(applicationService.search(any(), any(), isNull(), isNull())).thenReturn(
            getApplicationListContent(APPLICATION_ID1, APPLICATION_ID2)
        );
    }

    @Nested
    class Facets {

        @Test
        void should_map_unnamed_facets_to_the_key() {
            var facetBucketResponse = newUnnamedBucketResponse("planId");
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(PLAN), response);

            var expectedResponse = facetsResponse(getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
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
        void should_update_api_and_unmappable_facets() {
            var innerBucket1 = newUnnamedBucketResponse("200");
            var innerBucket2 = newUnnamedBucketResponse("204");
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1, List.of(innerBucket1, innerBucket2));

            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION, HTTP_STATUS), response);

            var innerNamedBucket1 = getBucketResponseWithDefaultName(innerBucket1);
            var innerNamedBucket2 = getBucketResponseWithDefaultName(innerBucket2);
            var expectedResponse = facetsResponse(
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_multiple_metrics() {
            var bucket1 = newUnnamedBucketResponse(API_ID1);
            var bucket2 = newUnnamedBucketResponse(API_ID2);
            var bucket3 = newUnnamedBucketResponse(API_ID2);

            var response = new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, List.of(bucket1)),
                    new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, List.of(bucket2, bucket3))
                )
            );

            var mappedResponse = processor.mapBucketNames(context, List.of(API), response);

            var expectedResponse = new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, List.of(getNamedApiBucketResponse(bucket1))),
                    new MetricFacetsResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        List.of(getNamedApiBucketResponse(bucket2), getNamedApiBucketResponse(bucket3))
                    )
                )
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        FacetsResponse facetsResponse(FacetBucketResponse... buckets) {
            return new FacetsResponse(List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, Arrays.asList(buckets))));
        }
    }

    @Nested
    class TimeSeries {

        @Test
        void should_map_unnamed_time_series_to_the_key() {
            var facetBucketResponse = newUnnamedBucketResponse("planId");

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(PLAN), response);

            var expectedResponse = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_map_http_status_code_group_buckets() {
            var facetBucketResponse = newStatusCodeRangeBucketResponse("200-299");

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var expectedFacetsResponse = new FacetBucketResponse("200-299", "2XX", null, List.of(new Measure(COUNT, FIXED_COUNT_VALUE)));

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
        void should_update_api_and_unmappable_facets() {
            var innerBucket1 = newUnnamedBucketResponse("200");
            var innerBucket2 = newUnnamedBucketResponse("204");
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1, List.of(innerBucket1, innerBucket2));

            var response = timeSeriesResponse(FIXED_TIME_KEY, FIXED_TIMESTAMP, facetBucketResponse);

            var mappedResponse = processor.mapBucketNames(context, List.of(APPLICATION, HTTP_STATUS), response);

            var innerNamedBucket1 = getBucketResponseWithDefaultName(innerBucket1);
            var innerNamedBucket2 = getBucketResponseWithDefaultName(innerBucket2);
            var expectedResponse = timeSeriesResponse(
                FIXED_TIME_KEY,
                FIXED_TIMESTAMP,
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

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
                        List.of(timeSeriesBucketResponse1, timeSeriesBucketResponse2)
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
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, List.of(namedBucketResponse1, namedBucketResponse2)))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
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
                List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, List.of(timeSeriesBucketResponse)))
            );
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

    private Page<ApplicationListItem> getApplicationListContent(String... applicationIds) {
        var items = Arrays.stream(applicationIds).map(this::getApplicationListItem).toList();
        return new Page<>(items, 0, items.size(), items.size());
    }

    private ApplicationListItem getApplicationListItem(String applicationId) {
        var item = new ApplicationListItem();
        item.setName(applicationNamesById.get(applicationId));
        item.setId(applicationId);

        return item;
    }

    private FacetBucketResponse getNamedApiBucketResponse(FacetBucketResponse bucketResponse) {
        return getNamedApiBucketResponse(bucketResponse, bucketResponse.buckets());
    }

    private FacetBucketResponse getNamedApiBucketResponse(FacetBucketResponse bucketResponse, List<FacetBucketResponse> innerBuckets) {
        var key = bucketResponse.key();
        var name = context.apiNameById().get().getOrDefault(key, key);
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
