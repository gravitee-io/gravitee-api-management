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
 * A field a plugin, the gateway or a policy attaches to a request, stored under one of the two maps the reporter
 * writes (OBS-70, OBS-171):
 * <ul>
 *   <li>{@code additional-metrics.<rawKey>}: the raw key's type prefix up to the first {@code _} gives the value type,
 *       so the name reads back without a lookup;</li>
 *   <li>{@code custom.<rawKey>}: a value the assign-metrics policy writes, always a keyword.</li>
 * </ul>
 *
 * <p>{@link #parse(String)} is the one reader of that grammar: every surface accepting a dynamic name, analytics
 * or logs, goes through it.
 *
 * @param root      the map the reporter writes the key under
 * @param valueType what the type prefix of {@code rawKey} declares; {@link ValueType#KEYWORD} under {@link Root#CUSTOM}
 * @param rawKey    the key as the reporter writes it under its root ({@code long_failover_count}, {@code fraud-tier})
 */
public record AdditionalMetricField(Root root, ValueType valueType, String rawKey) {
    public static final String NAME_PREFIX = Root.ADDITIONAL_METRICS.prefix();
    public static final int MAX_NAME_LENGTH = 256;
    public static final int MAX_CUSTOM_KEY_LENGTH = 256;

    // Possessive: a segment never gives characters back, so matching cannot backtrack nor recurse per segment.
    private static final Pattern RAW_KEY = Pattern.compile(
        "(?:long|int|double|bool|keyword|string|json)_[A-Za-z0-9_/-]++(?:\\.[A-Za-z0-9_/-]++)*+"
    );

    /** Holds only a key of its root's grammar, typed by its own prefix or a keyword: the field path derives from nothing else. */
    public AdditionalMetricField {
        if (root == null) {
            throw new IllegalArgumentException("No root for additional metric key " + rawKey);
        }
        switch (root) {
            case ADDITIONAL_METRICS -> {
                if (rawKey == null || NAME_PREFIX.length() + rawKey.length() > MAX_NAME_LENGTH || !RAW_KEY.matcher(rawKey).matches()) {
                    throw new IllegalArgumentException("Not an additional metric key: " + rawKey);
                }
                if (valueType != ValueType.ofRawKey(rawKey)) {
                    throw new IllegalArgumentException("Additional metric key " + rawKey + " is not of type " + valueType);
                }
            }
            case CUSTOM -> {
                if (!isCustomKey(rawKey)) {
                    throw new IllegalArgumentException("Not a custom metric key: " + rawKey);
                }
                if (valueType != ValueType.KEYWORD) {
                    throw new IllegalArgumentException("Custom metric key " + rawKey + " holds keywords, not " + valueType);
                }
            }
        }
    }

    /** A field of the {@code additional-metrics} map, typed by the prefix of its raw key. */
    public AdditionalMetricField(ValueType valueType, String rawKey) {
        this(Root.ADDITIONAL_METRICS, valueType, rawKey);
    }

    /** The field a well-formed dynamic name designates; empty for anything else, built-in names included. */
    public static Optional<AdditionalMetricField> parse(String name) {
        if (name == null) {
            return Optional.empty();
        }
        if (name.startsWith(Root.CUSTOM.prefix())) {
            var key = name.substring(Root.CUSTOM.prefix().length());
            return isCustomKey(key) ? Optional.of(new AdditionalMetricField(Root.CUSTOM, ValueType.KEYWORD, key)) : Optional.empty();
        }
        if (name.length() > MAX_NAME_LENGTH || !name.startsWith(NAME_PREFIX)) {
            return Optional.empty();
        }
        var rawKey = name.substring(NAME_PREFIX.length());
        if (!RAW_KEY.matcher(rawKey).matches()) {
            return Optional.empty();
        }
        return Optional.of(new AdditionalMetricField(ValueType.ofRawKey(rawKey), rawKey));
    }

    /** The wire name, {@code <root>.<rawKey>}: the path of the field on a request document. */
    public String name() {
        return root.prefix() + rawKey;
    }

    /**
     * Any key the reporter can index as a field, kept as written: Elasticsearch refuses an empty segment, the template
     * writes the key unescaped (a quote or a backslash breaks the document), {@code *} reads as a pattern in a field
     * query, and the policy never writes a blank name.
     */
    private static boolean isCustomKey(String key) {
        return (
            key != null &&
            !key.isBlank() &&
            key.length() <= MAX_CUSTOM_KEY_LENGTH &&
            !key.startsWith(".") &&
            !key.endsWith(".") &&
            !key.contains("..") &&
            key.chars().noneMatch(c -> c == '"' || c == '\\' || c == '*')
        );
    }

    /** The map of a request document the reporter writes a key under. */
    public enum Root {
        ADDITIONAL_METRICS("additional-metrics."),
        CUSTOM("custom.");

        private final String prefix;

        Root(String prefix) {
            this.prefix = prefix;
        }

        /** The start of the wire name of a key under this root, the {@code .} included. */
        public String prefix() {
            return prefix;
        }
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
