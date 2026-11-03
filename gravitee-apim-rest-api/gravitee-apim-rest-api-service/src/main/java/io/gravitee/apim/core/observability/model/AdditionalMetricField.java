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
package io.gravitee.apim.core.observability.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A field a plugin or the gateway attaches to a request as an additional metric, named on the wire
 * {@code additional-metrics.<rawKey>} (OBS-70). The raw key is the key the reporter writes, its type prefix
 * up to the first {@code _} gives the value type, so the name reads back without a lookup.
 *
 * <p>{@link #parse(String)} is the one reader of that grammar: every surface accepting a dynamic name, analytics
 * or logs, goes through it.
 *
 * @param valueType what the type prefix of {@code rawKey} declares
 * @param rawKey    the additional metric key, type prefix included ({@code long_failover_count})
 */
public record AdditionalMetricField(ValueType valueType, String rawKey) {
    public static final String NAME_PREFIX = "additional-metrics.";
    public static final int MAX_NAME_LENGTH = 256;

    // Possessive: a segment never gives characters back, so matching cannot backtrack nor recurse per segment.
    private static final Pattern RAW_KEY = Pattern.compile(
        "(?:long|int|double|bool|keyword|string|json)_[A-Za-z0-9_/-]++(?:\\.[A-Za-z0-9_/-]++)*+"
    );

    /** Holds only a key of the grammar, typed by its own prefix: the field path derives from nothing else. */
    public AdditionalMetricField {
        if (rawKey == null || NAME_PREFIX.length() + rawKey.length() > MAX_NAME_LENGTH || !RAW_KEY.matcher(rawKey).matches()) {
            throw new IllegalArgumentException("Not an additional metric key: " + rawKey);
        }
        if (valueType != ValueType.ofRawKey(rawKey)) {
            throw new IllegalArgumentException("Additional metric key " + rawKey + " is not of type " + valueType);
        }
    }

    /** The field a well-formed dynamic name designates; empty for anything else, built-in names included. */
    public static Optional<AdditionalMetricField> parse(String name) {
        if (name == null || name.length() > MAX_NAME_LENGTH || !name.startsWith(NAME_PREFIX)) {
            return Optional.empty();
        }
        var rawKey = name.substring(NAME_PREFIX.length());
        if (!RAW_KEY.matcher(rawKey).matches()) {
            return Optional.empty();
        }
        return Optional.of(new AdditionalMetricField(ValueType.ofRawKey(rawKey), rawKey));
    }

    /** The wire name, {@code additional-metrics.<rawKey>}. */
    public String name() {
        return NAME_PREFIX + rawKey;
    }

    public enum ValueType {
        KEYWORD("keyword_", Set.of(FilterOperator.EQ, FilterOperator.IN)),
        LONG("long_", Set.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE)),
        INT("int_", Set.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE)),
        DOUBLE("double_", Set.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE)),
        BOOL("bool_", Set.of(FilterOperator.EQ)),
        // Indexed as text: shown in a log's detail, never filtered, grouped or measured.
        STRING("string_", Set.of()),
        JSON("json_", Set.of());

        private final String prefix;
        private final Set<FilterOperator> operators;

        ValueType(String prefix, Set<FilterOperator> operators) {
            this.prefix = prefix;
            this.operators = operators;
        }

        /** The type prefix of a raw key, {@code _} included ({@code long_}). */
        public String prefix() {
            return prefix;
        }

        public Set<FilterOperator> operators() {
            return operators;
        }

        /** A keyword groups by its values, a boolean in its two buckets. */
        public boolean groupsByTerms() {
            return this == KEYWORD || this == BOOL;
        }

        /** A number groups by the ranges the request gives, never by its values. */
        public boolean groupsByRanges() {
            return isNumber();
        }

        public boolean isNumber() {
            return this == LONG || this == INT || this == DOUBLE;
        }

        public boolean isDecimal() {
            return this == DOUBLE;
        }

        private static ValueType ofRawKey(String rawKey) {
            return Arrays.stream(values())
                .filter(type -> rawKey.startsWith(type.prefix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No value type for additional metric key " + rawKey));
        }
    }
}
