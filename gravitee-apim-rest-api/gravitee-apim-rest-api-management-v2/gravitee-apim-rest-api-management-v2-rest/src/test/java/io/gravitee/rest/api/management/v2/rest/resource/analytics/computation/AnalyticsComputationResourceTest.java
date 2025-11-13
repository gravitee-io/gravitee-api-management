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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Bucket;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.BucketLeaf;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Measure;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import jakarta.ws.rs.client.Entity;
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
        }

        @Test
        void should_return_request_count() {
            var response = rootTarget().path("measures").request().post(Entity.json(aRequestCountMeasureRequest()));

            assertThat(response)
                .hasStatus(200)
                .asEntity(MeasuresResponse.class)
                .isEqualTo(
                    new MeasuresResponse().metrics(
                        List.of(
                            new MeasuresResponseMetricsInner()
                                .name(MetricName.HTTP_REQUESTS)
                                .measures(List.of(new Measure().name(MeasureName.COUNT).value(42)))
                        )
                    )
                );
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
        }

        @Test
        void should_return_request_count() {
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
}
