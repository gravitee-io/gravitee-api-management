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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.log.adapter.authz.AuthzDecisionLogFields;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The additional metrics read under a built-in name, computed from the places that turn a built-in name into a
 * field rather than listed by hand:
 *
 * <ul>
 *   <li>every metric, filter and facet run through every analytics {@link FieldResolver}, the companion fields read
 *       by {@link FieldResolver#entityTypeFromFilter} included (mapped to no name: they are read, never served);</li>
 *   <li>the fields the logs path reads: the additional metric fields of {@link RequestV2MetricsV4Fields} and
 *       {@link AuthzDecisionLogFields}, and the keys of {@link NativeApiMetricKeys}, which the logs search, the log
 *       details and logs-only filters such as {@code NATIVE_CLIENT_SOFTWARE_VERSION} name directly, as constants
 *       rather than through a built-in name.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
public final class BuiltInAdditionalMetricKeys {

    private static final String ADDITIONAL_METRICS_PREFIX = RequestV2MetricsV4Fields.ADDITIONAL_METRICS + ".";

    /** Every {@link FieldResolver} of this package; {@code BuiltInAdditionalMetricKeysTest} fails when one is missing. */
    static final List<FieldResolver> RESOLVERS = List.of(
        new HTTPFieldResolver(),
        new MessageFieldResolver(),
        new NativeApiFieldResolver(),
        new EventMetricsFieldResolver(),
        new AuthzFieldResolver(),
        new AuthzTrafficFieldResolver()
    );

    private static final Map<String, Set<String>> NAMES_BY_KEY = compute();

    private BuiltInAdditionalMetricKeys() {}

    /**
     * Each key, type prefix included, with the built-in names it is served under; empty when only a constant or a
     * companion lookup reads it.
     */
    public static Map<String, Set<String>> all() {
        return NAMES_BY_KEY;
    }

    private static Map<String, Set<String>> compute() {
        var namesByKey = new TreeMap<String, Set<String>>();
        for (var resolver : RESOLVERS) {
            for (var metric : Metric.values()) {
                add(namesByKey, resolve(() -> resolver.fromMetric(metric)), metric.name());
            }
            for (var facet : Facet.values()) {
                add(namesByKey, resolve(() -> resolver.fromFacet(facet)), facet.name());
            }
            for (var name : Filter.Name.values()) {
                var filter = new Filter(name, Filter.Operator.EQ, "value");
                add(namesByKey, resolve(() -> resolver.fromFilter(filter)), name.name());
                // A companion is read alongside the filter's own field, not served under its name.
                add(namesByKey, resolve(() -> resolver.entityTypeFromFilter(filter)), null);
            }
        }
        constants(RequestV2MetricsV4Fields.class, RequestV2MetricsV4Fields.Field.class).forEach(field ->
            add(namesByKey, field.v4Metrics(), null)
        );
        constants(NativeApiMetricKeys.class, String.class).forEach(key -> add(namesByKey, ADDITIONAL_METRICS_PREFIX + key, null));
        constants(AuthzDecisionLogFields.class, String.class).forEach(field -> add(namesByKey, field, null));
        return namesByKey
            .entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private static void add(Map<String, Set<String>> namesByKey, String field, String name) {
        if (field == null || !field.startsWith(ADDITIONAL_METRICS_PREFIX)) {
            return;
        }
        var names = namesByKey.computeIfAbsent(field.substring(ADDITIONAL_METRICS_PREFIX.length()), key -> new TreeSet<>());
        if (name != null) {
            names.add(name);
        }
    }

    /** A resolver answers {@link UnsupportedOperationException} for a name its search family does not read. */
    private static String resolve(Supplier<String> resolution) {
        try {
            return resolution.get();
        } catch (UnsupportedOperationException notReadByThisFamily) {
            return null;
        }
    }

    private static <T> List<T> constants(Class<?> holder, Class<T> type) {
        return Arrays.stream(holder.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
            .filter(field -> field.getType() == type)
            .map(field -> type.cast(read(field)))
            .toList();
    }

    private static Object read(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read the public constant " + field, e);
        }
    }
}
