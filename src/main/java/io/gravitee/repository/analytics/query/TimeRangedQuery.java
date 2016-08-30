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

import io.gravitee.repository.analytics.query.response.histogram.HistogramResponse;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class TimeRangedQuery implements Query<HistogramResponse> {

    // By default, get data for last day...
    private DateRange range = DateRangeBuilder.lastDay();

    // ... and use hour interval
    private Interval interval = IntervalBuilder.hour();

    public DateRange range() {
        return this.range;
    }

    void range(DateRange range) {
        this.range = range;
    }

    public Interval interval() {
        return this.interval;
    }

    void interval(Interval interval) {
        this.interval = interval;
    }

    @Override
    public Class<HistogramResponse> responseType() {
        return HistogramResponse.class;
    }
}
