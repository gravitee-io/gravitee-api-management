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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SinceQuery implements DateRangeQuery {

    private long end = System.currentTimeMillis();

    private ChronoUnit chronoUnit;
    private long time;

    public SinceQuery(long time, ChronoUnit chronoUnit) {
        this.time = time;
        this.chronoUnit = chronoUnit;
    }

    public SinceQuery() {
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public ChronoUnit getChronoUnit() {
        return chronoUnit;
    }

    public void setChronoUnit(ChronoUnit chronoUnit) {
        this.chronoUnit = chronoUnit;
    }

    @Override
    public long start() {
        return end - (time * chronoUnit.getDuration().toMillis());
    }

    @Override
    public long end() {
        System.out.println("---------------");
        System.out.println("end : " + end);
        System.out.println("instant : " + Instant.ofEpochMilli(end));
        System.out.println("zoned : " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), TimeZone.getTimeZone("GMT+1").toZoneId()));
        return end;
    }
}
