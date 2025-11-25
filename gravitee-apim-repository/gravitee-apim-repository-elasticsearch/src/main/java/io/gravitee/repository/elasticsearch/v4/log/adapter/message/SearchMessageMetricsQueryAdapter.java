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
        if (filter.apiId() != null) {
            terms.add(JsonObject.of("term", JsonObject.of(MessageMetricsFields.API_ID, filter.apiId())));
        }
        if (filter.requestId() != null) {
            terms.add(JsonObject.of("term", JsonObject.of(MessageMetricsFields.REQUEST_ID, filter.requestId())));
        }
        if (filter.connectorType() != null) {
            terms.add(JsonObject.of("term", JsonObject.of(MessageMetricsFields.CONNECTOR_TYPE, filter.connectorType())));
        }
        if (filter.connectorId() != null) {
            terms.add(JsonObject.of("term", JsonObject.of(MessageMetricsFields.CONNECTOR_ID, filter.connectorId())));
        }
        if (filter.operation() != null) {
            terms.add(JsonObject.of("term", JsonObject.of(MessageMetricsFields.OPERATION, filter.operation())));
        }
        if (filter.from() > 0 && filter.from() < filter.to()) {
            terms.add(
                JsonObject.of(
                    "range",
                    JsonObject.of(MessageMetricsFields.TIMESTAMP, JsonObject.of("gte", filter.from(), "lte", filter.to()))
                )
            );
        }
        if (!terms.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
        }

        return null;
    }

    private static JsonObject buildSort() {
        return JsonObject.of(MessageMetricsFields.TIMESTAMP, JsonObject.of("order", "desc"));
    }
}
