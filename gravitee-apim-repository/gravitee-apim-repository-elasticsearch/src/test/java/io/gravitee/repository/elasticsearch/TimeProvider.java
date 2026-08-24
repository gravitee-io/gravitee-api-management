/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.Getter;

@Getter
public class TimeProvider {

    /**
     * Zone "today" and "yesterday" are resolved in, by the sample data and by the tests alike (see
     * {@link #zone()}). UTC because the sample templates spell their timestamps out in UTC.
     *
     * <p>Note this only lines up with the day production resolves daily index names in — which comes from
     * the default zone — when the JVM itself runs on UTC, as CI does.
     */
    private static final ZoneId ZONE = ZoneOffset.UTC;

    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_DASH = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx"
    ).withZone(ZONE);
    private static final DateTimeFormatter DATE_FORMATTER_WITH_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);
    private static final DateTimeFormatter DATE_FORMATTER_WITH_DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZONE);

    /**
     * How far in the past the reference instant sits, so that sample data projecting timestamps forward
     * still lands in the past.
     */
    private static final Duration BACKDATE = Duration.ofMinutes(30);

    /**
     * How far into the day the reference instant is held when backdating would otherwise take it out of the
     * current day. Enough to clear the start-of-day boundary the tests query from.
     */
    private static final Duration START_OF_DAY_MARGIN = Duration.ofMinutes(30);

    /**
     * Reference instant shared by the whole Elasticsearch test module, frozen for the lifetime of the JVM.
     *
     * <p>Sample data is indexed once against this instant, and every test asserting on it must resolve
     * "today" and "yesterday" against the very same value — hence {@link #now()} rather than
     * {@link Instant#now()} on the test side.
     */
    private static final Instant REFERENCE = reference();

    private static Instant reference() {
        Instant systemNow = Instant.now();
        Instant backdated = systemNow.minus(BACKDATE).truncatedTo(ChronoUnit.MINUTES);
        // Backdating must never reach the previous day: repositories with no time range of their own read
        // today's index off the system clock (MonitoringRepository, the v2 analytics commands), so the
        // sample data has to sit on the day the system clock is on. Left unclamped, any run starting within
        // BACKDATE of midnight indexes a dataset dated the day before and fails wholesale.
        Instant floor = systemNow.atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant().plus(START_OF_DAY_MARGIN);
        return backdated.isBefore(floor) ? floor : backdated;
    }

    private final Instant now;
    private final String dateToday;
    private final String dateYesterday;
    private final String dateTimeToday;
    private final String dateTimeYesterday;
    private final String todayWithDot;
    private final String yesterdayWithDot;

    public TimeProvider() {
        now = REFERENCE;
        final Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        dateToday = DATE_FORMATTER_WITH_DASH.format(now);
        dateYesterday = DATE_FORMATTER_WITH_DASH.format(yesterday);

        dateTimeToday = DATE_TIME_FORMATTER_WITH_DASH.format(now);
        dateTimeYesterday = DATE_TIME_FORMATTER_WITH_DASH.format(yesterday);

        todayWithDot = DATE_FORMATTER_WITH_DOT.format(now);
        yesterdayWithDot = DATE_FORMATTER_WITH_DOT.format(yesterday);
    }

    /**
     * @return the reference instant every Elasticsearch test must use instead of {@link Instant#now()}.
     */
    public static Instant now() {
        return REFERENCE;
    }

    /**
     * @return the zone every Elasticsearch test must resolve days in, so that the window they query and the
     *         daily index the data sits in refer to the same day.
     */
    public static ZoneId zone() {
        return ZONE;
    }

    public void setTimestamps(Map<String, Object> data) {
        data.put("now", now.toEpochMilli());
        IntStream.rangeClosed(1, 15).forEach(i -> data.putIfAbsent("nowMinus" + i, now.minusSeconds(i * 60L).toEpochMilli()));
        IntStream.rangeClosed(1, 15).forEach(i -> data.putIfAbsent("nowPlus" + i, now.plusSeconds(i * 60L).toEpochMilli()));
    }
}
