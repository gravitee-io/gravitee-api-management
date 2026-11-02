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
package io.gravitee.gateway.reactive.core.tracing;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gateway-wide sampling of traced APIs, read from {@code services.opentelemetry.tracing.sampling.*}. A runtime overlay
 * replaces it for its API while it is active. Lists are comma-separated so they can be set from environment variables.
 */
public final class TracingSamplingSettings {

    /** Internal attribute holding the sampling config that applied to the request, read when the root span ends. */
    public static final String ATTR_INTERNAL_TRACING_SAMPLING = "tracing.sampling";

    private static final String PREFIX = "services.opentelemetry.tracing.sampling.";

    private TracingSamplingSettings() {}

    public static SamplingConfig from(final Configuration configuration) {
        return new SamplingConfig(
            configuration.getProperty(PREFIX + "ratio", Double.class, 1.0),
            configuration.getProperty(PREFIX + "respectRemoteParent", Boolean.class, true),
            Set.copyOf(list(configuration.getProperty(PREFIX + "methods", String.class, ""))),
            list(configuration.getProperty(PREFIX + "pathPrefixes", String.class, "")),
            list(configuration.getProperty(PREFIX + "keepStatuses", String.class, ""))
                .stream()
                .map(Integer::valueOf)
                .collect(Collectors.toSet()),
            configuration.getProperty(PREFIX + "keepErrors", Boolean.class, false)
        );
    }

    private static List<String> list(final String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }
}
