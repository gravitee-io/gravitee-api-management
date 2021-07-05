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
package io.gravitee.repository.analytics.query;

import io.gravitee.repository.analytics.query.count.CountQueryBuilder;
import io.gravitee.repository.analytics.query.groupby.GroupByQueryBuilder;
import io.gravitee.repository.analytics.query.stats.StatsQueryBuilder;
import io.gravitee.repository.analytics.query.tabular.TabularQueryBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class QueryBuilders {

    public static DateHistogramQueryBuilder dateHistogram() {
        return DateHistogramQueryBuilder.query();
    }

    public static GroupByQueryBuilder groupBy() {
        return GroupByQueryBuilder.query();
    }

    public static CountQueryBuilder count() {
        return CountQueryBuilder.query();
    }

    public static TabularQueryBuilder tabular() {
        return TabularQueryBuilder.query();
    }

    public static StatsQueryBuilder stats() {
        return StatsQueryBuilder.query();
    }
}
