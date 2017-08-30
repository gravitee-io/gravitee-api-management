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
package io.gravitee.repository.healthcheck.query;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractQueryBuilder<QB extends AbstractQueryBuilder, Q extends AbstractQuery> implements QueryBuilder {

    protected final Q query;

    protected AbstractQueryBuilder(Q query) {
        this.query = query;
    }

    @Override
    public Q build() {
        return query;
    }

    public QB query(String query) {
        if (query != null) {
            this.query.query(new QueryFilter(query));
        }

        return (QB) this;
    }

    public QB api(String api) {
        this.query.root(new RootFilter("api", api));

        return (QB) this;
    }
}
