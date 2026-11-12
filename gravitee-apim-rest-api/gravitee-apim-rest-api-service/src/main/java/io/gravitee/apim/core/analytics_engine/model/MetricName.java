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
package io.gravitee.apim.core.analytics_engine.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * What a measure aggregates: a built-in metric of the closed {@link MetricSpec.Name} vocabulary, or a field a plugin or
 * the gateway attaches to a request, which enters through {@link AdditionalMetricName} only (OBS-70). Only a field
 * holding a number is a metric; the validator refuses any other.
 */
public sealed interface MetricName permits MetricSpec.Name, AdditionalMetricName {
    /** The wire name: the constant of a built-in, {@code additional-metrics.<rawKey>} otherwise. */
    String name();

    /** The built-in of that name, else the additional metric field it designates; empty when it is neither. */
    static Optional<MetricName> parse(String name) {
        return Arrays.stream(MetricSpec.Name.values())
            .filter(builtIn -> builtIn.name().equals(name))
            .<MetricName>map(builtIn -> builtIn)
            .findFirst()
            .or(() -> AdditionalMetricName.parse(name));
    }
}
