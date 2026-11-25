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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetail;
import io.gravitee.repository.log.v4.model.analytics.ApiMetricsDetailQuery;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The custom metrics an assign-metrics policy writes, read back on the log detail, over the
 * "custom-metrics-logs-api-001" block of {@code freemarker-v4-analytics/v4-metrics.ftl}: one request carrying both
 * families, one carrying a custom name as a nested object, one carrying additional metrics only.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryCustomMetricsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API = "custom-metrics-logs-api-001";

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private AnalyticsElasticsearchRepository analyticsRepository;

    @Test
    void should_carry_the_custom_metrics_on_the_log_detail_apart_from_the_additional_metrics() {
        var detail = detail("cm-log-001");

        assertThat(detail.getCustomMetrics()).containsExactlyInAnyOrderEntriesOf(Map.of("customer.tier", "gold", "region", "eu-west"));
        assertThat(detail.getAdditionalMetrics()).containsExactly(Map.entry("keyword_fraud-check_tier", "high"));
    }

    @Test
    void should_join_a_custom_name_written_as_a_nested_object_on_the_log_detail() {
        assertThat(detail("cm-log-002").getCustomMetrics()).containsExactly(Map.entry("order.channel", "web"));
    }

    @Test
    void should_leave_the_custom_metrics_null_on_the_log_detail_of_a_request_without_any() {
        assertThat(detail("cm-log-003").getCustomMetrics()).isNull();
    }

    private ApiMetricsDetail detail(String requestId) {
        return analyticsRepository.findApiMetricsDetail(queryContext, new ApiMetricsDetailQuery(API, requestId)).orElseThrow();
    }
}
