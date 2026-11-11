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
package io.gravitee.repository.elasticsearch.v4.shared;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A message API writes a connection twice: a document when the connection opens, flagged
 * {@code connection-phase: open}, and another when it ends, flagged {@code connection-phase: end}.
 *
 * <ul>
 *   <li>A count reads every document but the end: each connection counts once, from the moment it opens.</li>
 *   <li>A value — a duration, a size — reads every document but the opening: it is only known once the
 *       connection has ended.</li>
 * </ul>
 *
 * <p>Documents without the field — written before it existed, by a v2 API or by any other kind of API — are read
 * by both.
 */
public final class ConnectionPhaseClause {

    public static final String FIELD = "connection-phase";
    public static final String OPEN = "open";
    public static final String END = "end";

    private ConnectionPhaseClause() {}

    /** @return the search, with the query it had restricted to documents that are not a connection end */
    public static String excludeEndsFrom(String search) {
        return excludeFrom(search, END);
    }

    /** @return the search, with the query it had restricted to documents that are not a connection opening */
    public static String excludeOpeningsFrom(String search) {
        return excludeFrom(search, OPEN);
    }

    /** The filter of an aggregation that reads values, in a search that also counts. */
    public static JsonObject notOpening() {
        return JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(phase(OPEN))));
    }

    /** The filter of an aggregation that counts, in a search that also reads values. */
    public static JsonObject notEnd() {
        return JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(phase(END))));
    }

    private static String excludeFrom(String search, String phase) {
        var json = new JsonObject(search);
        var bool = JsonObject.of("must_not", JsonArray.of(phase(phase)));
        var query = json.getJsonObject("query");
        if (query != null) {
            bool.put("must", JsonArray.of(query));
        }
        json.put("query", JsonObject.of("bool", bool));
        return json.encode();
    }

    private static JsonObject phase(String phase) {
        return JsonObject.of("term", JsonObject.of(FIELD, phase));
    }
}
