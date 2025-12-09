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

import io.gravitee.apim.core.log.model.MessageLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLog;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorType;
import io.gravitee.rest.api.management.v2.rest.model.MessageOperation;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchMessageLogsParam;
import io.gravitee.rest.api.model.analytics.SearchMessageLogsFilters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface ApiMessageLogsMapper {
    /** MapStruct mapper instance. */
    ApiMessageLogsMapper INSTANCE = Mappers.getMapper(ApiMessageLogsMapper.class);

    /**
     * Maps a list of MessageLog to a list of ApiMessageLog.
     *
     * @param messageMetricsList the list of MessageLog to map
     * @return the list of ApiMessageLog
     */
    List<ApiMessageLog> map(List<MessageLog> messageMetricsList);

    /**
     * Maps a MessageLog to an ApiMessageLog.
     *
     * @param messageMetrics the MessageLog to map
     * @return the ApiMessageLog
     */
    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "mapTimestamp")
    @Mapping(target = "operation", expression = "java(mapOperation(messageMetrics.getOperation()))")
    @Mapping(target = "connectorType", expression = "java(mapConnectorType(messageMetrics.getConnectorType()))")
    ApiMessageLog map(MessageLog messageMetrics);

    /**
     * Maps SearchMessageLogsParam to SearchMessageLogsFilters.
     *
     * @param searchMessageMetricsParam the SearchMessageLogsParam to map
     * @return the SearchMessageLogsFilters
     */
    @Mapping(target = "additional", expression = "java(mapAdditionalParams(searchMessageMetricsParam.getAdditional()))")
    SearchMessageLogsFilters map(SearchMessageLogsParam searchMessageMetricsParam);

    /**
     * Maps a String operation to a MessageOperation enum.
     *
     * @param operation the operation string
     * @return the MessageOperation enum or null if operation is null
     */
    default MessageOperation mapOperation(String operation) {
        return Optional.ofNullable(operation).map(MessageOperation::fromValue).orElse(null);
    }

    /**
     * Maps a String connectorType to a ConnectorType enum.
     *
     * @param connectorType the connector type string
     * @return the ConnectorType enum or null if connectorType is null
     */
    default ConnectorType mapConnectorType(String connectorType) {
        return Optional.ofNullable(connectorType).map(ConnectorType::fromValue).orElse(null);
    }

    /**
     * Parses a comma-separated string into a list of strings.
     *
     * @param value the comma-separated string
     * @return the list of strings or null if value is null or empty
     */
    default List<String> parseCommaSeparatedString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * Maps additional query parameters in the format: "fieldName;value1,value2".
     * Multiple parameters can be provided:
     * additional=field1;val1,val2&additional=field2;val3,val4
     *
     * <p>Produces: { "field1" -> ["val1", "val2"], "field2" -> ["val3", "val4"] }
     *
     * <p>If the same field appears multiple times, values are merged:
     * additional=field1;val1&additional=field1;val2 => "field1" -> ["val1", "val2"]
     *
     * @param additional the array of additional parameters
     * @return the map of field names to lists of values, or null if additional is null or
     *     empty
     */
    default Map<String, List<String>> mapAdditionalParams(String[] additional) {
        if (additional == null || additional.length == 0) {
            return null;
        }

        Map<String, List<String>> result = new LinkedHashMap<>();

        for (String param : additional) {
            String[] parts = parseParamParts(param);
            if (parts != null) {
                List<String> values = parseValues(parts[1]);
                if (!values.isEmpty()) {
                    result.computeIfAbsent(parts[0], k -> new ArrayList<>()).addAll(values);
                }
            }
        }

        return result.isEmpty() ? null : result;
    }

    private String[] parseParamParts(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }

        String[] parts = param.split(";", 2);
        if (parts.length != 2) {
            return null;
        }

        String fieldName = parts[0].trim();
        String valuesStr = parts[1].trim();

        if (fieldName.isEmpty() || valuesStr.isEmpty()) {
            return null;
        }

        return new String[] { fieldName, valuesStr };
    }

    private List<String> parseValues(String valuesStr) {
        return Arrays.stream(valuesStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
