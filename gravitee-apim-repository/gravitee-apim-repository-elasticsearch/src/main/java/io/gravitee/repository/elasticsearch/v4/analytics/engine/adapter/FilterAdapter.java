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

import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FilterAdapter {

    private final FieldResolver fieldResolver;

    public FilterAdapter(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    public JsonArray adapt(Query query) {
        var filters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            filters.add(filter(filter));
        }
        return filters;
    }

    private JsonObject filter(Filter filter) {
        return JsonObject.of(filterName(filter), filterValue(filter));
    }

    private String filterName(Filter filter) {
        return switch (filter.operator()) {
            case Filter.Operator.EQ -> "term";
            case Filter.Operator.IN -> "terms";
            case Filter.Operator.GTE, Filter.Operator.LTE -> "value.numberlabel";
        };
    }

    private JsonObject filterValue(Filter filter) {
        return switch (filter.operator()) {
            case Filter.Operator.EQ, LTE, GTE -> JsonObject.of(fieldResolver.fromFilter(filter), filter.value());
            case Filter.Operator.IN -> JsonObject.of(fieldResolver.fromFilter(filter), listValue(filter.value()));
        };
    }

    private JsonArray listValue(Object value) {
        if (Objects.requireNonNull(value) instanceof List<?> l) {
            return new JsonArray(l);
        }
        return JsonArray.of(value);
    }
}
