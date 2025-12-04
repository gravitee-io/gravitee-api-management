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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class FacetNamesMappingTest extends NamesPostprocessorImplAbstract {

    @Nested
    class UnmappableFacetsTest {

        @Test
        void should_map_unnamed_facets_to_the_key() {
            var facetBucketResponse = newUnnamedBucketResponse("planId");
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(PLAN), response);

            var expectedResponse = facetsResponse(getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }
    }

    @Nested
    class MapApiFacets {

        @Test
        void should_update_single_api_name() {
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1);
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

            var expectedResponse = facetsResponse(getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_api_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(API_ID1);
            var facetBucketResponse2 = newUnnamedBucketResponse(API_ID2);
            var response = facetsResponse(facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

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

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

            var expectedResponse = facetsResponse(getNamedApiBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }
    }

    @Nested
    class MapApplicationFacets {

        @Test
        void should_update_application_name_for_id_1() {
            var facetBucketResponse = newUnnamedBucketResponse("1");
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(getNamedApplicationBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_single_application_name() {
            var facetBucketResponse = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(getNamedApplicationBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_application_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(APPLICATION_ID1);
            var facetBucketResponse2 = newUnnamedBucketResponse(APPLICATION_ID1);
            var response = facetsResponse(facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(APPLICATION), response);

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

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(APPLICATION), response);

            var expectedResponse = facetsResponse(getNamedApplicationBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }
    }

    @Nested
    class MapMultipleFacets {

        @Test
        void should_update_api_and_application_facets() {
            var innerBucket1 = newUnnamedBucketResponse(APPLICATION_ID1);
            var innerBucket2 = newUnnamedBucketResponse(APPLICATION_ID2);
            var facetBucketResponse = newUnnamedBucketResponse(API_ID1, List.of(innerBucket1, innerBucket2));

            var response = facetsResponse(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API, APPLICATION), response);

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

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(APPLICATION, API), response);

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

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(APPLICATION, HTTP_STATUS), response);

            var innerNamedBucket1 = getBucketResponseWithDefaultName(innerBucket1);
            var innerNamedBucket2 = getBucketResponseWithDefaultName(innerBucket2);
            var expectedResponse = facetsResponse(
                getNamedApplicationBucketResponse(facetBucketResponse, List.of(innerNamedBucket1, innerNamedBucket2))
            );

            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }
    }

    @Nested
    class MultipleMetricsMappingTest {

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

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

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
    }

    FacetsResponse facetsResponse(FacetBucketResponse... buckets) {
        return new FacetsResponse(List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, Arrays.asList(buckets))));
    }
}
