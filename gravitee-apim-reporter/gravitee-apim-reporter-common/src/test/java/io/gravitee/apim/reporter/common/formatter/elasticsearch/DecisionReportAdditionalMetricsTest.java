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
package io.gravitee.apim.reporter.common.formatter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DecisionReportAdditionalMetricsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Node NODE = when(mock(Node.class).id()).thenReturn("gateway-id").getMock();

    @ParameterizedTest(name = "es{0}x")
    @ValueSource(ints = { 7, 8, 9 })
    void should_render_a_report_that_carries_only_multi_valued_keyword_metrics(int elasticsearchVersion) throws Exception {
        var report = DecisionReport.builder()
            .timestamp(1751015211866L)
            .gatewayId("gw")
            .organizationId("org")
            .environmentId("env")
            .apiId("api")
            .eventId("evt-1")
            .phase(DecisionReport.Phase.RESOLVED)
            .decisionPointType(DecisionReport.DECISION_POINT_AUTHZ)
            .decisionPointId("default")
            .outcome(DecisionReport.Outcome.ALLOW)
            .enforced(DecisionReport.Enforced.ALLOW)
            .status(DecisionReport.Status.SUCCESS)
            .additionalMetrics(Set.of(new AdditionalMetric.KeywordListMetric("keyword_authz_tools", List.of("search", "the \"book\""))))
            .build();

        String rendered = new ElasticsearchFormatter<Reportable>(NODE, elasticsearchVersion).format0(report, Map.of()).toString();
        var tools = JSON.readTree(rendered).at("/additional-metrics/keyword_authz_tools");

        assertThat(tools.isArray()).as("additional-metrics.keyword_authz_tools in %s", rendered).isTrue();
        assertThat(JSON.convertValue(tools, List.class)).containsExactly("search", "the \"book\"");
    }
}
