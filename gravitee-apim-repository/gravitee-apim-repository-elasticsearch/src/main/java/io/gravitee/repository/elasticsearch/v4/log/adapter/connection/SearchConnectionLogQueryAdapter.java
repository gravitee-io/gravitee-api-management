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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.springframework.util.CollectionUtils;

public class SearchConnectionLogQueryAdapter {

    private SearchConnectionLogQueryAdapter() {}

    public static String adapt(ConnectionLogQuery query) {
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

    private static JsonObject buildElasticQuery(ConnectionLogQuery.Filter filter) {
        if (filter == null) {
            return null;
        }

        var mustFilterList = new ArrayList<JsonObject>();

        addApisFilter(filter, mustFilterList);

        addFromAndToFilters(filter, mustFilterList);

        addApplicationsFilter(filter, mustFilterList);

        addPlansFilter(filter, mustFilterList);

        addHttpMethodsFilter(filter, mustFilterList);

        addStatusesFilter(filter, mustFilterList);

        addEntrypointIdsFilter(filter, mustFilterList);

        if (!mustFilterList.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(mustFilterList.toArray())));
        }

        return null;
    }

    private static void addApisFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApiIds())) {
            mustFilterList.add(buildV2AndV4Terms("api", "api-id", filter.getApiIds()));
        }
    }

    private static void addEntrypointIdsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getEntrypointIds())) {
            mustFilterList.add(JsonObject.of("terms", JsonObject.of("entrypoint-id", filter.getEntrypointIds())));
        }
    }

    private static void addStatusesFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getStatuses())) {
            mustFilterList.add(JsonObject.of("terms", JsonObject.of("status", filter.getStatuses())));
        }
    }

    private static void addHttpMethodsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMethods())) {
            mustFilterList.add(buildV2AndV4Terms("method", "http-method", filter.getMethods().stream().map(HttpMethod::code).toList()));
        }
    }

    private static void addPlansFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getPlanIds())) {
            mustFilterList.add(buildV2AndV4Terms("plan", "plan-id", filter.getPlanIds()));
        }
    }

    private static void addApplicationsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApplicationIds())) {
            mustFilterList.add(buildV2AndV4Terms("application", "application-id", filter.getApplicationIds()));
        }
    }

    private static void addFromAndToFilters(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (filter.getFrom() != null || filter.getTo() != null) {
            var timestampJsonObject = new JsonObject();
            if (filter.getFrom() != null) {
                timestampJsonObject.put("gte", filter.getFrom());
            }
            if (filter.getTo() != null) {
                timestampJsonObject.put("lte", new Date(filter.getTo()));
            }
            mustFilterList.add(JsonObject.of("range", JsonObject.of("@timestamp", timestampJsonObject)));
        }
    }

    private static JsonObject buildSort() {
        return JsonObject.of("@timestamp", JsonObject.of("order", "desc"));
    }

    private static JsonObject buildV2AndV4Terms(String v2RequestIndexRef, String v4MetricsIndexRef, Collection<?> value) {
        var terms = new ArrayList<JsonObject>();
        terms.add(JsonObject.of("terms", JsonObject.of(v2RequestIndexRef, value.toArray())));
        terms.add(JsonObject.of("terms", JsonObject.of(v4MetricsIndexRef, value.toArray())));
        return buildShould(terms);
    }

    private static JsonObject buildShould(List<JsonObject> terms) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(terms.toArray())));
    }
}
