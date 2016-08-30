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

import java.time.temporal.ChronoUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DateRangeBuilder {

    public static DateRange between(long startTime, long endTime) {
        BetweenDateRange betweenQuery = new BetweenDateRange();

        betweenQuery.setStart(startTime);
        betweenQuery.setEnd(endTime);

        return betweenQuery;
    }

    public static DateRange lastMonth() {
        return lastMonths(1);
    }

    public static DateRange lastDay() {
        return lastDays(1);
    }

    public static DateRange lastHour() {
        return lastHours(1);
    }

    public static DateRange lastMinute() {
        return lastMinutes(1);
    }

    public static DateRange lastDays(int days) {
        SinceDateRange sinceQuery = new SinceDateRange();
        sinceQuery.setChronoUnit(ChronoUnit.DAYS);
        sinceQuery.setTime(days);

        return sinceQuery;
    }

    public static DateRange lastMonths(int months) {
        SinceDateRange sinceQuery = new SinceDateRange();
        sinceQuery.setChronoUnit(ChronoUnit.MONTHS);
        sinceQuery.setTime(months);

        return sinceQuery;
    }

    public static DateRange lastHours(int hours) {
        SinceDateRange sinceQuery = new SinceDateRange();

        sinceQuery.setChronoUnit(ChronoUnit.HOURS);
        sinceQuery.setTime(hours);

        return sinceQuery;
    }

    public static DateRange lastMinutes(int minutes) {
        SinceDateRange sinceQuery = new SinceDateRange();

        sinceQuery.setChronoUnit(ChronoUnit.MINUTES);
        sinceQuery.setTime(minutes);

        return sinceQuery;
    }
}
