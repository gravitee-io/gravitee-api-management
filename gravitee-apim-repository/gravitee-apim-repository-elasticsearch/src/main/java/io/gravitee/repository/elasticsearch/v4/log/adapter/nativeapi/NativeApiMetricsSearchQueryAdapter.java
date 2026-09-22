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

import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl;
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

    /**
     * @param maxResultWindow the cluster's {@code index.max_result_window}; a from/size page reaching past it
     *     is refused here rather than by Elasticsearch, so the caller gets a 400 naming the page it asked for
     *     instead of a 500 wrapping a {@code search_phase_execution_exception}.
     *     <p>It only started to matter once the real total reached the paginator: while the count was capped
     *     at 10 000, the last page it could offer landed exactly on the default window. With the true count of
     *     a busy API the paginator offers pages beyond it.
     *     <p>The lasting answer is {@code search_after}, which pages without a window at all. That is a bigger
     *     change than this one, and it needs the same sort key on both the count and the scroll.
     * @throws IllegalArgumentException when the requested page reaches past the window. Thrown rather than
     *     clamped: a clamped page would answer with someone else's rows and look like success.
     */
    public static String adapt(NativeApiMetricsQuery query, int maxResultWindow) {
        // long, because the product overflows int for a page number a client is free to send: page and size
        // are only bounded below. An overflowed from is negative, slips past the check below, and reaches
        // Elasticsearch as a 400 — the very outcome this guard exists to replace.
        long from = (long) (query.getPage() - 1) * query.getSize();
        if (from + query.getSize() > maxResultWindow) {
            throw new IllegalArgumentException(
                "page " +
                    query.getPage() +
                    " of size " +
                    query.getSize() +
                    " reaches beyond the first " +
                    maxResultWindow +
                    " connection events; " +
                    remedy(query, maxResultWindow)
            );
        }
        var must = new ArrayList<JsonObject>();
        must.add(JsonObject.of("term", JsonObject.of(RequestV2MetricsV4Fields.API_ID.v4Metrics(), query.getApiId())));
        addTimestampRange(query, must);
        addTermsFilter(must, RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics(), query.getApplicationIds());
        addTermsFilter(must, RequestV2MetricsV4Fields.PLAN_ID.v4Metrics(), query.getPlanIds());
        addTermsFilter(must, CONNECTION_STATUS_FIELD, query.getConnectionStatuses());

        var json = JsonObject.of(
            "from",
            from,
            "size",
            query.getSize(),
            // Without it Elasticsearch stops counting at 10 000 and the page count silently caps with it.
            ElasticsearchDsl.Keys.TRACK_TOTAL_HITS,
            true,
            "query",
            JsonObject.of("bool", JsonObject.of("must", JsonArray.of(must.toArray()))),
            // request-id breaks ties: connection events are stamped in bursts and share a millisecond, and
            // ordering within a tie is not stable across shards, so from/size paging repeats and skips rows.
            // Unique per document for anything the gateway writes from now on — the Elasticsearch reporter
            // uses it as the document `_id`. Documents written before that change share one id across a
            // connection, so the tie-break is only a partial one for them: it keeps a connection's events
            // together, which is already better than no second key at all.
            "sort",
            JsonArray.of(
                JsonObject.of(RequestV2MetricsV4Fields.TIMESTAMP, JsonObject.of("order", "desc")),
                JsonObject.of(RequestV2MetricsV4Fields.REQUEST_ID.v4Metrics(), JsonObject.of("order", "asc", "unmapped_type", "keyword"))
            )
        );
        return json.encode();
    }

    /**
     * Which half of the request is too big decides the advice.
     *
     * <p>Page size has no upper bound of its own — {@code PaginationParam} enforces only {@code >= 1} — so a
     * caller can reach the window on page 1 with a large enough {@code perPage}. Telling them to narrow the
     * time range there is advice that cannot work: the events they asked for are the first ones.
     */
    private static String remedy(NativeApiMetricsQuery query, int maxResultWindow) {
        return query.getSize() > maxResultWindow
            ? "ask for a smaller page size"
            : "narrow the time range or the filters to bring these events onto an earlier page";
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
