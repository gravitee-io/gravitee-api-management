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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import com.google.common.net.InetAddresses;
import io.gravitee.repository.analytics.engine.api.query.TrafficScope;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields;
import io.gravitee.repository.elasticsearch.v4.shared.EntrypointScopeClause;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FilterValuesQueryAdapter {

    private static final String API_ID_FIELD = "api-id";
    private static final String API_PRODUCT_ID_FIELD = "api-product-id";

    /** Fields every v4-metrics template maps as {@code ip}. */
    private static final Set<String> IP_FIELDS = Set.of(RequestV2MetricsV4Fields.REMOTE_ADDRESS);

    /** Over the request documents, which carry the entrypoint of each call: a traffic scope reads both of its arms. */
    public String adapt(FilterValuesQuery query) {
        return adapt(query, EntrypointScopeClause::trafficScope);
    }

    /** Over the message documents, as {@code FilterAdapter} reads them: a traffic scope keeps its API arm only. */
    public String adaptOnMessages(FilterValuesQuery query) {
        return adapt(query, EntrypointScopeClause::trafficScopeApis);
    }

    private String adapt(FilterValuesQuery query, BiFunction<TrafficScope, Function<List<String>, JsonObject>, JsonObject> scopeClause) {
        var root = new JsonObject();
        root.put("size", 0);

        var boolFilter = new JsonArray();
        if (query.from() != null && query.to() != null) {
            boolFilter.add(JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", query.from(), "lte", query.to()))));
        }

        if (query.searchPattern() != null && !query.searchPattern().isBlank()) {
            boolFilter.add(valueClause(query.esFieldName(), query.searchPattern()));
        }

        if (query.apiIds() != null) {
            if (query.apiIds().isEmpty()) {
                boolFilter.add(JsonObject.of("match_none", JsonObject.of()));
            } else {
                boolFilter.add(JsonObject.of("terms", JsonObject.of(API_ID_FIELD, new JsonArray(new ArrayList<>(query.apiIds())))));
            }
        }

        if (query.apiProductId() != null) {
            boolFilter.add(JsonObject.of("term", JsonObject.of(API_PRODUCT_ID_FIELD, query.apiProductId())));
        }

        if (query.values() != null) {
            boolFilter.add(
                query.values().isEmpty()
                    ? JsonObject.of("match_none", JsonObject.of())
                    : JsonObject.of("terms", JsonObject.of(query.esFieldName(), new JsonArray(new ArrayList<>(query.values()))))
            );
        }

        if (query.trafficScope() != null) {
            boolFilter.add(
                scopeClause.apply(query.trafficScope(), apiIds ->
                    JsonObject.of("terms", JsonObject.of(API_ID_FIELD, new JsonArray(apiIds)))
                )
            );
        }

        if (!boolFilter.isEmpty()) {
            root.put("query", JsonObject.of("bool", JsonObject.of("filter", boolFilter)));
        }

        var termsSource = JsonObject.of("field", query.esFieldName());
        var compositeSource = JsonObject.of("value", JsonObject.of("terms", termsSource));
        var composite = JsonObject.of("size", query.size(), "sources", JsonArray.of(compositeSource));

        if (query.afterKey() != null && !query.afterKey().isEmpty()) {
            composite.put("after", new JsonObject(query.afterKey()));
        }

        root.put(
            "aggs",
            JsonObject.of(
                "filter_values",
                JsonObject.of("composite", composite),
                "total_count",
                JsonObject.of("cardinality", JsonObject.of("field", query.esFieldName()))
            )
        );

        return root.encode();
    }

    private static JsonObject valueClause(String field, String typed) {
        if (IP_FIELDS.contains(field)) {
            // An `ip` field takes neither a prefix nor a case-insensitive term: Elasticsearch refuses both.
            return isIpAddress(typed)
                ? JsonObject.of(Query.TERM, JsonObject.of(field, typed))
                : JsonObject.of("match_none", JsonObject.of());
        }
        // A prefix, never a substring: a leading wildcard would walk every term of the field.
        var prefix = JsonObject.of(Query.VALUE, typed, Query.CASE_INSENSITIVE, true);
        return JsonObject.of(Query.PREFIX, JsonObject.of(field, prefix));
    }

    /**
     * A full IPv4 or IPv6 address written as a literal, parsed without any lookup: a host name is never resolved. A
     * range ({@code 10.0.0.0/8}) or a scope id ({@code fe80::1%en0}) is no stored address, although Elasticsearch
     * would accept either on an {@code ip} field.
     */
    private static boolean isIpAddress(String typed) {
        return typed.indexOf('%') < 0 && InetAddresses.isInetAddress(typed);
    }
}
