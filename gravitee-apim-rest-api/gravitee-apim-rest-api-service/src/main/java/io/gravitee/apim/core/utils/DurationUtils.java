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
package io.gravitee.apim.core.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DurationUtils {

    public static Duration buildIntervalFromTimePeriod(Instant from, Instant to) {
        Duration duration = Duration.between(from, to);
        return INTERVAL
            .stream()
            .filter(id -> id.dataDuration().compareTo(duration) <= 0)
            .max(Comparator.comparing(IntervalData::dataDuration))
            .map(IntervalData::interval)
            .orElse(Duration.ofDays(1));
    }

    private static final Collection<IntervalData> INTERVAL = List.of(
        new IntervalData(Duration.ofMinutes(5), Duration.ofSeconds(10)),
        new IntervalData(Duration.ofMinutes(30), Duration.ofSeconds(15)),
        new IntervalData(Duration.ofHours(1), Duration.ofSeconds(30)),
        new IntervalData(Duration.ofHours(3), Duration.ofMinutes(1)),
        new IntervalData(Duration.ofHours(6), Duration.ofMinutes(2)),
        new IntervalData(Duration.ofHours(12), Duration.ofMinutes(5)),
        new IntervalData(Duration.ofDays(1), Duration.ofMinutes(10)),
        new IntervalData(Duration.ofDays(3), Duration.ofMinutes(30)),
        new IntervalData(Duration.ofDays(7), Duration.ofHours(1)),
        new IntervalData(Duration.ofDays(14), Duration.ofHours(3)),
        new IntervalData(Duration.ofDays(30), Duration.ofHours(6)),
        new IntervalData(Duration.ofDays(90), Duration.ofHours(12))
    );

    private record IntervalData(Duration dataDuration, Duration interval) {}
}
