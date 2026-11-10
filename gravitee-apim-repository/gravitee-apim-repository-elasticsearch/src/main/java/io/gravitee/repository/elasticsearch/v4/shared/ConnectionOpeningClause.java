/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
 * {@code connection-phase: open}, and another when it ends. Analytics count connections, so they read the end
 * only; a connection still open is counted once it ends, in the same time bucket since both carry the time it
 * opened.
 *
 * <p>Documents without the field — written before it existed, by a v2 API or by any other kind of API — are
 * kept.
 */
public final class ConnectionOpeningClause {

    public static final String FIELD = "connection-phase";
    public static final String OPENING = "open";

    private ConnectionOpeningClause() {}

    /** @return the search, with the query it had restricted to documents that are not a connection opening */
    public static String excludeFrom(String search) {
        var json = new JsonObject(search);
        var bool = JsonObject.of("must_not", JsonArray.of(opening()));
        var query = json.getJsonObject("query");
        if (query != null) {
            bool.put("must", JsonArray.of(query));
        }
        json.put("query", JsonObject.of("bool", bool));
        return json.encode();
    }

    public static JsonObject opening() {
        return JsonObject.of("term", JsonObject.of(FIELD, OPENING));
    }
}
