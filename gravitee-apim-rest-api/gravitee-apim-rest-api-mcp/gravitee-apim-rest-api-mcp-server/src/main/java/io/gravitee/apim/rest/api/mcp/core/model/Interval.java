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
import java.util.regex.Pattern;

/**
 * The length of one point of a time series: {@code 30s}, {@code 5m}, {@code 1h}, {@code 1d}, or milliseconds, as the
 * Management API takes it.
 */
public final class Interval {

    private static final Pattern FORMAT = Pattern.compile("^([1-9]\\d*)([smhd])$");

    private Interval() {}

    public static Duration parse(Object interval) {
        if (interval instanceof Number milliseconds && milliseconds.longValue() > 0) {
            return Duration.ofMillis(milliseconds.longValue());
        }
        var matcher = interval == null ? null : FORMAT.matcher(interval.toString().trim());
        if (matcher == null || !matcher.matches()) {
            throw new TriageException(
                "Invalid interval '" +
                    interval +
                    "'. Use a number followed by s, m, h or d, for example 30s or 5m, or a number of milliseconds."
            );
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> Duration.ofDays(amount);
        };
    }
}
