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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.Getter;

@Getter
public class TimeProvider {

    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_DASH = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss.SSSxxx"
    ).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER_WITH_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(
        ZoneId.systemDefault()
    );
    private static final DateTimeFormatter DATE_FORMATTER_WITH_DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(
        ZoneId.systemDefault()
    );

    private final Instant now;
    private final String dateToday;
    private final String dateYesterday;
    private final String dateTimeToday;
    private final String dateTimeYesterday;
    private final String todayWithDot;
    private final String yesterdayWithDot;

    public TimeProvider() {
        now = Instant.now().minus(30, ChronoUnit.MINUTES);
        final Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        dateToday = DATE_FORMATTER_WITH_DASH.format(now);
        dateYesterday = DATE_FORMATTER_WITH_DASH.format(yesterday);

        dateTimeToday = DATE_TIME_FORMATTER_WITH_DASH.format(now);
        dateTimeYesterday = DATE_TIME_FORMATTER_WITH_DASH.format(yesterday);

        todayWithDot = DATE_FORMATTER_WITH_DOT.format(now);
        yesterdayWithDot = DATE_FORMATTER_WITH_DOT.format(yesterday);
    }

    public void setTimestamps(Map<String, Object> data) {
        data.put("now", now.toEpochMilli());
        IntStream.rangeClosed(1, 10).forEach(i -> data.putIfAbsent("nowMinus" + i, now.minusSeconds(i * 60L).toEpochMilli()));
        IntStream.rangeClosed(1, 10).forEach(i -> data.putIfAbsent("nowPlus" + i, now.plusSeconds(i * 60L).toEpochMilli()));
    }
}
