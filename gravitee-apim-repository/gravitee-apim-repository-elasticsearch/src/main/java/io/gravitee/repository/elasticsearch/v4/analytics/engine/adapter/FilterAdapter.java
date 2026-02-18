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

import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FilterAdapter {

    static final String ENTRYPOINT_FIELD = "entrypoint-id";
    static final String HTTP_PROXY_ENTRYPOINT_ID = "http-proxy";
    static final String LLM_PROXY_ENTRYPOINT_ID = "llm-proxy";
    static final String MCP_PROXY_ENTRYPOINT_ID = "mcp-proxy";

    static final List<Filter.Name> HTTP_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.APPLICATION,
        Filter.Name.PLAN,
        Filter.Name.GATEWAY,
        Filter.Name.HOST,
        Filter.Name.TENANT,
        Filter.Name.ZONE,
        Filter.Name.HTTP_METHOD,
        Filter.Name.HTTP_STATUS_CODE_GROUP,
        Filter.Name.HTTP_STATUS,
        Filter.Name.HTTP_PATH,
        Filter.Name.HTTP_PATH_MAPPING,
        Filter.Name.GEO_IP_CITY,
        Filter.Name.GEO_IP_REGION,
        Filter.Name.GEO_IP_COUNTRY,
        Filter.Name.GEO_IP_CONTINENT,
        Filter.Name.LLM_PROXY_MODEL,
        Filter.Name.LLM_PROXY_PROVIDER,
        Filter.Name.MCP_PROXY_METHOD,
        Filter.Name.MCP_PROXY_TOOL,
        Filter.Name.MCP_PROXY_RESOURCE,
        Filter.Name.MCP_PROXY_PROMPT
    );

    static final List<Filter.Name> MESSAGE_FILTER_NAMES = List.of(
        Filter.Name.MESSAGE_CONNECTOR_TYPE,
        Filter.Name.MESSAGE_CONNECTOR_ID,
        Filter.Name.MESSAGE_OPERATION_TYPE,
        Filter.Name.MESSAGE_COUNT,
        Filter.Name.MESSAGE_SIZE,
        Filter.Name.MESSAGE_ERROR_COUNT
    );

    private final FieldResolver fieldResolver;

    public FilterAdapter(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    public JsonArray adaptForMessage(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForMessage(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters;
    }

    public JsonArray adaptForHTTP(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForHTTP(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters.add(httpFilter());
    }

    public JsonArray adaptForMessageConnexion(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForMessageConnexion(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters.add(messageFilter());
    }

    public boolean shouldAdaptForHTTP(Filter filter) {
        return HTTP_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForMessage(Filter filter) {
        return MESSAGE_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForMessageConnexion(Filter filter) {
        return HTTP_FILTER_NAMES.contains(filter.name());
    }

    public JsonObject httpFilter() {
        return JsonObject.of(
            "terms",
            JsonObject.of(ENTRYPOINT_FIELD, JsonArray.of(HTTP_PROXY_ENTRYPOINT_ID, LLM_PROXY_ENTRYPOINT_ID, MCP_PROXY_ENTRYPOINT_ID))
        );
    }

    public JsonObject messageFilter() {
        return JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(httpFilter())));
    }

    private JsonObject filter(Filter filter) {
        return JsonObject.of(filterName(filter), filterValue(filter));
    }

    private String filterName(Filter filter) {
        return switch (filter.operator()) {
            case EQ -> "term";
            case Filter.Operator.IN -> "terms";
            case Filter.Operator.GTE, Filter.Operator.LTE -> "value.numberlabel";
        };
    }

    private JsonObject filterValue(Filter filter) {
        return switch (filter.operator()) {
            case EQ, LTE, GTE -> JsonObject.of(fieldResolver.fromFilter(filter), filter.value());
            case Filter.Operator.IN -> JsonObject.of(fieldResolver.fromFilter(filter), listValue(filter.value()));
        };
    }

    private JsonArray listValue(Object value) {
        if (Objects.requireNonNull(value) instanceof Collection<?> l) {
            return new JsonArray(new ArrayList<>(l));
        }
        return JsonArray.of(value);
    }
}
