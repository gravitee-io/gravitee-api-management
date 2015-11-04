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

import io.gravitee.repository.analytics.model.query.IntervalQuery;

import java.time.temporal.ChronoUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class IntervalQueryBuilder {

    public static IntervalQuery month() {
        return interval(ChronoUnit.MONTHS, 1);
    }

    public static IntervalQuery months(int interval) {
        return interval(ChronoUnit.MONTHS, interval);
    }

    public static IntervalQuery day() {
        return interval(ChronoUnit.DAYS, 1);
    }

    public static IntervalQuery days(int interval) {
        return interval(ChronoUnit.DAYS, interval);
    }

    public static IntervalQuery hour() {
        return interval(ChronoUnit.HOURS, 1);
    }

    public static IntervalQuery hours(int interval) {
        return interval(ChronoUnit.HOURS, interval);
    }

    public static IntervalQuery minute() {
        return interval(ChronoUnit.MINUTES, 1);
    }

    public static IntervalQuery minutes(int interval) {
        return interval(ChronoUnit.MINUTES, interval);
    }

    public static IntervalQuery second() {
        return interval(ChronoUnit.SECONDS, 1);
    }

    public static IntervalQuery seconds(int interval) {
        return interval(ChronoUnit.SECONDS, interval);
    }

    public static IntervalQuery interval(ChronoUnit unit, int interval) {
        return () -> unit.getDuration().toMillis() * interval;
    }
}
