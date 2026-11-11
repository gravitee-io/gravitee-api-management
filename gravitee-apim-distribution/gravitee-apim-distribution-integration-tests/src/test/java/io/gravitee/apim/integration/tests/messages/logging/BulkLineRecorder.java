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
package io.gravitee.apim.integration.tests.messages.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.reporter.common.formatter.elasticsearch.ElasticsearchFormatter;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.v4.log.Log;
import io.gravitee.reporter.api.v4.log.MessageLog;
import io.gravitee.reporter.api.v4.metric.MessageMetrics;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Formats every reportable into the Elasticsearch 8 bulk lines the reporter would send, at the moment it is reported,
 * with a daily index name.
 */
class BulkLineRecorder {

    record BulkLine(JsonNode action, JsonNode source) {
        String index() {
            return action.at("/index/_index").asText();
        }

        String id() {
            return action.at("/index/_id").asText();
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DAY = "2026.11.10";

    private final List<String> raw = new CopyOnWriteArrayList<>();

    BulkLineRecorder(Node node, FakeReporter reporter) {
        ElasticsearchFormatter<Reportable> formatter = new ElasticsearchFormatter<>(node, 8);
        reporter.setReportableHandler(reportable -> {
            Buffer bulk = formatter.format0(reportable, Map.of("index", "gravitee-" + type(reportable) + "-" + DAY));
            if (bulk != null) {
                raw.add(bulk.toString().strip());
            }
        });
    }

    List<BulkLine> lines(String type) {
        List<BulkLine> found = new ArrayList<>();
        for (String bulk : raw) {
            String[] lines = bulk.split("\n", 2);
            try {
                BulkLine line = new BulkLine(MAPPER.readTree(lines[0]), MAPPER.readTree(lines[1]));
                if (line.index().equals("gravitee-" + type + "-" + DAY)) {
                    found.add(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return found;
    }

    private static String type(Reportable reportable) {
        return switch (reportable) {
            case Metrics ignored -> "v4-metrics";
            case Log ignored -> "v4-log";
            case MessageMetrics ignored -> "v4-message-metrics";
            case MessageLog ignored -> "v4-message-log";
            default -> reportable.getClass().getSimpleName();
        };
    }
}
