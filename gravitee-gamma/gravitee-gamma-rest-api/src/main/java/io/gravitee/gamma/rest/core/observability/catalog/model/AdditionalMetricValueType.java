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

import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import java.util.Arrays;
import java.util.Optional;

/**
 * Value type of an additional metric, read from the type prefix of its key.
 *
 * @author GraviteeSource Team
 */
public enum AdditionalMetricValueType {
    LONG("long_"),
    INT("int_"),
    DOUBLE("double_"),
    BOOL("bool_"),
    KEYWORD("keyword_"),
    STRING("string_"),
    JSON("json_");

    private final String prefix;

    AdditionalMetricValueType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    /** Stored as text: shown in the log detail, never filtered, faceted or measured. */
    public boolean isDetailOnly() {
        return this == STRING || this == JSON;
    }

    /** The filter type a key of this value type is offered under; empty for a detail-only type. */
    public Optional<FilterType> filterType() {
        return switch (this) {
            case KEYWORD -> Optional.of(FilterType.KEYWORD);
            case LONG, INT, DOUBLE -> Optional.of(FilterType.NUMBER);
            case BOOL -> Optional.of(FilterType.BOOLEAN);
            case STRING, JSON -> Optional.empty();
        };
    }

    public static Optional<AdditionalMetricValueType> ofRawKey(String rawKey) {
        return Arrays.stream(values())
            .filter(type -> rawKey != null && rawKey.startsWith(type.prefix))
            .findFirst();
    }
}
