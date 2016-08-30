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
public class IntervalBuilder {

    public static Interval year() {
        return interval(ChronoUnit.YEARS, 1);
    }

    public static Interval month() {
        return interval(ChronoUnit.MONTHS, 1);
    }

    public static Interval months(int interval) {
        return interval(ChronoUnit.MONTHS, interval);
    }

    public static Interval day() {
        return interval(ChronoUnit.DAYS, 1);
    }

    public static Interval days(int interval) {
        return interval(ChronoUnit.DAYS, interval);
    }

    public static Interval hour() {
        return interval(ChronoUnit.HOURS, 1);
    }

    public static Interval hours(int interval) {
        return interval(ChronoUnit.HOURS, interval);
    }

    public static Interval minute() {
        return interval(ChronoUnit.MINUTES, 1);
    }

    public static Interval minutes(int interval) {
        return interval(ChronoUnit.MINUTES, interval);
    }

    public static Interval second() {
        return interval(ChronoUnit.SECONDS, 1);
    }

    public static Interval seconds(int interval) {
        return interval(ChronoUnit.SECONDS, interval);
    }

    public static Interval interval(ChronoUnit unit, int interval) {
        return () -> unit.getDuration().toMillis() * interval;
    }

    public static Interval interval(long intervalInMs) {
        return () -> intervalInMs;
    }
}
