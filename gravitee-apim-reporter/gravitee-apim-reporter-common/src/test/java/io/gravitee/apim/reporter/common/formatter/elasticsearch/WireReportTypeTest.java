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
import io.gravitee.apim.reporter.common.MetricsType;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.metric.event.ApiEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.ApplicationEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.BaseEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.OperationEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.TopicEventMetrics;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WireReportTypeTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Node NODE = when(mock(Node.class).id()).thenReturn("gateway-id").getMock();
    private static final long TIMESTAMP = 1751015211866L;

    @ParameterizedTest(name = "es{0}x {1} is exported as {2}")
    @MethodSource("trees_and_reportables")
    void export_render_carries_the_declared_report_type(int elasticsearchVersion, String name, Reportable report, String expectedType)
        throws Exception {
        String rendered = new ElasticsearchFormatter<Reportable>(NODE, elasticsearchVersion).format0(report, Map.of()).toString();

        assertThat(JSON.readTree(rendered).get("type").asText()).isEqualTo(expectedType);
    }

    @Test
    void every_event_metrics_and_decision_type_registered_in_metrics_type_is_covered() {
        List<Class<?>> covered = reportables()
            .<Class<?>>map(arguments -> arguments.get()[1].getClass())
            .toList();
        List<Class<?>> registered = Arrays.stream(MetricsType.values())
            .<Class<?>>map(MetricsType::getClazz)
            .filter(clazz -> BaseEventMetrics.class.isAssignableFrom(clazz) || DecisionReport.class.isAssignableFrom(clazz))
            .toList();

        assertThat(covered).containsExactlyInAnyOrderElementsOf(registered);
    }

    static Stream<Arguments> trees_and_reportables() {
        return Stream.of(7, 8, 9).flatMap(version ->
            reportables().map(arguments -> {
                var values = arguments.get();
                return Arguments.of(version, values[0], values[1], values[2]);
            })
        );
    }

    static Stream<Arguments> reportables() {
        return Stream.of(
            Arguments.of("decision report", decisionReport(), "decisions"),
            Arguments.of("operation event metrics", operationEventMetrics(), "event-metrics"),
            Arguments.of("topic event metrics", topicEventMetrics(), "event-metrics"),
            Arguments.of("application event metrics", applicationEventMetrics(), "event-metrics"),
            Arguments.of("api event metrics", apiEventMetrics(), "event-metrics")
        );
    }

    private static DecisionReport decisionReport() {
        return DecisionReport.builder()
            .timestamp(TIMESTAMP)
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
            .build();
    }

    private static OperationEventMetrics operationEventMetrics() {
        return OperationEventMetrics.builder()
            .timestamp(TIMESTAMP)
            .gatewayId("gw")
            .organizationId("org")
            .environmentId("env")
            .apiId("api")
            .operation("PRODUCE")
            .build();
    }

    private static TopicEventMetrics topicEventMetrics() {
        return TopicEventMetrics.builder()
            .timestamp(TIMESTAMP)
            .gatewayId("gw")
            .organizationId("org")
            .environmentId("env")
            .apiId("api")
            .topic("orders")
            .build();
    }

    private static ApplicationEventMetrics applicationEventMetrics() {
        return ApplicationEventMetrics.builder()
            .timestamp(TIMESTAMP)
            .gatewayId("gw")
            .organizationId("org")
            .environmentId("env")
            .apiId("api")
            .build();
    }

    private static ApiEventMetrics apiEventMetrics() {
        return ApiEventMetrics.builder()
            .timestamp(TIMESTAMP)
            .gatewayId("gw")
            .organizationId("org")
            .environmentId("env")
            .apiId("api")
            .build();
    }
}
