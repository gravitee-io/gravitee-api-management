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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Describes what the widget editor offers for some API kinds: their metrics, each once, grouped by family,
 * and the facets those metrics can be grouped by.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityAnalyticsDefinitionUseCase {

    private final AnalyticsDefinitionPort analyticsDefinitionPort;

    public record Input(Set<ApiType> apiTypes) {}

    public record Output(AnalyticsDefinition definition) {}

    public Output execute(Input input) {
        if (input.apiTypes() == null || input.apiTypes().isEmpty()) {
            throw InvalidObservabilityQueryException.missingApiTypes();
        }
        // Kind order, not request order: the same kinds always get the same answer, and the same ETag.
        var kinds = EnumSet.copyOf(input.apiTypes());
        Map<String, MetricDefinition> metrics = new LinkedHashMap<>();
        for (var kind : kinds) {
            if (!ApiType.API_KINDS.contains(kind)) {
                throw InvalidObservabilityQueryException.notAnApiKind(kind.name());
            }
            analyticsDefinitionPort.metricsOf(kind).forEach(metric -> metrics.putIfAbsent(metric.name(), metric));
        }
        return new Output(AnalyticsDefinition.of(metrics.values()));
    }
}
