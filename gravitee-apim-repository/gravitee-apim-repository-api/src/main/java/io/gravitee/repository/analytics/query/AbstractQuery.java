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
package io.gravitee.repository.analytics.query;

import io.gravitee.repository.analytics.query.response.Response;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractQuery<T extends Response> implements Query<T> {

    private RootFilter rootFilter;

    private TimeRangeFilter timeRangeFilter;

    private QueryFilter queryFilter;

    private List<TermsFilter> termsFilters;

    public RootFilter root() {
        return rootFilter;
    }

    void root(RootFilter rootFilter) {
        this.rootFilter = rootFilter;
    }

    public TimeRangeFilter timeRange() {
        return timeRangeFilter;
    }

    void timeRange(TimeRangeFilter timeRangeFilter) {
        this.timeRangeFilter = timeRangeFilter;
    }

    public QueryFilter query() {
        return queryFilter;
    }

    void query(QueryFilter queryFilter) {
        this.queryFilter = queryFilter;
    }

    public List<TermsFilter> terms() {
        return termsFilters;
    }

    void terms(List<TermsFilter> termsFilters) {
        this.termsFilters = termsFilters;
    }
}
