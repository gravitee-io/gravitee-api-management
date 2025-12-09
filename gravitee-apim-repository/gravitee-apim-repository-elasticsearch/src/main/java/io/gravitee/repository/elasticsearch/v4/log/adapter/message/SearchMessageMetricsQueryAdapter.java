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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;

public class SearchMessageMetricsQueryAdapter {

    private SearchMessageMetricsQueryAdapter() {}

    public static String adapt(MessageMetricsQuery query) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("from", (query.getPage() - 1) * query.getSize());
        jsonContent.put("size", query.getSize());

        var esQuery = buildElasticQuery(query.getFilter());
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }

        jsonContent.put("sort", buildSort());

        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildElasticQuery(MessageMetricsQuery.Filter filter) {
        if (filter == null) {
            return null;
        }

        var terms = new ArrayList<JsonObject>();

        addTermIfNotNull(terms, MessageMetricsFields.API_ID, filter.apiId());
        addTermIfNotNull(terms, MessageMetricsFields.REQUEST_ID, filter.requestId());
        addTermIfNotNull(terms, MessageMetricsFields.CONNECTOR_TYPE, filter.connectorType());
        addTermIfNotNull(terms, MessageMetricsFields.CONNECTOR_ID, filter.connectorId());
        addTermIfNotNull(terms, MessageMetricsFields.OPERATION, filter.operation());
        addRangeQueryIfValid(terms, filter.from(), filter.to());
        addAdditionalFieldsQueries(terms, filter.additional());
        addExistsFilterIfRequired(terms, filter.requiresAdditional());

        return terms.isEmpty() ? null : JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
    }

    private static void addTermIfNotNull(ArrayList<JsonObject> terms, String field, String value) {
        if (value == null) {
            return;
        }
        terms.add(JsonObject.of("term", JsonObject.of(field, value)));
    }

    private static void addRangeQueryIfValid(ArrayList<JsonObject> terms, long from, long to) {
        if (from <= 0 || from >= to) {
            return;
        }
        terms.add(JsonObject.of("range", JsonObject.of(MessageMetricsFields.TIMESTAMP, JsonObject.of("gte", from, "lte", to))));
    }

    private static void addAdditionalFieldsQueries(ArrayList<JsonObject> terms, java.util.Map<String, java.util.List<String>> additional) {
        if (additional == null || additional.isEmpty()) {
            return;
        }

        additional.forEach((fieldName, values) -> {
            if (fieldName == null || fieldName.trim().isEmpty() || values == null || values.isEmpty()) {
                return;
            }

            var normalizedValues = new JsonArray();
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                var trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    normalizedValues.add(trimmed);
                }
            }

            if (!normalizedValues.isEmpty()) {
                addAdditionalFieldQuery(terms, fieldName.trim(), normalizedValues);
            }
        });
    }

    private static void addAdditionalFieldQuery(ArrayList<JsonObject> terms, String fieldName, JsonArray values) {
        if (values.isEmpty()) {
            return;
        }

        var fieldPath = MessageMetricsFields.ADDITIONAL_METRICS + "." + fieldName;

        JsonObject query;

        if (fieldName.startsWith("string_")) {
            if (values.size() == 1) {
                query = JsonObject.of("match_phrase", JsonObject.of(fieldPath, values.getString(0)));
            } else {
                var shouldClauses = new JsonArray();
                for (int i = 0; i < values.size(); i++) {
                    shouldClauses.add(JsonObject.of("match_phrase", JsonObject.of(fieldPath, values.getString(i))));
                }
                query = JsonObject.of("bool", JsonObject.of("should", shouldClauses, "minimum_should_match", 1));
            }
        } else {
            query = values.size() == 1
                ? JsonObject.of("term", JsonObject.of(fieldPath, values.getString(0)))
                : JsonObject.of("terms", JsonObject.of(fieldPath, values));
        }

        terms.add(query);
    }

    private static void addExistsFilterIfRequired(ArrayList<JsonObject> terms, Boolean requiresAdditional) {
        if (Boolean.TRUE.equals(requiresAdditional)) {
            terms.add(JsonObject.of("exists", JsonObject.of("field", MessageMetricsFields.ADDITIONAL_METRICS)));
        }
    }

    private static JsonObject buildSort() {
        return JsonObject.of(MessageMetricsFields.TIMESTAMP, JsonObject.of("order", "desc"));
    }
}
