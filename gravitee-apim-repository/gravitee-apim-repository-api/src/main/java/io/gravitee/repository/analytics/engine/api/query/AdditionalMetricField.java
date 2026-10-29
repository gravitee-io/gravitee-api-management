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
 * A field a plugin or the gateway attaches to a request as an additional metric, stored under the
 * reporter's {@code additional-metrics} map with its raw key.
 *
 * @param valueType what the type prefix of {@code rawKey} declares
 * @param rawKey    the additional metric key, type prefix included ({@code long_failover_count})
 */
public record AdditionalMetricField(ValueType valueType, String rawKey) implements FilterName, FacetName {
    public static final String NAME_PREFIX = "additional-metrics.";

    @Override
    public String name() {
        return NAME_PREFIX + rawKey;
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
