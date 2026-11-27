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
package io.gravitee.gamma.rest.resources.observability.logs.dto;

import static io.gravitee.gamma.rest.resources.observability.ObservabilityOpenApi.propertyNames;
import static io.gravitee.gamma.rest.resources.observability.ObservabilityOpenApi.schemas;
import static io.gravitee.gamma.rest.resources.observability.ObservabilityOpenApi.serializedNames;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogsSchemaTest {

    @ParameterizedTest
    @CsvSource(
        {
            "LogEntry, io.gravitee.gamma.rest.resources.observability.logs.dto.LogEntryDto",
            "LogDetail, io.gravitee.gamma.rest.resources.observability.logs.dto.LogDetailDto",
            "AnalyticsMeasuresRequest, io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsMeasuresRequestDto",
        }
    )
    void should_serialize_exactly_the_properties_of_its_schema(String schemaName, Class<? extends Record> dto) {
        assertThat(serializedNames(dto)).containsExactlyInAnyOrderElementsOf(propertyNames(schemaName));
    }

    /** Both maps are always written, empty for a request without any: a client never has to test for their absence. */
    @ParameterizedTest
    @CsvSource({ "LogEntry", "LogDetail" })
    void should_document_the_custom_metrics_as_a_required_map_of_text_values_next_to_the_required_additional_metrics(String schemaName) {
        JsonNode schema = schemas().path(schemaName);
        JsonNode customMetrics = schema.path("properties").path("customMetrics");

        assertThat(customMetrics.path("type").asText()).isEqualTo("object");
        assertThat(customMetrics.path("additionalProperties").path("type").asText()).isEqualTo("string");
        assertThat(StreamSupport.stream(schema.path("required").spliterator(), false).map(JsonNode::asText)).contains(
            "customMetrics",
            "additionalMetrics"
        );
    }
}
