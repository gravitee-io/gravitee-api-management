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
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { DateMapper.class })
public interface ApiMessageLogsMapper {
    Logger logger = LoggerFactory.getLogger(ApiMessageLogsMapper.class);
    ApiMessageLogsMapper INSTANCE = Mappers.getMapper(ApiMessageLogsMapper.class);

    List<ApiMessageLog> map(List<MessageLog> messageMetricsList);

    @Mapping(source = "timestamp", target = "timestamp", qualifiedByName = "mapTimestamp")
    @Mapping(target = "operation", expression = "java(mapOperation(messageMetrics.getOperation()))")
    @Mapping(target = "connectorType", expression = "java(mapConnectorType(messageMetrics.getConnectorType()))")
    ApiMessageLog map(MessageLog messageMetrics);

    @Mapping(target = "additional", expression = "java(parseAdditionalParams(searchMessageMetricsParam.getAdditional()))")
    SearchMessageLogsFilters map(SearchMessageLogsParam searchMessageMetricsParam);

    default MessageOperation mapOperation(String operation) {
        if (operation == null) {
            return null;
        }
        try {
            return MessageOperation.fromValue(operation);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to map operation value: {}", operation, e);
            return null;
        }
    }

    default ConnectorType mapConnectorType(String connectorType) {
        if (connectorType == null) {
            return null;
        }
        try {
            return ConnectorType.fromValue(connectorType);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to map connector type value: {}", connectorType, e);
            return null;
        }
    }

    default List<String> parseCommaSeparatedString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Parses additional query parameters in the format: "fieldName;value1,value2"
     * Multiple parameters can be provided:
     *   additional=field1;val1,val2&additional=field2;val3,val4
     *
     * Produces:
     *   { "field1" -> ["val1", "val2"], "field2" -> ["val3", "val4"] }
     *
     * If the same field appears multiple times, values are merged:
     *   additional=field1;val1&additional=field1;val2
     *   => "field1" -> ["val1", "val2"]
     */
    default Map<String, List<String>> parseAdditionalParams(String[] additional) {
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

        String trimmed = param.trim();
        String[] parts = trimmed.split(";", 2);
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
            .collect(Collectors.toList());
    }
}
