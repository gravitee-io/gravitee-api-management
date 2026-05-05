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
package io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi;

import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricsQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import org.springframework.util.CollectionUtils;

public final class NativeApiMetricsSearchQueryAdapter {

    private static final String CONNECTION_STATUS_FIELD =
        RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.CONNECTION_STATUS;

    private NativeApiMetricsSearchQueryAdapter() {}

    public static String adapt(NativeApiMetricsQuery query) {
        var must = new ArrayList<JsonObject>();
        must.add(JsonObject.of("term", JsonObject.of(RequestV2MetricsV4Fields.API_ID.v4Metrics(), query.getApiId())));
        addTimestampRange(query, must);
        addTermsFilter(must, RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics(), query.getApplicationIds());
        addTermsFilter(must, RequestV2MetricsV4Fields.PLAN_ID.v4Metrics(), query.getPlanIds());
        addTermsFilter(must, CONNECTION_STATUS_FIELD, query.getConnectionStatuses());

        var json = JsonObject.of(
            "from",
            (query.getPage() - 1) * query.getSize(),
            "size",
            query.getSize(),
            "query",
            JsonObject.of("bool", JsonObject.of("must", JsonArray.of(must.toArray()))),
            "sort",
            JsonArray.of(JsonObject.of(RequestV2MetricsV4Fields.TIMESTAMP, JsonObject.of("order", "desc")))
        );
        return json.encode();
    }

    private static void addTimestampRange(NativeApiMetricsQuery query, ArrayList<JsonObject> must) {
        if (query.getFrom() == null && query.getTo() == null) {
            return;
        }
        var range = new JsonObject();
        if (query.getFrom() != null) {
            range.put("gte", query.getFrom());
        }
        if (query.getTo() != null) {
            range.put("lte", new Date(query.getTo()));
        }
        must.add(JsonObject.of("range", JsonObject.of(RequestV2MetricsV4Fields.TIMESTAMP, range)));
    }

    private static void addTermsFilter(ArrayList<JsonObject> must, String field, Set<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        must.add(JsonObject.of("terms", JsonObject.of(field, values.toArray())));
    }
}
