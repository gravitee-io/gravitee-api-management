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
package io.gravitee.repository.analytics.query.groupby;

import io.gravitee.repository.analytics.query.AbstractQueryBuilder;
import io.gravitee.repository.analytics.query.Range;
import io.gravitee.repository.analytics.query.Sort;
import io.gravitee.repository.analytics.query.ValueRangeBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupByQueryBuilder extends AbstractQueryBuilder<GroupByQueryBuilder, GroupByQuery> {

    protected GroupByQueryBuilder(GroupByQuery query) {
        super(query);
    }

    public static GroupByQueryBuilder query() {
        return new GroupByQueryBuilder(new GroupByQuery());
    }

    public GroupByQueryBuilder field(String field) {
        query.field(field);
        return this;
    }

    public GroupByQueryBuilder range(Range<Double> range) {
        query.groups().add(range);
        return this;
    }

    public GroupByQueryBuilder range(double from, double to) {
        query.groups().add(ValueRangeBuilder.range(from, to));
        return this;
    }

    public GroupByQueryBuilder range(int from, int to) {
        query.groups().add(ValueRangeBuilder.range((double)from, (double)to));
        return this;
    }

    public GroupByQueryBuilder sort(Sort sort) {
        query.sort(sort);
        return this;
    }
}
