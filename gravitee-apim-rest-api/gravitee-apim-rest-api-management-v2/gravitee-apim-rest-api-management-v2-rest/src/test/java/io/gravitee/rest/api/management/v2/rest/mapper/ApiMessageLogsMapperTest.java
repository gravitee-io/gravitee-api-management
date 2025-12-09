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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.log.model.MessageLog;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorType;
import io.gravitee.rest.api.management.v2.rest.model.MessageOperation;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchMessageLogsParam;
import io.gravitee.rest.api.model.analytics.SearchMessageLogsFilters;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMessageLogsMapperTest {

    private final ApiMessageLogsMapper mapper = Mappers.getMapper(ApiMessageLogsMapper.class);

    @Nested
    @DisplayName("mapAdditionalParams")
    class MapAdditionalParamsTests {

        @Test
        void should_return_null_when_additional_is_null() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(null);
            assertThat(result).isNull();
        }

        @Test
        void should_return_null_when_additional_is_empty() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(new String[0]);
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @MethodSource("singleFieldTestCases")
        void should_parse_single_field(String[] input, List<String> expectedValues) {
            Map<String, List<String>> result = mapper.mapAdditionalParams(input);
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactlyElementsOf(expectedValues);
        }

        static Stream<Arguments> singleFieldTestCases() {
            return Stream.of(
                Arguments.of(new String[] { "field1;value1" }, List.of("value1")),
                Arguments.of(new String[] { "field1;value1,value2,value3" }, List.of("value1", "value2", "value3"))
            );
        }

        @Test
        void should_parse_multiple_fields() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(
                new String[] { "field1;value1", "field2;value2", "field3;value3" }
            );
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1");
            assertThat(result.get("field2")).containsExactly("value2");
            assertThat(result.get("field3")).containsExactly("value3");
        }

        @Test
        void should_trim_whitespace_from_field_names_and_values() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(new String[] { "  field1  ;  value1  ,  value2  " });
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1", "value2");
        }

        @Test
        void should_filter_out_empty_values() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(new String[] { "field1;value1,,value2, ,value3" });
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1", "value2", "value3");
        }

        @Test
        void should_skip_invalid_format_entries() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(
                new String[] { "field1;value1", "invalid-format", "field2;value2" }
            );
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1");
            assertThat(result.get("field2")).containsExactly("value2");
            assertThat(result.containsKey("invalid-format")).isFalse();
        }

        @Test
        void should_skip_entries_with_empty_field_name() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(new String[] { " ;value1", "field2;value2" });
            assertThat(result).isNotNull();
            assertThat(result.containsKey("")).isFalse();
            assertThat(result.get("field2")).containsExactly("value2");
        }

        @Test
        void should_skip_entries_with_empty_values() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(new String[] { "field1;", "field2;value2" });
            assertThat(result).isNotNull();
            assertThat(result.containsKey("field1")).isFalse();
            assertThat(result.get("field2")).containsExactly("value2");
        }

        @Test
        void should_handle_multiple_values_for_multiple_fields() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(
                new String[] { "int_webhook_resp-status;200,500", "string_webhook_url;https://example.com,https://test.com" }
            );
            assertThat(result).isNotNull();
            assertThat(result.get("int_webhook_resp-status")).containsExactly("200", "500");
            assertThat(result.get("string_webhook_url")).containsExactly("https://example.com", "https://test.com");
        }

        @Test
        void should_merge_values_when_same_field_appears_multiple_times() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(
                new String[] { "field1;value1", "field1;value2", "field1;value3,value4" }
            );
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1", "value2", "value3", "value4");
        }

        @Test
        void should_merge_values_for_same_field_with_different_fields() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(
                new String[] { "field1;value1", "field2;value2", "field1;value3" }
            );
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1", "value3");
            assertThat(result.get("field2")).containsExactly("value2");
        }

        @Test
        void should_skip_null_entries() {
            Map<String, List<String>> result = mapper.mapAdditionalParams(new String[] { "field1;value1", null, "field2;value2" });
            assertThat(result).isNotNull();
            assertThat(result.get("field1")).containsExactly("value1");
            assertThat(result.get("field2")).containsExactly("value2");
        }
    }

    @Nested
    @DisplayName("parseCommaSeparatedString")
    class ParseCommaSeparatedStringTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "", "   " })
        void should_return_null_when_value_is_null_empty_or_whitespace(String value) {
            List<String> result = mapper.parseCommaSeparatedString(value);
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @MethodSource("parseCommaSeparatedStringTestCases")
        void should_parse_comma_separated_string(String input, List<String> expected) {
            List<String> result = mapper.parseCommaSeparatedString(input);
            assertThat(result).containsExactlyElementsOf(expected);
        }

        static Stream<Arguments> parseCommaSeparatedStringTestCases() {
            return Stream.of(
                Arguments.of("value1", List.of("value1")),
                Arguments.of("value1,value2,value3", List.of("value1", "value2", "value3")),
                Arguments.of(" value1 , value2 , value3 ", List.of("value1", "value2", "value3")),
                Arguments.of("value1,,value2, ,value3", List.of("value1", "value2", "value3")),
                Arguments.of("value1,value,with,commas,value2", List.of("value1", "value", "with", "commas", "value2")),
                Arguments.of("  value1  ", List.of("value1"))
            );
        }
    }

    @Nested
    @DisplayName("map SearchMessageLogsParam to SearchMessageLogsFilters")
    class MapSearchMessageLogsParamTests {

        private SearchMessageLogsParam createParam() {
            SearchMessageLogsParam param = new SearchMessageLogsParam();
            param.setFrom(1000L);
            param.setTo(2000L);
            return param;
        }

        @Test
        void should_map_all_fields() {
            SearchMessageLogsParam param = createParam();
            param.setOperation("subscribe");
            param.setConnectorType("entrypoint");
            param.setConnectorId("webhook");
            param.setRequestId("request-123");
            param.setAdditional(new String[] { "int_webhook_resp-status;200,500", "string_webhook_url;https://example.com" });

            SearchMessageLogsFilters filters = mapper.map(param);

            assertThat(filters).isNotNull();
            assertThat(filters.from()).isEqualTo(1000L);
            assertThat(filters.to()).isEqualTo(2000L);
            assertThat(filters.operation()).isEqualTo("subscribe");
            assertThat(filters.connectorType()).isEqualTo("entrypoint");
            assertThat(filters.connectorId()).isEqualTo("webhook");
            assertThat(filters.requestId()).isEqualTo("request-123");
            assertThat(filters.additional()).isNotNull();
            assertThat(filters.additional().get("int_webhook_resp-status")).containsExactly("200", "500");
            assertThat(filters.additional().get("string_webhook_url")).containsExactly("https://example.com");
        }

        @Test
        void should_map_with_null_additional() {
            SearchMessageLogsParam param = createParam();
            param.setAdditional(null);

            SearchMessageLogsFilters filters = mapper.map(param);

            assertThat(filters.additional()).isNull();
        }

        @Test
        void should_map_with_empty_additional_array() {
            SearchMessageLogsParam param = createParam();
            param.setAdditional(new String[0]);

            SearchMessageLogsFilters filters = mapper.map(param);

            assertThat(filters.additional()).isNull();
        }

        @ParameterizedTest
        @MethodSource("additionalFieldsTestCases")
        void should_map_additional_fields(String[] additional, Map<String, List<String>> expected) {
            SearchMessageLogsParam param = createParam();
            param.setAdditional(additional);

            SearchMessageLogsFilters filters = mapper.map(param);

            assertThat(filters.additional()).isNotNull();
            assertThat(filters.additional()).isEqualTo(expected);
        }

        static Stream<Arguments> additionalFieldsTestCases() {
            return Stream.of(
                Arguments.of(new String[] { "int_webhook_resp-status;200" }, Map.of("int_webhook_resp-status", List.of("200"))),
                Arguments.of(new String[] { "int_webhook_resp-status;200,500" }, Map.of("int_webhook_resp-status", List.of("200", "500"))),
                Arguments.of(
                    new String[] { "field1;value1", "field2;value2", "field3;value3" },
                    Map.of("field1", List.of("value1"), "field2", List.of("value2"), "field3", List.of("value3"))
                ),
                Arguments.of(new String[] { "  field1  ;  value1  ,  value2  " }, Map.of("field1", List.of("value1", "value2"))),
                Arguments.of(new String[] { "field1;value1,,value2, ,value3" }, Map.of("field1", List.of("value1", "value2", "value3")))
            );
        }

        @Test
        void should_skip_invalid_format_entries() {
            SearchMessageLogsParam param = createParam();
            param.setAdditional(new String[] { "field1;value1", "invalid-format", "field2;value2" });

            SearchMessageLogsFilters filters = mapper.map(param);

            assertThat(filters.additional()).isNotNull();
            assertThat(filters.additional().get("field1")).containsExactly("value1");
            assertThat(filters.additional().get("field2")).containsExactly("value2");
            assertThat(filters.additional().containsKey("invalid-format")).isFalse();
        }

        @ParameterizedTest
        @CsvSource(delimiterString = ";", value = { "true;true", "false;false", ";" })
        void should_map_requiresAdditional(Boolean input, Boolean expected) {
            SearchMessageLogsParam param = createParam();
            param.setRequiresAdditional(input);

            SearchMessageLogsFilters filters = mapper.map(param);

            assertThat(filters.requiresAdditional()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("map MessageLog to ApiMessageLog")
    class MapMessageLogTests {

        @Test
        void should_map_message_log_with_all_fields() {
            MessageLog messageLog = MessageLog.builder()
                .apiId("api-123")
                .requestId("request-123")
                .timestamp("2024-01-01T00:00:00Z")
                .operation("subscribe")
                .connectorType("entrypoint")
                .connectorId("webhook")
                .build();

            var result = mapper.map(messageLog);

            assertThat(result).isNotNull();
            assertThat(result.getApiId()).isEqualTo("api-123");
            assertThat(result.getRequestId()).isEqualTo("request-123");
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getOperation()).isEqualTo(MessageOperation.SUBSCRIBE);
            assertThat(result.getConnectorType()).isEqualTo(ConnectorType.ENTRYPOINT);
            assertThat(result.getConnectorId()).isEqualTo("webhook");
        }

        @ParameterizedTest
        @CsvSource(delimiterString = ";", nullValues = "null", value = { "publish;PUBLISH", "subscribe;SUBSCRIBE", "null;null" })
        void should_map_operation(String input, MessageOperation expected) {
            MessageLog messageLog = MessageLog.builder().timestamp("2024-01-01T00:00:00Z").operation(input).build();
            var result = mapper.map(messageLog);
            assertThat(result.getOperation()).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource(delimiterString = ";", nullValues = "null", value = { "endpoint;ENDPOINT", "entrypoint;ENTRYPOINT", "null;null" })
        void should_map_connector_type(String input, ConnectorType expected) {
            MessageLog messageLog = MessageLog.builder().timestamp("2024-01-01T00:00:00Z").connectorType(input).build();
            var result = mapper.map(messageLog);
            assertThat(result.getConnectorType()).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = { "invalid", "unknown" })
        void should_throw_exception_for_invalid_operation(String invalidOperation) {
            MessageLog messageLog = MessageLog.builder().timestamp("2024-01-01T00:00:00Z").operation(invalidOperation).build();
            assertThatThrownBy(() -> mapper.map(messageLog)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = { "invalid", "unknown" })
        void should_throw_exception_for_invalid_connector_type(String invalidConnectorType) {
            MessageLog messageLog = MessageLog.builder().timestamp("2024-01-01T00:00:00Z").connectorType(invalidConnectorType).build();
            assertThatThrownBy(() -> mapper.map(messageLog)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("map List<MessageLog> to List<ApiMessageLog>")
    class MapMessageLogListTests {

        @Test
        void should_map_empty_list() {
            List<MessageLog> messageLogs = Collections.emptyList();
            var result = mapper.map(messageLogs);
            assertThat(result).isEmpty();
        }

        @Test
        void should_map_list_with_single_item() {
            MessageLog messageLog = MessageLog.builder()
                .apiId("api-123")
                .requestId("request-123")
                .timestamp("2024-01-01T00:00:00Z")
                .build();
            var result = mapper.map(Collections.singletonList(messageLog));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApiId()).isEqualTo("api-123");
            assertThat(result.get(0).getRequestId()).isEqualTo("request-123");
        }

        @Test
        void should_map_list_with_multiple_items() {
            MessageLog log1 = MessageLog.builder().apiId("api-1").requestId("request-1").timestamp("2024-01-01T00:00:00Z").build();
            MessageLog log2 = MessageLog.builder().apiId("api-2").requestId("request-2").timestamp("2024-01-01T00:00:00Z").build();
            MessageLog log3 = MessageLog.builder().apiId("api-3").requestId("request-3").timestamp("2024-01-01T00:00:00Z").build();

            var result = mapper.map(Arrays.asList(log1, log2, log3));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getApiId()).isEqualTo("api-1");
            assertThat(result.get(1).getApiId()).isEqualTo("api-2");
            assertThat(result.get(2).getApiId()).isEqualTo("api-3");
        }
    }
}
