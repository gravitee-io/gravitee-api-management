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
package io.gravitee.gamma.rest.core.observability.catalog.port.service_provider;

import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import java.util.Optional;

/**
 * Port onto the platform's reader of additional metric names (OBS-70), the one grammar the analytics and logs
 * engines also accept.
 *
 * @author GraviteeSource Team
 */
public interface AdditionalMetricGrammarPort {
    /** The field a well-formed {@code additional-metrics.<rawKey>} name designates; empty for anything else. */
    Optional<AdditionalMetricField> parse(String name);

    /**
     * The value type the type prefix of {@code rawKey} declares, whether the rest of the key is well-formed or not:
     * what a refusal explains. Empty when the key starts with no type prefix.
     */
    Optional<AdditionalMetricValueType> typeDeclaredBy(String rawKey);
}
