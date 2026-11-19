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
package io.gravitee.apim.reporter.elasticsearch.config;

import static java.lang.String.format;

import io.gravitee.elasticsearch.utils.Type;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * How long Elastic Cloud Serverless keeps the documents of each data stream: the configured {@code lifecycle.retention.<type>}, or
 * {@value #DEFAULT_RETENTION}.
 */
public final class ServerlessRetention {

    public static final String DEFAULT_RETENTION = "45d";
    public static final String ALLOW_DECREASE_SETTING = "reporters.elasticsearch.lifecycle.retention.allow_decrease";
    private static final Pattern DURATION = Pattern.compile("([0-9]+)(nanos|micros|ms|s|m|h|d)");
    private static final Map<String, ChronoUnit> UNITS = Map.of(
        "nanos",
        ChronoUnit.NANOS,
        "micros",
        ChronoUnit.MICROS,
        "ms",
        ChronoUnit.MILLIS,
        "s",
        ChronoUnit.SECONDS,
        "m",
        ChronoUnit.MINUTES,
        "h",
        ChronoUnit.HOURS,
        "d",
        ChronoUnit.DAYS
    );

    private final Map<LifecycleGroup, String> retentions;
    private final boolean allowDecrease;

    private ServerlessRetention(Map<LifecycleGroup, String> retentions, boolean allowDecrease) {
        this.retentions = retentions;
        this.allowDecrease = allowDecrease;
    }

    public static ServerlessRetention of(ReporterConfiguration configuration) {
        final Map<LifecycleGroup, String> retentions = new EnumMap<>(LifecycleGroup.class);
        for (LifecycleGroup group : LifecycleGroup.values()) {
            retentions.put(group, configuration.dataRetention(group).orElse(DEFAULT_RETENTION));
        }
        return new ServerlessRetention(retentions, configuration.isDataRetentionAllowDecrease());
    }

    /**
     * @return why a configured retention cannot be sent to Elasticsearch, naming the setting, or empty when all can
     */
    public static Optional<String> violation(ReporterConfiguration configuration) {
        return Arrays.stream(LifecycleGroup.values())
            .filter(group ->
                configuration
                    .dataRetention(group)
                    .filter(retention -> !isPositiveDuration(retention))
                    .isPresent()
            )
            .findFirst()
            .map(group ->
                format(
                    "'%s' is set to '%s', which is not a positive duration Elasticsearch accepts: use a whole number and a unit, such as '%s' or '12h'.",
                    group.retentionSetting(),
                    configuration.dataRetention(group).orElseThrow(),
                    DEFAULT_RETENTION
                )
            );
    }

    /**
     * @param retention a duration in the Elasticsearch time unit format, such as {@code 45d}
     */
    public static Duration duration(String retention) {
        return parse(retention).orElseThrow(() -> new IllegalArgumentException(format("'%s' is not an Elasticsearch duration", retention)));
    }

    private static boolean isPositiveDuration(String retention) {
        return parse(retention)
            .filter(duration -> duration.isPositive())
            .isPresent();
    }

    private static Optional<Duration> parse(String retention) {
        final Matcher matcher = DURATION.matcher(retention);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.of(Long.parseLong(matcher.group(1)), UNITS.get(matcher.group(2))));
        } catch (ArithmeticException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String of(Type type) {
        return retentions.get(LifecycleGroup.of(type));
    }

    /**
     * @return whether a retention shorter than the one of an existing data stream is applied
     */
    public boolean allowDecrease() {
        return allowDecrease;
    }

    /**
     * @return the retention of each setting, such as {@code request=45d, log=7d, ...}
     */
    public String summary() {
        return retentions
            .entrySet()
            .stream()
            .map(entry -> entry.getKey().key() + '=' + entry.getValue())
            .collect(Collectors.joining(", "));
    }
}
