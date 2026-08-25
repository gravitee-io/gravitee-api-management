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
import java.time.LocalTime;
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
     * Time of day the traffic samples sit at.
     *
     * <p>A sample may follow the clock, as the health-check and monitoring ones do, as long as every assertion
     * made about it follows too — those tests query a window built from the same reference, so the two never
     * drift apart. The traffic samples are different: some of their documents carry an hour written into the
     * template while others interpolate the reference, so a moving reference changes the distance between the
     * two. Documents that fall in distinct time buckets at one hour of the day then merge into a single bucket
     * at another, and any assertion that counts buckets becomes a function of when the build runs.
     *
     * <p>The value is squeezed between two bounds, both measured rather than assumed:
     *
     * <ul>
     *   <li>after 00:01, because the day-sized windows in {@code MetricsElasticsearchRepositoryTest} open at
     *       {@code withHour(0).withMinute(1)}. Set to midnight, two of them stop seeing these samples;</li>
     *   <li>before the moment the build starts, because {@code ElasticsearchAnalyticsRepositoryTest} queries
     *       {@code lastDays(30)}, whose upper bound is the wall clock: a sample dated later is invisible to it.
     *       The nightly of 2026-08-24 started at 00:16 UTC, which leaves room but not much.</li>
     * </ul>
     *
     * <p>No constant closes that band completely: a build reaching {@code testCountWithTwoApis} in the four
     * minutes between 00:01 and this value would count four documents instead of seven. Closing it for good
     * means moving those two lower bounds off 00:01, which is a change to tests this one does not touch.
     *
     * <p>It also has to stay below the ten minute interval used by
     * {@code should_return_response_status_for_api_v2_and_v4}: that test reads the current day's samples in
     * the bucket its window closes on, and expects the average they produce.
     */
    private static final LocalTime TRAFFIC_SAMPLE_TIME = LocalTime.of(0, 5);

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
    private final String trafficDateTimeToday;
    private final String trafficDateTimeYesterday;
    private final String todayWithDot;
    private final String yesterdayWithDot;

    public TimeProvider() {
        now = REFERENCE;
        final Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        dateToday = DATE_FORMATTER_WITH_DASH.format(now);
        dateYesterday = DATE_FORMATTER_WITH_DASH.format(yesterday);

        dateTimeToday = DATE_TIME_FORMATTER_WITH_DASH.format(now);
        dateTimeYesterday = DATE_TIME_FORMATTER_WITH_DASH.format(yesterday);

        final Instant trafficToday = trafficSampleInstant();
        trafficDateTimeToday = DATE_TIME_FORMATTER_WITH_DASH.format(trafficToday);
        trafficDateTimeYesterday = DATE_TIME_FORMATTER_WITH_DASH.format(trafficToday.minus(1, ChronoUnit.DAYS));

        todayWithDot = DATE_FORMATTER_WITH_DOT.format(now);
        yesterdayWithDot = DATE_FORMATTER_WITH_DOT.format(yesterday);
    }

    /**
     * @return the instant the traffic samples sit at, today. Tests asserting on them must build their windows
     *         from this rather than from {@link #now()}.
     */
    public static Instant trafficSampleInstant() {
        return REFERENCE.atZone(ZONE).toLocalDate().atTime(TRAFFIC_SAMPLE_TIME).atZone(ZONE).toInstant();
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
