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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMetricsDetailAdapterTest {

    @Test
    void should_carry_the_custom_metrics_apart_from_the_additional_metrics() {
        var detail = ApiMetricsDetail.builder()
            .requestId("request-id")
            .customMetrics(Map.of("customer-tier", "gold"))
            .additionalMetrics(Map.of("keyword_fraud-check_tier", "high"))
            .build();

        var result = ApiMetricsDetailAdapter.INSTANCE.map(detail);

        assertThat(result.getCustomMetrics()).containsExactly(Map.entry("customer-tier", "gold"));
        assertThat(result.getAdditionalMetrics()).containsExactly(Map.entry("keyword_fraud-check_tier", "high"));
    }
}
