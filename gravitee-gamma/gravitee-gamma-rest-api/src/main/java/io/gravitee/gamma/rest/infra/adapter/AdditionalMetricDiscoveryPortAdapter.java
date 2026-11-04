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

import io.gravitee.apim.core.analytics_engine.query_service.AdditionalMetricDiscoveryQueryService;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * {@link AdditionalMetricDiscoveryPort} on the platform's discovery pass, which serves a recent pass per environment.
 * A key the grammar refuses is dropped before the cap, so it takes no one's place.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class AdditionalMetricDiscoveryPortAdapter implements AdditionalMetricDiscoveryPort {

    private final AdditionalMetricDiscoveryQueryService queryService;
    private final AdditionalMetricGrammarPort grammar;

    @Override
    public AdditionalMetricDiscovery discover(String organizationId, String environmentId, Set<String> uncountedKeys) {
        var pass = queryService.discover(organizationId, environmentId);
        Map<String, AdditionalMetricField> fieldsByKey = new HashMap<>();
        pass
            .metrics()
            .forEach(metric ->
                grammar
                    .parse(AdditionalMetricField.NAME_PREFIX + metric.rawKey())
                    .ifPresent(field -> fieldsByKey.put(metric.rawKey(), field))
            );
        var capped = pass.retainKeys(fieldsByKey::containsKey).capDiscovered(uncountedKeys);
        var fields = capped
            .metrics()
            .stream()
            .map(metric -> new DiscoveredAdditionalMetric(metric.rawKey(), fieldsByKey.get(metric.rawKey()).valueType(), metric.lastSeen()))
            .toList();
        return new AdditionalMetricDiscovery(fields, capped.refreshedAt(), capped.truncated());
    }
}
