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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

        addRequestIdsFilter(filter, mustFilterList);

        addTransactionIdsFilter(filter, mustFilterList);

        addUriFilter(filter, mustFilterList);

        addResponseTimeRangesFilter(filter, mustFilterList);

        if (!mustFilterList.isEmpty()) {
            return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(mustFilterList.toArray())));
        }

        return null;
    }

    private static void addApisFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApiIds())) {
            mustFilterList.add(buildV2AndV4Terms(ConnectionLogField.API_ID, filter.getApiIds()));
        }
    }

    private static void addRequestIdsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getRequestIds())) {
            mustFilterList.add(buildV2AndV4Terms(ConnectionLogField.REQUEST_ID, filter.getRequestIds()));
        }
    }

    private static void addTransactionIdsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getTransactionIds())) {
            mustFilterList.add(buildV2AndV4Terms(ConnectionLogField.TRANSACTION_ID, filter.getTransactionIds()));
        }
    }

    private static void addUriFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        var uriKeyword = filter.getUri();
        if (uriKeyword != null && !uriKeyword.isBlank()) {
            var beginningSlash = uriKeyword.startsWith("/") ? "" : "/";
            var endingWildcard = uriKeyword.endsWith("*") ? "" : "*";

            mustFilterList.add(
                JsonObject.of("wildcard", JsonObject.of(ConnectionLogField.URI, beginningSlash + uriKeyword + endingWildcard))
            );
        }
    }

    private static void addEntrypointIdsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getEntrypointIds())) {
            mustFilterList.add(
                JsonObject.of("terms", JsonObject.of(ConnectionLogField.ENTRYPOINT_ID.v4Metrics(), filter.getEntrypointIds()))
            );
        }
    }

    private static void addStatusesFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getStatuses())) {
            mustFilterList.add(JsonObject.of("terms", JsonObject.of(ConnectionLogField.STATUS, filter.getStatuses())));
        }
    }

    private static void addHttpMethodsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getMethods())) {
            mustFilterList.add(
                buildV2AndV4Terms(ConnectionLogField.HTTP_METHOD, filter.getMethods().stream().map(HttpMethod::code).toList())
            );
        }
    }

    private static void addPlansFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getPlanIds())) {
            mustFilterList.add(buildV2AndV4Terms(ConnectionLogField.PLAN_ID, filter.getPlanIds()));
        }
    }

    private static void addApplicationsFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getApplicationIds())) {
            mustFilterList.add(buildV2AndV4Terms(ConnectionLogField.APPLICATION_ID, filter.getApplicationIds()));
        }
    }

    private static void addResponseTimeRangesFilter(ConnectionLogQuery.Filter filter, List<JsonObject> mustFilterList) {
        if (!CollectionUtils.isEmpty(filter.getResponseTimeRanges())) {
            var responseTimeRanges = new ArrayList<JsonObject>();
            filter
                .getResponseTimeRanges()
                .forEach(responseTimeRange -> {
                    var rangeJsonObject = new JsonObject();
                    if (responseTimeRange.getFrom() != null) {
                        rangeJsonObject.put("gte", responseTimeRange.getFrom());
                    }
                    if (responseTimeRange.getTo() != null) {
                        rangeJsonObject.put("lte", new Date(responseTimeRange.getTo()));
                    }

                    responseTimeRanges.add(
                        JsonObject.of("range", JsonObject.of(ConnectionLogField.GATEWAY_RESPONSE_TIME.v2Request(), rangeJsonObject))
                    );
                    responseTimeRanges.add(
                        JsonObject.of("range", JsonObject.of(ConnectionLogField.GATEWAY_RESPONSE_TIME.v4Metrics(), rangeJsonObject))
                    );
                });

            mustFilterList.add(buildShould(responseTimeRanges));
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
            mustFilterList.add(JsonObject.of("range", JsonObject.of(ConnectionLogField.TIMESTAMP, timestampJsonObject)));
        }
    }

    private static JsonObject buildSort() {
        return JsonObject.of(ConnectionLogField.TIMESTAMP, JsonObject.of("order", "desc"));
    }

    private static JsonObject buildV2AndV4Terms(ConnectionLogField.Field field, Collection<?> value) {
        var terms = new ArrayList<JsonObject>();
        terms.add(JsonObject.of("terms", JsonObject.of(field.v2Request(), value.toArray())));
        terms.add(JsonObject.of("terms", JsonObject.of(field.v4Metrics(), value.toArray())));
        return buildShould(terms);
    }

    private static JsonObject buildShould(List<JsonObject> terms) {
        return JsonObject.of("bool", JsonObject.of("should", JsonArray.of(terms.toArray())));
    }
}
