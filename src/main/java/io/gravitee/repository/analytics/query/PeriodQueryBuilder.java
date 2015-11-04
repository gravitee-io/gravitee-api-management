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

import io.gravitee.repository.analytics.model.BetweenQuery;
import io.gravitee.repository.analytics.model.DateRangeQuery;
import io.gravitee.repository.analytics.model.SinceQuery;

import java.time.temporal.ChronoUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PeriodQueryBuilder {

    public static DateRangeQuery between(long startTime, long endTime) {
        BetweenQuery betweenQuery = new BetweenQuery();

        betweenQuery.setStart(startTime);
        betweenQuery.setEnd(endTime);

        return betweenQuery;
    }

    public static DateRangeQuery lastMonth() {
        return lastMonths(1);
    }

    public static DateRangeQuery lastDay() {
        return lastDays(1);
    }

    public static DateRangeQuery lastHour() {
        return lastHours(1);
    }

    public static DateRangeQuery lastMinute() {
        return lastMinutes(1);
    }

    public static DateRangeQuery lastDays(int days) {
        SinceQuery sinceQuery = new SinceQuery();
        sinceQuery.setChronoUnit(ChronoUnit.DAYS);
        sinceQuery.setTime(days);

        return sinceQuery;
    }

    public static DateRangeQuery lastMonths(int months) {
        SinceQuery sinceQuery = new SinceQuery();
        sinceQuery.setChronoUnit(ChronoUnit.MONTHS);
        sinceQuery.setTime(months);

        return sinceQuery;
    }

    public static DateRangeQuery lastHours(int hours) {
        SinceQuery sinceQuery = new SinceQuery();

        sinceQuery.setChronoUnit(ChronoUnit.HOURS);
        sinceQuery.setTime(hours);

        return sinceQuery;
    }

    public static DateRangeQuery lastMinutes(int minutes) {
        SinceQuery sinceQuery = new SinceQuery();

        sinceQuery.setChronoUnit(ChronoUnit.MINUTES);
        sinceQuery.setTime(minutes);

        return sinceQuery;
    }
}
