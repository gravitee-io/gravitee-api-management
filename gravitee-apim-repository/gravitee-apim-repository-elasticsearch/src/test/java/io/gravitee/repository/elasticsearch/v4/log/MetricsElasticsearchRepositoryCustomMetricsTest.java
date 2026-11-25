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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The custom metrics an assign-metrics policy writes, read back on the logs search rows, over the
 * "custom-metrics-logs-api-001" block of {@code freemarker-v4-analytics/v4-metrics.ftl}: one request carrying both
 * families, one carrying a custom name as a nested object, one carrying additional metrics only.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class MetricsElasticsearchRepositoryCustomMetricsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API = "custom-metrics-logs-api-001";

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Test
    void should_carry_the_custom_metrics_on_each_search_row_apart_from_the_additional_metrics() {
        var rows = searchRows();

        assertThat(rows.get("cm-log-001").getCustomMetrics()).containsExactlyInAnyOrderEntriesOf(
            Map.of("customer.tier", "gold", "region", "eu-west")
        );
        assertThat(rows.get("cm-log-001").getAdditionalMetrics()).containsExactly(Map.entry("keyword_fraud-check_tier", "high"));
        assertThat(rows.get("cm-log-002").getCustomMetrics()).containsExactly(Map.entry("order.channel", "web"));
        assertThat(rows.get("cm-log-003").getCustomMetrics()).isNull();
    }

    private Map<String, Metrics> searchRows() {
        var response = metricsV4Repository.searchMetrics(
            queryContext,
            MetricsQuery.builder().page(1).size(20).filter(MetricsQuery.Filter.builder().apiIds(Set.of(API)).build()).build(),
            List.of(DefinitionVersion.V4)
        );
        assertThat(response.data()).extracting(Metrics::getRequestId).containsExactlyInAnyOrder("cm-log-001", "cm-log-002", "cm-log-003");
        return response.data().stream().collect(Collectors.toMap(Metrics::getRequestId, Function.identity()));
    }
}
