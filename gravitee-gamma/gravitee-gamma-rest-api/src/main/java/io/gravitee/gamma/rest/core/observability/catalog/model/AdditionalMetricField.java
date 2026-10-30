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
package io.gravitee.gamma.rest.core.observability.catalog.model;

/**
 * An additional metric field: the key a plugin or the gateway writes ({@code rawKey}, type prefix included) and its
 * value type. Its wire name is {@code additional-metrics.<rawKey>}. Names are read by the platform's parser, through
 * {@link io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort}.
 *
 * @author GraviteeSource Team
 */
public record AdditionalMetricField(String rawKey, AdditionalMetricValueType valueType) {
    public static final String NAME_PREFIX = "additional-metrics.";
    private static final String RESERVED_SEGMENT = "custom_";

    public String name() {
        return NAME_PREFIX + rawKey;
    }

    /** {@code <type>_custom_<name>}: kept for the typed custom metrics of assign-metrics. */
    public boolean isReserved() {
        return rawKey.startsWith(valueType.prefix() + RESERVED_SEGMENT);
    }

    public String derivedLabel() {
        return AdditionalMetricLabel.derive(this);
    }
}
