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
package io.gravitee.gamma.rest.core.observability.catalog.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricUnit;

/**
 * A metric definition, or the name it is written on, breaks a rule; answered with {@code 400}. The technical code
 * names the rule.
 *
 * @author GraviteeSource Team
 */
public class InvalidMetricDefinitionException extends ValidationDomainException {

    private static final String CODE_PREFIX = "observability.metric_definition.";

    private InvalidMetricDefinitionException(String message, String rule) {
        super(message, CODE_PREFIX + rule);
    }

    public static InvalidMetricDefinitionException invalidName(String message) {
        return new InvalidMetricDefinitionException(message, "invalid_name");
    }

    public static InvalidMetricDefinitionException unknownTypePrefix(String name) {
        return new InvalidMetricDefinitionException(
            "'%s' has no known type prefix: use long_, int_, double_, bool_, keyword_, string_ or json_".formatted(name),
            "unknown_type_prefix"
        );
    }

    public static InvalidMetricDefinitionException reservedKey(String name) {
        return new InvalidMetricDefinitionException(
            "'%s' is reserved: keys of the form <type>_custom_<name> belong to the typed custom metrics".formatted(name),
            "reserved_key"
        );
    }

    public static InvalidMetricDefinitionException builtInKey(String name) {
        return new InvalidMetricDefinitionException(
            "A built-in metric already uses '%s'; built-in entries are read-only".formatted(name),
            "built_in_key"
        );
    }

    public static InvalidMetricDefinitionException tooLong(String field, int maxLength) {
        return new InvalidMetricDefinitionException("The %s is longer than %d characters".formatted(field, maxLength), field + "_too_long");
    }

    public static InvalidMetricDefinitionException invalidUnit(String unit) {
        return new InvalidMetricDefinitionException(
            "'%s' is not a unit: use a named unit or a suffix of at most %d printable characters".formatted(
                unit,
                AdditionalMetricUnit.MAX_SUFFIX_LENGTH
            ),
            "invalid_unit"
        );
    }

    public static InvalidMetricDefinitionException missingBody() {
        return new InvalidMetricDefinitionException("Request body is required", "invalid_body");
    }
}
