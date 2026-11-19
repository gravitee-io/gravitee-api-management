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

import io.gravitee.repository.analytics.engine.api.query.EndUser;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collection;
import java.util.List;

/**
 * The clause selecting calls by end user. {@link EndUser#NONE} matches the calls without a {@code user} field, which a
 * {@code terms} clause cannot reach; the other values match as terms.
 *
 * @author GraviteeSource Team
 */
public final class EndUserClause {

    private EndUserClause() {}

    public static JsonObject of(String field, Collection<String> values) {
        var users = values
            .stream()
            .filter(value -> !EndUser.NONE.equals(value))
            .toList();
        if (users.size() == values.size()) {
            return JsonObject.of("terms", JsonObject.of(field, new JsonArray(users)));
        }
        var withoutUser = JsonObject.of("bool", JsonObject.of("must_not", JsonObject.of("exists", JsonObject.of("field", field))));
        if (users.isEmpty()) {
            return withoutUser;
        }
        return JsonObject.of(
            "bool",
            JsonObject.of(
                "should",
                new JsonArray(List.of(JsonObject.of("terms", JsonObject.of(field, new JsonArray(users))), withoutUser)),
                "minimum_should_match",
                1
            )
        );
    }
}
