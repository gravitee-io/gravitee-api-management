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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DateHistogramQueryBuilder extends AbstractQueryBuilder<DateHistogramQueryBuilder, DateHistogramQuery> {

    protected DateHistogramQueryBuilder(DateHistogramQuery query) {
        super(query);
    }

    static DateHistogramQueryBuilder query() {
        return new DateHistogramQueryBuilder(new DateHistogramQuery());
    }

    public DateHistogramQueryBuilder aggregation(Aggregation aggregation) {
        query.aggregations().add(aggregation);
        return this;
    }

    public DateHistogramQueryBuilder aggregation(AggregationType type, String field) {
        return aggregation(type, field, null);
    }

    public DateHistogramQueryBuilder aggregation(AggregationType type, String field, Integer size) {
        query.aggregations().add(new Aggregation() {
            @Override
            public AggregationType type() {
                return type;
            }

            @Override
            public String field() {
                return field;
            }

            @Override
            public Integer size() {
                return size;
            }
        });

        return this;
    }
}
