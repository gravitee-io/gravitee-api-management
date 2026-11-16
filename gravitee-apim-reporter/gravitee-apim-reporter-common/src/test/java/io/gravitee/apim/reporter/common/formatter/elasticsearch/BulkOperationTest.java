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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.monitor.Monitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Data streams accept {@code create} only: the action line keeps the index and the document id of an {@code index} one.
 */
class BulkOperationTest extends AbstractFormatterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Node NODE = when(mock(Node.class).id()).thenReturn("gateway-id").getMock();
    private static final Map<String, Object> OPTIONS = Map.of("index", "gravitee-type", "pipeline", "my-pipeline");
    /** Message documents draw a new id each time they are formatted. */
    private static final Set<String> MESSAGES = Set.of("v4.metric.MessageMetrics", "v4.log.MessageLog");

    @Override
    protected Type type() {
        return Type.ELASTICSEARCH;
    }

    static Stream<Arguments> trees_and_reportables() {
        return Stream.of(7, 8, 9).flatMap(version ->
            Stream.of(
                Arguments.of(version, "log.Log", "log.json"),
                Arguments.of(version, "http.Metrics", "metrics.json"),
                Arguments.of(version, "health.EndpointStatus", "endpoint-status.json"),
                Arguments.of(version, "v4.log.Log", "v4/log.json"),
                Arguments.of(version, "v4.metric.Metrics", "v4/metrics.json"),
                Arguments.of(version, "v4.metric.MessageMetrics", "v4/message-metrics.json"),
                Arguments.of(version, "v4.log.MessageLog", "v4/message-log.json"),
                Arguments.of(version, "v4.metric.event.ApiEventMetrics", "v4/api-event-metrics.json"),
                Arguments.of(version, "v4.report.DecisionReport", "v4/decision-report.json")
            )
        );
    }

    @ParameterizedTest(name = "es{0}x {1}")
    @MethodSource("trees_and_reportables")
    void should_create_each_document_with_the_index_and_id_an_index_action_carries(int version, String className, String input)
        throws Exception {
        Reportable reportable = readGiven(input, className);

        JsonNode created = actionLine(new ElasticsearchFormatter<>(NODE, version, BulkOperation.CREATE), reportable);
        JsonNode indexed = actionLine(new ElasticsearchFormatter<>(NODE, version, BulkOperation.INDEX), reportable);

        assertThat(fieldNames(created)).containsExactly("create");
        JsonNode metadata = created.get("create");
        assertThat(fieldNames(metadata)).isEqualTo(fieldNames(single(indexed)));
        assertThat(metadata.get("_index")).isEqualTo(single(indexed).get("_index"));
        assertThat(metadata.path("pipeline")).isEqualTo(single(indexed).path("pipeline"));
        if (!MESSAGES.contains(className)) {
            assertThat(metadata.path("_id")).isEqualTo(single(indexed).path("_id"));
        }
    }

    @ParameterizedTest(name = "es{0}x")
    @MethodSource("versions")
    void should_create_monitor_documents(int version) throws Exception {
        Reportable monitor = readGiven("monitor.json", Monitor.class);

        assertThat(fieldNames(actionLine(new ElasticsearchFormatter<>(NODE, version, BulkOperation.CREATE), monitor))).containsExactly(
            "create"
        );
    }

    @ParameterizedTest(name = "es{0}x")
    @MethodSource("versions")
    void should_index_by_default(int version) throws Exception {
        Reportable metrics = readGiven("metrics.json", "http.Metrics");

        assertThat(fieldNames(actionLine(new ElasticsearchFormatter<>(NODE, version), metrics))).containsExactly("index");
    }

    static Stream<Integer> versions() {
        return Stream.of(7, 8, 9);
    }

    private static JsonNode actionLine(ElasticsearchFormatter<Reportable> formatter, Reportable reportable) throws Exception {
        String firstLine = formatter.format0(reportable, OPTIONS).toString().strip().lines().findFirst().orElseThrow();
        return JSON.readTree(firstLine);
    }

    /** {@code event-metrics} and {@code decisions} were data streams already: they are created in every mode. */
    private static JsonNode single(JsonNode actionLine) {
        return actionLine.elements().next();
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
