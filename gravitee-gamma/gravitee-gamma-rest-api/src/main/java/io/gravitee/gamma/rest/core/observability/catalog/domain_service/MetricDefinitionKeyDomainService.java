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
package io.gravitee.gamma.rest.core.observability.catalog.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.rest.core.observability.catalog.exception.InvalidMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.model.MetricDefinitionNames;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Reads the names and keys of additional and custom metric fields with the platform's parser, and checks that a name
 * may carry a record: a user key of either root, not of the reserved {@code additional-metrics.<type>_custom_<name>}
 * form nor its {@code <type>_custom.<name>} decoy, and not read by a built-in entry.
 *
 * @author GraviteeSource Team
 */
@DomainService
@AllArgsConstructor
public class MetricDefinitionKeyDomainService {

    private static final int MAX_NAME_LENGTH = 256;
    private static final int ABBREVIATED_LENGTH = 32;
    /** Said only if the grammar refuses a custom name without naming its rule, rather than failing the request. */
    private static final String CUSTOM_NAME_FALLBACK = "it is not a valid custom metric name";

    private final AdditionalMetricGrammarPort grammar;
    private final BuiltInAdditionalMetricsPort builtInAdditionalMetrics;

    /**
     * The field a name designates, or a refusal saying which part of the grammar it breaks. A record describes any user
     * key the grammar reads, an {@code additional-metrics.<type>_<key>} or a {@code custom.<name>};
     * {@link #writableField} narrows that set for writes.
     */
    public AdditionalMetricField field(String name) {
        return grammar.parse(name).orElseThrow(() -> refusal(name));
    }

    /**
     * A key read from documents, kept only when its wire name is of the grammar and its value type is the one the grammar
     * reads: discovered keys come from traffic, not from a validated write.
     */
    public Optional<AdditionalMetricField> discovered(DiscoveredAdditionalMetric discovered) {
        return grammar.parse(discovered.name()).filter(field -> field.valueType() == discovered.valueType());
    }

    /** The field of a key, type prefix included; empty when the key is outside the grammar. */
    public Optional<AdditionalMetricField> fieldOfKey(String rawKey) {
        return grammar.parse(AdditionalMetricField.NAME_PREFIX + rawKey);
    }

    public AdditionalMetricField writableField(String name) {
        AdditionalMetricField field = field(name);
        // Read as written so such a key stays listed, it is never declared: its name would reach messages and logs.
        if (MetricDefinitionNames.holdsUnprintable(name)) {
            throw InvalidMetricDefinitionException.unprintableName(name);
        }
        if (field.isReserved()) {
            throw InvalidMetricDefinitionException.reservedKey(name);
        }
        if (field.namesCustomMetric()) {
            throw InvalidMetricDefinitionException.customMetricKey(name, field.customMetricLookalike());
        }
        if (builtInWireNames(builtInAdditionalMetrics.builtInNamesByRawKey()).contains(field.name())) {
            throw InvalidMetricDefinitionException.builtInKey(name);
        }
        return field;
    }

    /** Explains a name the parser refused; the parser alone decides what is accepted. */
    private InvalidMetricDefinitionException refusal(String name) {
        if (name == null || name.isBlank()) {
            return InvalidMetricDefinitionException.invalidName(
                "A metric definition needs the name of an additional or custom metric field"
            );
        }
        if (name.startsWith(AdditionalMetricField.Root.CUSTOM.prefix())) {
            return InvalidMetricDefinitionException.invalidName(
                "'%s' is not a valid field name: %s".formatted(abbreviated(name), grammar.refusal(name).orElse(CUSTOM_NAME_FALLBACK))
            );
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return InvalidMetricDefinitionException.invalidName(
                "'%s' is longer than %d characters".formatted(abbreviated(name), MAX_NAME_LENGTH)
            );
        }
        if (!name.startsWith(AdditionalMetricField.NAME_PREFIX)) {
            return InvalidMetricDefinitionException.invalidName(
                "'%s' is not an additional or custom metric field: it must start with '%s' or '%s'".formatted(
                    name,
                    AdditionalMetricField.NAME_PREFIX,
                    AdditionalMetricField.Root.CUSTOM.prefix()
                )
            );
        }
        if (grammar.typeDeclaredBy(name.substring(AdditionalMetricField.NAME_PREFIX.length())).isEmpty()) {
            return InvalidMetricDefinitionException.unknownTypePrefix(name);
        }
        return InvalidMetricDefinitionException.invalidName(
            "'%s' is not a valid additional metric field: after its type prefix, use letters, digits, '-', '_' and '/' in segments joined by single dots".formatted(
                name
            )
        );
    }

    /**
     * The wire names of the keys built-ins read, in a set the caller may add to. Built-ins read keys of the
     * {@code additional-metrics} map, resolved to {@code additional-metrics.<rawKey>} by the OBS-70 identity rule: a
     * custom key spelt like one of them is another field.
     */
    public static Set<String> builtInWireNames(Map<String, Set<String>> builtInNamesByRawKey) {
        Set<String> names = new HashSet<>();
        builtInNamesByRawKey.keySet().forEach(rawKey -> names.add(AdditionalMetricField.NAME_PREFIX + rawKey));
        return names;
    }

    private static String abbreviated(String name) {
        return name.length() > ABBREVIATED_LENGTH ? name.substring(0, ABBREVIATED_LENGTH) + "…" : name;
    }
}
