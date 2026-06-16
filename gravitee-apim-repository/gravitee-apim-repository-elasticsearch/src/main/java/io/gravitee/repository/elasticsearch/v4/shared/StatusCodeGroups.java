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

import io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collection;

/**
 * ES-specific utility for translating HTTP status code groups and status range bounds into
 * Elasticsearch {@code range} queries. Delegates to {@link HttpStatusCodeGroups} for the
 * canonical group→[min,max] mapping. Consumed by both the analytics {@code FilterAdapter}
 * and the logs {@code SearchMetricsQueryAdapter}.
 */
public final class StatusCodeGroups {

    private StatusCodeGroups() {}

    /**
     * Builds an ES {@code range} filter for a single status code group (e.g. "2XX" → 200–299).
     */
    public static JsonObject rangeForGroup(String field, String statusCodeGroup) {
        if (statusCodeGroup == null) {
            throw new IllegalArgumentException("Status code group must not be null");
        }
        var bounds = HttpStatusCodeGroups.resolve(statusCodeGroup).orElseThrow(() ->
            new IllegalArgumentException("Unknown status code group: " + statusCodeGroup)
        );
        return rangeFilter(field, bounds.min(), bounds.max());
    }

    /**
     * Builds an ES {@code bool.should} of {@code range} filters for multiple status code groups
     * (IN operator). Returns a single range when only one group is provided.
     */
    public static JsonObject shouldForGroups(String field, Collection<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return JsonObject.of("match_none", JsonObject.of());
        }
        if (groups.size() == 1) {
            return rangeForGroup(field, groups.iterator().next());
        }
        var ranges = new JsonArray();
        for (var group : groups) {
            ranges.add(rangeForGroup(field, group));
        }
        return JsonObject.of("bool", JsonObject.of("should", ranges, "minimum_should_match", 1));
    }

    /**
     * Builds an ES {@code range} filter with optional open bounds, suitable for
     * {@code HTTP_STATUS GTE/LTE} (single open bound) or closed ranges.
     *
     * @param field ES field name (e.g. "status")
     * @param gte   inclusive lower bound, or {@code null} for no lower bound
     * @param lte   inclusive upper bound, or {@code null} for no upper bound
     */
    public static JsonObject rangeForBounds(String field, Integer gte, Integer lte) {
        if (gte == null && lte == null) {
            throw new IllegalArgumentException("At least one bound (gte or lte) must be non-null");
        }
        var rangeBody = new JsonObject();
        if (gte != null) {
            rangeBody.put("gte", gte);
        }
        if (lte != null) {
            rangeBody.put("lte", lte);
        }
        return JsonObject.of("range", JsonObject.of(field, rangeBody));
    }

    private static JsonObject rangeFilter(String field, int gte, int lte) {
        return JsonObject.of("range", JsonObject.of(field, JsonObject.of("gte", gte, "lte", lte)));
    }
}
