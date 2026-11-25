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
package io.gravitee.repository.analytics.engine.api.query;

/**
 * A field a plugin, the gateway or a policy attaches to a request, stored under one of the reporter's maps with its raw
 * key: {@code additional-metrics} (typed by the key's prefix) or {@code custom} (assign-metrics values, keywords).
 *
 * @param root      the map the reporter writes the key under
 * @param valueType what the type prefix of {@code rawKey} declares; a keyword under {@link Root#CUSTOM}
 * @param rawKey    the key under its root, type prefix included for an additional metric ({@code long_failover_count})
 */
public record AdditionalMetricField(Root root, ValueType valueType, String rawKey) implements FilterName, FacetName {
    public static final String NAME_PREFIX = Root.ADDITIONAL_METRICS.prefix();

    /** A custom key holds keywords only: an engine would type any other value as a number on a keyword field. */
    public AdditionalMetricField {
        if (root == Root.CUSTOM && valueType != ValueType.KEYWORD) {
            throw new IllegalArgumentException("Custom metric key " + rawKey + " holds keywords, not " + valueType);
        }
    }

    /** A field of the {@code additional-metrics} map. */
    public AdditionalMetricField(ValueType valueType, String rawKey) {
        this(Root.ADDITIONAL_METRICS, valueType, rawKey);
    }

    /** The wire name, which is also the path of the field on a request document. */
    @Override
    public String name() {
        return root.prefix() + rawKey;
    }

    public enum Root {
        ADDITIONAL_METRICS("additional-metrics."),
        CUSTOM("custom.");

        private final String prefix;

        Root(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }
    }

    public enum ValueType {
        KEYWORD,
        LONG,
        INT,
        DOUBLE,
        BOOL,
        STRING,
        JSON,
    }
}
