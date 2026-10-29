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
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import java.util.Optional;
import lombok.AllArgsConstructor;

/**
 * Reads the names and keys of additional metric fields with the platform's parser, and checks that a name may carry a
 * record: an additional metric field, not of the reserved {@code <type>_custom_<name>} form, and not read by a
 * built-in entry.
 *
 * @author GraviteeSource Team
 */
@DomainService
@AllArgsConstructor
public class MetricDefinitionKeyDomainService {

    private static final int MAX_NAME_LENGTH = 256;

    private final AdditionalMetricGrammarPort grammar;
    private final BuiltInAdditionalMetricsPort builtInAdditionalMetrics;

    /** The field a name designates, or a refusal saying which part of the grammar it breaks. */
    public AdditionalMetricField field(String name) {
        return grammar.parse(name).orElseThrow(() -> refusal(name));
    }

    /**
     * A key read from documents, kept only when it is of the grammar and its value type is the one its prefix states:
     * discovered keys come from traffic, not from a validated write.
     */
    public Optional<AdditionalMetricField> discovered(String rawKey, AdditionalMetricValueType valueType) {
        return grammar.parse(AdditionalMetricField.NAME_PREFIX + rawKey).filter(field -> field.valueType() == valueType);
    }

    public AdditionalMetricField writableField(String name) {
        AdditionalMetricField field = field(name);
        if (field.isReserved()) {
            throw InvalidMetricDefinitionException.reservedKey(name);
        }
        // A built-in reading a key resolves to additional-metrics.<rawKey>, the field the OBS-70 identity rule gives
        // the dynamic name of that key, so comparing keys compares the resolved fields.
        if (builtInAdditionalMetrics.builtInNamesByRawKey().containsKey(field.rawKey())) {
            throw InvalidMetricDefinitionException.builtInKey(name);
        }
        return field;
    }

    /** Explains a name the parser refused; the parser alone decides what is accepted. */
    private static InvalidMetricDefinitionException refusal(String name) {
        if (name == null || name.isBlank()) {
            return InvalidMetricDefinitionException.invalidName("A metric definition needs the name of an additional metric field");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return InvalidMetricDefinitionException.invalidName(
                "'%s' is longer than %d characters".formatted(name.substring(0, 32) + "…", MAX_NAME_LENGTH)
            );
        }
        if (!name.startsWith(AdditionalMetricField.NAME_PREFIX)) {
            return InvalidMetricDefinitionException.invalidName(
                "'%s' is not an additional metric field: it must start with '%s'".formatted(name, AdditionalMetricField.NAME_PREFIX)
            );
        }
        if (AdditionalMetricValueType.ofRawKey(name.substring(AdditionalMetricField.NAME_PREFIX.length())).isEmpty()) {
            return InvalidMetricDefinitionException.unknownTypePrefix(name);
        }
        return InvalidMetricDefinitionException.invalidName(
            "'%s' is not a valid additional metric field: after its type prefix, use letters, digits, '-', '_' and '/' in segments joined by single dots".formatted(
                name
            )
        );
    }
}
