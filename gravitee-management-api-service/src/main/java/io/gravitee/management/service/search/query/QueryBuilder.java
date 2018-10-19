/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.management.service.search.query;

import io.gravitee.management.model.common.Pageable;
import io.gravitee.management.model.search.Indexable;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class QueryBuilder<T extends Indexable> {

    private final Query<T> query;

    private QueryBuilder(final Class<T> root) {
         query = new Query<>(root);
    }

    public static <S extends Indexable> QueryBuilder<S> create(Class<S> root) {
        return new QueryBuilder<>(root);
    }

    public QueryBuilder<T> setQuery(final String query) {
        this.query.setQuery(query);
        return this;
    }

    public QueryBuilder<T> addFilter(String name, Object value) {
        query.getFilters().put(name, value);
        return this;
    }

    public QueryBuilder<T> removeFilter(String name) {
        query.getFilters().remove(name);
        return this;
    }

    public QueryBuilder<T> setFilters(Map<String, Object> filters) {
        query.setFilters(filters);
        return this;
    }

    public QueryBuilder<T> setPage(Pageable pageable) {
        query.setPage(pageable);
        return this;
    }

    public Query<T> build() {
        return this.query;
    }
}
