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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation;

import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * Counts how many distinct values a keyword field holds.
 *
 * <p>Useful wherever rows repeat an identifier and the question is how many identifiers there were rather
 * than how many rows: how many conversations a set of LLM requests belongs to, how many turns within them.
 *
 * <p>Elasticsearch's {@code cardinality} is approximate: exact to its precision threshold, and within a few
 * percent beyond it. That is the right trade here — a dashboard reporting "about 40,000 conversations" is
 * useful, and the exact alternative would mean holding every id in memory.
 *
 * @author GraviteeSource Team
 */
public class CardinalityBuilder {

    private final String field;

    public CardinalityBuilder(String field) {
        this.field = field;
    }

    public Map<String, JsonObject> build(String aggName) {
        return Map.of(aggName, new JsonObject().put("cardinality", new JsonObject().put("field", field)));
    }
}
