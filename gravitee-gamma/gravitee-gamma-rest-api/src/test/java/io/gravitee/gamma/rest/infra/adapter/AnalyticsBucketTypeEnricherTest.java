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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsBucketTypeEnricherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_add_group_and_leaf_types_to_multi_level_facets_response() {
        var leaf = new FacetBucketResponse("200", "200 OK", null, List.of(new Measure(MetricSpec.Measure.COUNT, 883)));
        var group = new FacetBucketResponse("200", "200", List.of(leaf), null);
        var response = new FacetsResponse(
            List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(group)))
        );

        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichFacetsResponse(node);

        assertThat(node.at("/metrics/0/buckets/0/type").asText()).isEqualTo("GROUP");
        assertThat(node.at("/metrics/0/buckets/0/buckets/0/type").asText()).isEqualTo("LEAF");
        assertThat(node.at("/metrics/0/buckets/0/buckets/0/measures/0/value").asInt()).isEqualTo(883);
    }

    @Test
    void should_add_leaf_type_to_flat_facets_response() {
        var leaf = new FacetBucketResponse("200", "200 OK", null, List.of(new Measure(MetricSpec.Measure.COUNT, 42)));
        var response = new FacetsResponse(
            List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(leaf)))
        );

        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichFacetsResponse(node);

        assertThat(node.at("/metrics/0/buckets/0/type").asText()).isEqualTo("LEAF");
    }

    @Test
    void should_treat_empty_nested_buckets_as_leaf() {
        var leaf = new FacetBucketResponse("200-299", "2xx Success", List.of(), List.of(new Measure(MetricSpec.Measure.COUNT, 0)));
        var response = new FacetsResponse(
            List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(leaf)))
        );

        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichFacetsResponse(node);

        assertThat(node.at("/metrics/0/buckets/0/type").asText()).isEqualTo("LEAF");
    }

    @Test
    void should_add_group_and_leaf_types_to_time_series_response() {
        var facetLeaf = new FacetBucketResponse("APP-1", "App 1", null, List.of(new Measure(MetricSpec.Measure.COUNT, 5)));
        var timeBucket = new TimeSeriesBucketResponse("2026-06-10T00:00:00Z", null, 1_749_513_600_000L, List.of(facetLeaf), null);
        var response = new TimeSeriesResponse(
            List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(timeBucket)))
        );

        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichTimeSeriesResponse(node);

        assertThat(node.at("/metrics/0/buckets/0/type").asText()).isEqualTo("GROUP");
        assertThat(node.at("/metrics/0/buckets/0/buckets/0/type").asText()).isEqualTo("LEAF");
    }
}
