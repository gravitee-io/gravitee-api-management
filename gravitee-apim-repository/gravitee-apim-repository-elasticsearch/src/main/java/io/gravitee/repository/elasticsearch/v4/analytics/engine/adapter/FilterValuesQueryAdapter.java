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

import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;

public class FilterValuesQueryAdapter {

    private static final String API_ID_FIELD = "api-id";

    public String adapt(FilterValuesQuery query) {
        var root = new JsonObject();
        root.put("size", 0);

        var boolFilter = new JsonArray();
        if (query.from() != null && query.to() != null) {
            boolFilter.add(JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", query.from(), "lte", query.to()))));
        }

        var searching = query.searchPattern() != null && !query.searchPattern().isBlank();
        if (searching) {
            // Prefix match on the keyword field (case-insensitive): a fingerprint or a name|fingerprint value is
            // never typed whole, and a prefix still avoids the cost of a leading wildcard.
            var prefixValue = JsonObject.of("value", query.searchPattern(), "case_insensitive", true);
            boolFilter.add(JsonObject.of("prefix", JsonObject.of(query.esFieldName(), prefixValue)));
        }

        if (query.apiIds() != null) {
            if (query.apiIds().isEmpty()) {
                boolFilter.add(JsonObject.of("match_none", JsonObject.of()));
            } else {
                boolFilter.add(JsonObject.of("terms", JsonObject.of(API_ID_FIELD, new JsonArray(new ArrayList<>(query.apiIds())))));
            }
        }

        if (!boolFilter.isEmpty()) {
            root.put("query", JsonObject.of("bool", JsonObject.of("filter", boolFilter)));
        }

        if (searching) {
            // The document filter keeps every value of a matching multi-valued document, and a composite terms
            // source takes no include, so a search aggregates plain terms narrowed to the matching values. It is
            // not paged: the matches of one pattern fit a page.
            var terms = JsonObject.of(
                "field",
                query.esFieldName(),
                "size",
                query.size(),
                "include",
                caseInsensitiveRegex(query.searchPattern()) + ".*"
            );
            root.put("aggs", JsonObject.of("filter_values", JsonObject.of("terms", terms)));
            return root.encode();
        }

        var termsSource = JsonObject.of("field", query.esFieldName());
        var compositeSource = JsonObject.of("value", JsonObject.of("terms", termsSource));
        var composite = JsonObject.of("size", query.size(), "sources", JsonArray.of(compositeSource));

        if (query.afterKey() != null && !query.afterKey().isEmpty()) {
            composite.put("after", new JsonObject(query.afterKey()));
        }

        root.put(
            "aggs",
            JsonObject.of(
                "filter_values",
                JsonObject.of("composite", composite),
                "total_count",
                JsonObject.of("cardinality", JsonObject.of("field", query.esFieldName()))
            )
        );

        return root.encode();
    }

    private static final String REGEX_RESERVED = ".?+*|{}[]()\"\\#@&<>~";

    /** A Lucene regex matching {@code pattern} literally, ignoring case: the include syntax has no flag for it. */
    private static String caseInsensitiveRegex(String pattern) {
        var regex = new StringBuilder();
        pattern
            .codePoints()
            .forEach(codePoint -> {
                var lower = Character.toLowerCase(codePoint);
                var upper = Character.toUpperCase(codePoint);
                if (lower != upper) {
                    regex.append('[').appendCodePoint(lower).appendCodePoint(upper).append(']');
                } else {
                    if (REGEX_RESERVED.indexOf(codePoint) >= 0) {
                        regex.append('\\');
                    }
                    regex.appendCodePoint(codePoint);
                }
            });
        return regex.toString();
    }
}
