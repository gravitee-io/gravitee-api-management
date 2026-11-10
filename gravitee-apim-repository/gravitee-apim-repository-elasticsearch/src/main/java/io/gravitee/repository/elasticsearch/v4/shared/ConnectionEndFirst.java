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

import io.vertx.core.json.JsonObject;

/**
 * The opening and the end of a message API connection share their request id and their timestamp: when a read
 * keeps one of them, this sort key puts the end first, and leaves the opening to a connection still open.
 */
public final class ConnectionEndFirst {

    private static final String REQUEST_ENDED = "request-ended";

    private ConnectionEndFirst() {}

    /** Indices without the field — v2 among them — sort as if it were false rather than failing the shard. */
    public static JsonObject sort() {
        return JsonObject.of(REQUEST_ENDED, JsonObject.of("order", "desc", "unmapped_type", "boolean"));
    }
}
