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

import static io.gravitee.apim.core.analytics_engine.model.FacetSpec.Name.API;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.apim.core.analytics_engine.model.*;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
@ExtendWith(SpringExtension.class)
class NamedPostProcessorImplTest {

    @Inject
    NamesPostProcessorImpl namesPostProcessorImpl;

    @Nested
    class FacetNameMappingTest {

        final String apiId1 = "id1";
        final String apiName1 = "api1";

        final String apiId2 = "id2";
        final String apiName2 = "api2";

        final Map<String, String> apiNamesById = Map.of(apiId1, apiName1, apiId2, apiName2);

        final MetricsContext context = new MetricsContext(null).withApiNamesById(apiNamesById);

        final Random random = new Random();

        @Test
        void should_update_single_api_name() {
            var facetBucketResponse = newUnnamedBucketResponse(apiId1);
            var response = responseWithBuckets(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

            var expectedResponse = responseWithBuckets(getNamedBucketResponse(facetBucketResponse));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_update_multiple_api_names() {
            var facetBucketResponse1 = newUnnamedBucketResponse(apiId1);
            var facetBucketResponse2 = newUnnamedBucketResponse(apiId2);
            var response = responseWithBuckets(facetBucketResponse1, facetBucketResponse2);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

            var expectedResponse = responseWithBuckets(
                getNamedBucketResponse(facetBucketResponse1),
                getNamedBucketResponse(facetBucketResponse2)
            );
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        @Test
        void should_not_update_unknown_api_names() {
            String apiId = "unknown-id";
            var facetBucketResponse = newUnnamedBucketResponse(apiId);
            var response = responseWithBuckets(facetBucketResponse);

            var mappedResponse = namesPostProcessorImpl.mapNames(context, List.of(API), response);

            var expectedResponse = responseWithBuckets(getNamedBucketResponse(facetBucketResponse, apiId));
            assertThat(mappedResponse).isEqualTo(expectedResponse);
        }

        FacetsResponse responseWithBuckets(FacetBucketResponse... buckets) {
            return new FacetsResponse(List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, Arrays.asList(buckets))));
        }

        FacetBucketResponse newUnnamedBucketResponse(String apiId) {
            return new FacetBucketResponse(apiId, null, null, List.of(new Measure(MetricSpec.Measure.COUNT, random.nextInt(100))));
        }

        FacetBucketResponse getNamedBucketResponse(FacetBucketResponse bucketResponse) {
            var key = bucketResponse.key();
            var name = apiNamesById.get(key);
            return getNamedBucketResponse(bucketResponse, name);
        }

        FacetBucketResponse getNamedBucketResponse(FacetBucketResponse bucketResponse, String name) {
            var key = bucketResponse.key();
            return new FacetBucketResponse(key, name, bucketResponse.buckets(), bucketResponse.measures());
        }
    }
}
