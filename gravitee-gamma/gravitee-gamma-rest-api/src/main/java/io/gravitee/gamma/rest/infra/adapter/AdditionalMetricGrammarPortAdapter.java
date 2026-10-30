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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.observability.model.AdditionalMetricField.ValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import java.util.Arrays;
import java.util.Optional;

/**
 * Reads additional metric names with the APIM-core parser, so a key Gamma accepts is a key the engines accept.
 *
 * @author GraviteeSource Team
 */
public class AdditionalMetricGrammarPortAdapter implements AdditionalMetricGrammarPort {

    @Override
    public Optional<AdditionalMetricField> parse(String name) {
        return io.gravitee.apim.core.observability.model.AdditionalMetricField.parse(name).map(field ->
            new AdditionalMetricField(field.rawKey(), AdditionalMetricValueType.valueOf(field.valueType().name()))
        );
    }

    @Override
    public Optional<AdditionalMetricValueType> typeDeclaredBy(String rawKey) {
        return Arrays.stream(ValueType.values())
            .filter(type -> rawKey != null && rawKey.startsWith(type.prefix()))
            .findFirst()
            .map(type -> AdditionalMetricValueType.valueOf(type.name()));
    }
}
