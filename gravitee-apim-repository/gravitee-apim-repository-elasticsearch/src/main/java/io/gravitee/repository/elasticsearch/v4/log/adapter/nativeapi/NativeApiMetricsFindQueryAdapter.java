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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Date;
import java.util.List;

public final class NativeApiMetricsFindQueryAdapter {

    private NativeApiMetricsFindQueryAdapter() {}

    public static String adapt(String apiId, String requestId, Long from, Long to) {
        var must = List.of(
            term(RequestV2MetricsV4Fields.API_ID.v4Metrics(), apiId),
            term(RequestV2MetricsV4Fields.REQUEST_ID.v4Metrics(), requestId),
            timestampRange(from, to)
        );
        var query = JsonObject.of("size", 1, "query", JsonObject.of("bool", JsonObject.of("must", JsonArray.of(must.toArray()))));
        return query.encode();
    }

    private static JsonObject term(String field, String value) {
        return JsonObject.of("term", JsonObject.of(field, value));
    }

    private static JsonObject timestampRange(Long from, Long to) {
        return JsonObject.of("range", JsonObject.of(RequestV2MetricsV4Fields.TIMESTAMP, JsonObject.of("gte", from, "lte", new Date(to))));
    }
}
