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

import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Elasticsearch shape of an entrypoint scope over v4 metrics documents, shared by the analytics engine
 * ({@code FilterAdapter}) and the logs query builder ({@code SearchMetricsQueryAdapter}) so both signals
 * select the same documents.
 *
 * <p>A default scope is a complement ({@link #excluding}): it keeps every entrypoint the registry does not
 * know and, by construction, every document written without an {@code entrypoint-id}. An explicit filter is
 * exact ({@link #exactly}); it reaches documents without an entrypoint id only through the synthetic
 * {@link ObservabilityEntrypoints#NO_ENTRYPOINT_VALUE}. Both name the field and its {@code keyword} sub-field,
 * so an index that mapped {@code entrypoint-id} as text before the current template answers the same way.
 */
public final class EntrypointScopeClause {

    public static final String FIELD = "entrypoint-id";

    /** Legacy indices map the field as {@code text}; their {@code keyword} sub-field still holds the raw id. */
    private static final String KEYWORD_FIELD = FIELD + ".keyword";

    private EntrypointScopeClause() {}

    /** The analytics default: every entrypoint not counted as HTTP request traffic is left out. */
    public static JsonObject analyticsDefault() {
        return excluding(ObservabilityEntrypoints.ANALYTICS_EXCLUDED_IDS);
    }

    /**
     * Documents whose entrypoint is none of the ids, documents without an entrypoint id included, unless
     * {@link ObservabilityEntrypoints#NO_ENTRYPOINT_VALUE} is among them: excluding it requires the field.
     *
     * <p>Excluding nothing keeps everything, which is what a complement of the empty set means and what this
     * change asks for everywhere else: when the scope is in doubt, count the document.
     */
    public static JsonObject excluding(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return JsonObject.of("match_all", JsonObject.of());
        }
        var unwanted = new JsonArray();
        var declaredIds = withoutSyntheticValue(ids);
        if (!declaredIds.isEmpty()) {
            unwanted.add(terms(FIELD, declaredIds)).add(terms(KEYWORD_FIELD, declaredIds));
        }
        if (declaredIds.size() != ids.size()) {
            unwanted.add(withoutEntrypoint());
        }
        return JsonObject.of("bool", JsonObject.of("must_not", unwanted));
    }

    /**
     * Documents whose entrypoint is one of the values, {@link ObservabilityEntrypoints#NO_ENTRYPOINT_VALUE}
     * selecting the documents without an entrypoint id. No value selects nothing, as an empty {@code IN} does
     * everywhere else in the query builders.
     */
    public static JsonObject exactly(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return JsonObject.of("match_none", JsonObject.of());
        }
        var wanted = new JsonArray();
        var declaredIds = withoutSyntheticValue(values);
        if (!declaredIds.isEmpty()) {
            wanted.add(terms(FIELD, declaredIds)).add(terms(KEYWORD_FIELD, declaredIds));
        }
        if (declaredIds.size() != values.size()) {
            wanted.add(withoutEntrypoint());
        }
        if (wanted.size() == 1) {
            return wanted.getJsonObject(0);
        }
        return JsonObject.of("bool", JsonObject.of("should", wanted, "minimum_should_match", 1));
    }

    private static List<String> withoutSyntheticValue(Collection<String> values) {
        return values
            .stream()
            .filter(value -> !ObservabilityEntrypoints.NO_ENTRYPOINT_VALUE.equals(value))
            .toList();
    }

    private static JsonObject withoutEntrypoint() {
        return JsonObject.of("bool", JsonObject.of("must_not", JsonObject.of("exists", JsonObject.of("field", FIELD))));
    }

    private static JsonObject terms(String field, Collection<String> ids) {
        return JsonObject.of("terms", JsonObject.of(field, new JsonArray(new ArrayList<>(ids))));
    }
}
