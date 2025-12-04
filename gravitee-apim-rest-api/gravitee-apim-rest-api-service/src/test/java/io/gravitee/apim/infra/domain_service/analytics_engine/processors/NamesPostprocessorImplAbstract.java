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

import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.COUNT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.ApplicationService;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
@ExtendWith(SpringExtension.class)
public class NamesPostprocessorImplAbstract {

    @Inject
    NamesPostprocessorImpl namesPostProcessorImpl;

    @Inject
    ApplicationService applicationSearchService;

    static final String API_ID1 = "api-id1";
    static final String API_ID2 = "api-id2";

    static final String APPLICATION_ID1 = "app-id1";
    static final String APPLICATION_ID2 = "app-id2";

    final Map<String, String> applicationNamesById = Map.of(APPLICATION_ID1, "app1", APPLICATION_ID2, "app2");

    final Random random = new SecureRandom();

    MetricsContext context;

    @BeforeEach
    void setUp() {
        var apiNamesById = Map.of(API_ID1, "api1", API_ID2, "api2");

        context = new MetricsContext(null).withApiNamesById(apiNamesById);

        when(applicationSearchService.search(any(), any(), isNull(), isNull())).thenReturn(
            getApplicationListContent(APPLICATION_ID1, APPLICATION_ID2)
        );
    }

    // Helper to create single-level unnamed bucket responses
    FacetBucketResponse newUnnamedBucketResponse(String apiId) {
        return newUnnamedBucketResponse(apiId, null);
    }

    // Helper to create unnamed bucket responses with child buckets
    FacetBucketResponse newUnnamedBucketResponse(String apiId, List<FacetBucketResponse> innerBuckets) {
        List<Measure> measures = null;
        if (CollectionUtils.isEmpty(innerBuckets)) {
            measures = List.of(new Measure(COUNT, random.nextInt()));
        }
        return new FacetBucketResponse(apiId, null, innerBuckets, measures);
    }

    @NotNull
    Page<ApplicationListItem> getApplicationListContent(String... applicationIds) {
        var items = Arrays.stream(applicationIds).map(this::getApplicationListItem).toList();
        return new Page(items, 0, items.size(), items.size());
    }

    ApplicationListItem getApplicationListItem(String applicationId) {
        var item = new ApplicationListItem();
        item.setName(applicationNamesById.get(applicationId));
        item.setId(applicationId);

        return item;
    }

    FacetBucketResponse getNamedApiBucketResponse(FacetBucketResponse bucketResponse) {
        return getNamedApiBucketResponse(bucketResponse, bucketResponse.buckets());
    }

    FacetBucketResponse getNamedApiBucketResponse(FacetBucketResponse bucketResponse, List<FacetBucketResponse> innerBuckets) {
        var key = bucketResponse.key();
        var name = context.apiNameById().get().getOrDefault(key, key);
        return new FacetBucketResponse(key, name, innerBuckets, bucketResponse.measures());
    }

    FacetBucketResponse getNamedApplicationBucketResponse(FacetBucketResponse bucketResponse) {
        return getNamedApplicationBucketResponse(bucketResponse, bucketResponse.buckets());
    }

    FacetBucketResponse getNamedApplicationBucketResponse(FacetBucketResponse bucketResponse, List<FacetBucketResponse> innerBuckets) {
        var key = bucketResponse.key();
        var name = key.equals("1") ? "Unknown" : applicationNamesById.getOrDefault(key, key);
        return new FacetBucketResponse(key, name, innerBuckets, bucketResponse.measures());
    }

    // Returns a bucket response with the name set to the key
    FacetBucketResponse getBucketResponseWithDefaultName(FacetBucketResponse bucketResponse) {
        var key = bucketResponse.key();
        return new FacetBucketResponse(key, key, bucketResponse.buckets(), bucketResponse.measures());
    }
}
