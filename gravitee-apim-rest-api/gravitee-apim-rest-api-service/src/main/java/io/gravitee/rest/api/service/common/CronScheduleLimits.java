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
package io.gravitee.rest.api.service.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ExecutionException;
import org.springframework.scheduling.support.CronExpression;

public final class CronScheduleLimits {

    private static final LocalDateTime CYCLE_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime CYCLE_END = CYCLE_START.plusYears(400);
    private static final Cache<String, Duration> MINIMUM_INTERVALS = CacheBuilder.newBuilder().maximumSize(1_000).build();

    private CronScheduleLimits() {}

    public static boolean isMoreFrequentThanLimit(String userCron, long minimumIntervalMillis) {
        return minimumIntervalMillis > 0 && minimumInterval(userCron).compareTo(Duration.ofMillis(minimumIntervalMillis)) < 0;
    }

    public static long limitFrequency(long userDelayMillis, long minimumIntervalMillis) {
        return minimumIntervalMillis > 0 ? Math.max(userDelayMillis, minimumIntervalMillis) : userDelayMillis;
    }

    public static boolean isMoreFrequentThanLimit(long userDelayMillis, long minimumIntervalMillis) {
        return minimumIntervalMillis > 0 && userDelayMillis < minimumIntervalMillis;
    }

    static Duration minimumInterval(String cron) {
        var expression = CronExpression.parse(cron);
        try {
            return MINIMUM_INTERVALS.get(expression.toString(), () -> computeMinimumInterval(expression));
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("Unable to analyze cron expression: " + cron, e.getCause());
        }
    }

    private static Duration computeMinimumInterval(CronExpression expression) {
        var firstExecution = expression.next(CYCLE_START.minusNanos(1));
        if (firstExecution == null || !firstExecution.isBefore(CYCLE_END)) {
            throw new IllegalArgumentException("Cron expression has no execution in a complete Gregorian calendar cycle: " + expression);
        }

        var firstTime = firstExecution.toLocalTime();
        var lastTime = firstTime;
        Duration minimumWithinDay = null;
        var nextExecution = expression.next(firstExecution);

        while (nextExecution != null && nextExecution.toLocalDate().equals(firstExecution.toLocalDate())) {
            var interval = Duration.between(lastTime, nextExecution.toLocalTime());
            minimumWithinDay = shorter(minimumWithinDay, interval);
            lastTime = nextExecution.toLocalTime();
            nextExecution = expression.next(nextExecution);
        }

        var shortestPossibleDayBoundary = Duration.between(
            CYCLE_START.toLocalDate().atTime(lastTime),
            CYCLE_START.toLocalDate().plusDays(1).atTime(firstTime)
        );
        if (minimumWithinDay != null && shortestPossibleDayBoundary.compareTo(minimumWithinDay) >= 0) {
            return minimumWithinDay;
        }

        Duration minimum = minimumWithinDay;
        var previousDate = firstExecution.toLocalDate();
        var current = nextExecution;
        while (current != null && current.isBefore(CYCLE_END)) {
            var boundaryInterval = Duration.between(previousDate.atTime(lastTime), current.toLocalDate().atTime(firstTime));
            minimum = shorter(minimum, boundaryInterval);
            if (boundaryInterval.equals(shortestPossibleDayBoundary)) {
                return minimum;
            }
            previousDate = current.toLocalDate();
            current = expression.next(previousDate.atTime(LocalTime.MAX));
        }

        var wrappedFirstExecution = firstExecution.plusYears(400);
        var wrappedBoundary = Duration.between(previousDate.atTime(lastTime), wrappedFirstExecution.toLocalDate().atTime(firstTime));
        return shorter(minimum, wrappedBoundary);
    }

    private static Duration shorter(Duration current, Duration candidate) {
        return current == null || candidate.compareTo(current) < 0 ? candidate : current;
    }
}
