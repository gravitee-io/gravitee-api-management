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
package io.gravitee.repository.analytics.model.query;

import io.gravitee.repository.analytics.query.IntervalQueryBuilder;
import io.gravitee.repository.analytics.query.PeriodQueryBuilder;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefaultQuery implements Query {

    // By default, only the last day
    private DateRangeQuery dateRangeQuery = PeriodQueryBuilder.lastDay();
    private IntervalQuery intervalQuery = IntervalQueryBuilder.minute();

    // By default, retrieve only hits
    private QueryType queryType = QueryType.HITS;

    private FilterQuery filterQuery;

    public DateRangeQuery dateRange() {
        return dateRangeQuery;
    }

    public void dateRange(DateRangeQuery timedQuery) {
        this.dateRangeQuery = timedQuery;
    }

    public QueryType type() {
        return queryType;
    }

    public void type(QueryType queryType) {
        this.queryType = queryType;
    }

    public FilterQuery filter() {
        return filterQuery;
    }

    public void filter(FilterQuery filterQuery) {
        this.filterQuery = filterQuery;
    }

    public IntervalQuery interval() {
        return intervalQuery;
    }

    public void interval(IntervalQuery intervalQuery) {
        this.intervalQuery = intervalQuery;
    }
}
