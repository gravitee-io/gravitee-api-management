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
package io.gravitee.apim.reporter.common.formatter.msgpack;

import static io.gravitee.apim.reporter.common.formatter.Mappers.JSON;
import static io.gravitee.apim.reporter.common.formatter.Mappers.MESSAGE_PACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.reporter.common.MetricsType;
import io.gravitee.apim.reporter.common.formatter.AbstractFormatterTest;
import io.gravitee.apim.reporter.common.formatter.FormatterFactory;
import io.gravitee.apim.reporter.common.formatter.FormatterFactoryConfiguration;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.configuration.Rules;
import io.gravitee.reporter.api.monitor.Monitor;
import io.gravitee.reporter.api.v4.log.Log;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The MsgPack output must select the same data as the JSON output for the same rules (APIM-14254).
 *
 * @author Ouahid KHELIFI (ouahid.khelifi at graviteesource.com)
 * @author GraviteeSource Team
 */
class MsgPackFormatterDataSelectionTest extends AbstractFormatterTest {

    private static final FormatterFactory FACTORY = new FormatterFactory(
        mock(Node.class),
        FormatterFactoryConfiguration.builder()
            .rule(
                MetricsType.V4_LOG,
                rules(
                    Set.of("entrypointRequest.headers.x-gravitee-api-key", "endpointResponse.headers", "method"),
                    Map.of("entrypointRequest.uri", "clientUri")
                )
            )
            .rule(MetricsType.V4_METRICS, rules(Set.of("remoteAddress", "userAgent"), Map.of("apiName", "api_name")))
            .rule(MetricsType.NODE_MONITOR, rules(Set.of("*"), Map.of()))
            .build()
    );

    @Override
    protected Type type() {
        return Type.MESSAGE_PACK;
    }

    @Test
    void should_exclude_a_nested_header() throws IOException {
        var headers = formatV4Log().at("/entrypointRequest/headers");

        assertThat(headers.has("x-gravitee-api-key")).isFalse();
        assertThat(headers.has("Accept")).isTrue();
    }

    @Test
    void should_exclude_a_nested_object() throws IOException {
        var endpointResponse = formatV4Log().at("/endpointResponse");

        assertThat(endpointResponse.has("headers")).isFalse();
        assertThat(endpointResponse.get("status").asInt()).isEqualTo(200);
    }

    @Test
    void should_rename_a_nested_field() throws IOException {
        var entrypointRequest = formatV4Log().at("/entrypointRequest");

        assertThat(entrypointRequest.has("uri")).isFalse();
        assertThat(entrypointRequest.get("clientUri").asText()).isEqualTo("/test-v4");
    }

    @Test
    void should_apply_a_root_rule_to_root_fields_only() throws IOException {
        var log = formatV4Log();

        assertThat(log.at("/entrypointRequest/method").asText()).isEqualTo("GET");
        assertThat(log.at("/endpointRequest/method").asText()).isEqualTo("GET");
    }

    @Test
    void should_not_report_a_fully_excluded_report() {
        var monitor = readGiven("monitor.json", Monitor.class);

        assertThat(FACTORY.getFormatter(Type.JSON, MetricsType.NODE_MONITOR).format(monitor)).isNull();
        assertThat(FACTORY.getFormatter(Type.MESSAGE_PACK, MetricsType.NODE_MONITOR).format(monitor)).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "V4_LOG, v4/log.json, v4.log.Log", "V4_METRICS, v4/metrics.json, v4.metric.Metrics" })
    void should_select_the_same_data_as_json(MetricsType metricsType, String given, String className) throws IOException {
        var reportable = readGiven(given, className);

        var json = FACTORY.getFormatter(Type.JSON, metricsType).format(reportable);
        var msgPack = FACTORY.getFormatter(Type.MESSAGE_PACK, metricsType).format(reportable);

        assertThat(toTree(msgPack, MESSAGE_PACK)).usingRecursiveComparison().isEqualTo(toTree(json, JSON));
    }

    private JsonNode formatV4Log() throws IOException {
        var log = readGiven("v4/log.json", Log.class);
        return toTree(FACTORY.getFormatter(Type.MESSAGE_PACK, MetricsType.V4_LOG).format(log), MESSAGE_PACK);
    }

    private static JsonNode toTree(Buffer buffer, ObjectMapper mapper) throws IOException {
        return buffer == null ? null : mapper.readTree(buffer.getBytes());
    }

    private static Rules rules(Set<String> excluded, Map<String, String> renamed) {
        var rules = new Rules();
        rules.setExcludeFields(excluded);
        rules.setIncludeFields(Set.of());
        rules.setRenameFields(renamed);
        return rules;
    }
}
