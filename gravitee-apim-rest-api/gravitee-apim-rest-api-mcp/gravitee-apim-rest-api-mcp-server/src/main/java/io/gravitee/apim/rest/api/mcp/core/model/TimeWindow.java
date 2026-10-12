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
package io.gravitee.apim.rest.api.mcp.core.model;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

/**
 * A time range asked for as "the last 15m", so that a caller never computes epoch values.
 */
public record TimeWindow(Instant from, Instant to) {
    public static final String DEFAULT = "15m";

    private static final Pattern FORMAT = Pattern.compile("^([1-9]\\d{0,3})([mhd])$");
    private static final Duration LONGEST = Duration.ofDays(30);

    /**
     * @param window a length such as 15m, 2h or 1d; defaults to {@value #DEFAULT}
     * @param until an ISO-8601 instant ending the window; defaults to now
     */
    public static TimeWindow parse(String window, String until, Instant now) {
        var to = until == null || until.isBlank() ? now.truncatedTo(ChronoUnit.SECONDS) : parseUntil(until, now);
        return new TimeWindow(to.minus(parseLength(window == null || window.isBlank() ? DEFAULT : window)), to);
    }

    /** The window of equal length that ends where this one starts. */
    public TimeWindow baseline() {
        return new TimeWindow(from.minus(length()), from);
    }

    public Duration length() {
        return Duration.between(from, to);
    }

    private static Duration parseLength(String window) {
        var matcher = FORMAT.matcher(window.trim());
        if (!matcher.matches()) {
            throw new TriageException("Invalid window '" + window + "'. Use a number followed by m, h or d, for example 15m, 2h or 1d.");
        }
        long amount = Long.parseLong(matcher.group(1));
        var length = switch (matcher.group(2)) {
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> Duration.ofDays(amount);
        };
        if (length.compareTo(LONGEST) > 0) {
            throw new TriageException("Window '" + window + "' is too long. The longest window is 30d.");
        }
        return length;
    }

    private static Instant parseUntil(String until, Instant now) {
        Instant instant;
        try {
            instant = Instant.parse(until.trim());
        } catch (DateTimeParseException e) {
            throw new TriageException("Invalid until '" + until + "'. Use an ISO-8601 UTC instant, for example 2026-01-31T14:00:00Z.");
        }
        if (instant.isAfter(now)) {
            throw new TriageException("until '" + until + "' is in the future. Omit it to end the window now.");
        }
        return instant;
    }
}
