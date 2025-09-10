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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.elasticsearch.AbstractAdapterTest;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FindApiMetricsDetailResponseAdapterTest extends AbstractAdapterTest {

    @Nested
    class AdaptFirst {

        @Test
        void should_return_empty_when_no_hit() {
            var result = FindApiMetricsDetailResponseAdapter.adaptFirst(new SearchResponse());
            assertThat(result).isEmpty();
        }

        @Test
        void should_build_api_metrics_detail() {
            final SearchResponse searchResponse = buildSearchHit("api-proxy-v4-metrics.json");
            final Optional<ApiMetricsDetail> apiMetricsDetail = FindApiMetricsDetailResponseAdapter.adaptFirst(searchResponse);

            assertThat(apiMetricsDetail)
                .isEqualTo(
                    Optional.of(
                        ApiMetricsDetail
                            .builder()
                            .timestamp("2025-08-01T17:29:20.385+02:00")
                            .apiId("2ebe3deb-1859-4d5b-be3d-eb1859dd5b16")
                            .requestId("39107cc9-b8bf-4f16-907c-c9b8bf8f16fb")
                            .transactionId("39107cc9-b8bf-4f16-907c-c9b8bf8f16fb")
                            .host("localhost:8082")
                            .applicationId("1")
                            .planId("ccefeab8-2f7c-45dc-afea-b82f7c75dc1a")
                            .gateway("b504bb7b-8b6e-426f-84bb-7b8b6e626f3f")
                            .status(202)
                            .uri("/v4/echo")
                            .requestContentLength(0L)
                            .responseContentLength(276L)
                            .remoteAddress("0:0:0:0:0:0:0:1")
                            .gatewayLatency(3L)
                            .gatewayResponseTime(19L)
                            .endpointResponseTime(16L)
                            .method(HttpMethod.GET)
                            .endpoint("https://api.gravitee.io/echo")
                            .warnings(List.of())
                            .build()
                    )
                );
        }
    }
}
