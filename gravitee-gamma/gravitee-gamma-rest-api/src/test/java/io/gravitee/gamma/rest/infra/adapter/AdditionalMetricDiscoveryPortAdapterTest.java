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

import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricDiscovery;
import io.gravitee.apim.core.analytics_engine.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.port.AdditionalMetricDiscoveryPortContractTest;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import java.util.List;

/** The contract on the platform's pass, served by a stub of the platform's query service. */
class AdditionalMetricDiscoveryPortAdapterTest extends AdditionalMetricDiscoveryPortContractTest {

    @Override
    protected AdditionalMetricDiscoveryPort portServing(List<Seen> found, boolean truncated) {
        var pass = new AdditionalMetricDiscovery(
            found
                .stream()
                .map(seen -> new DiscoveredAdditionalMetric(seen.rawKey(), seen.lastSeen()))
                .toList(),
            REFRESHED_AT,
            truncated
        );
        return new AdditionalMetricDiscoveryPortAdapter((organizationId, environmentId) -> pass, new AdditionalMetricGrammarPortAdapter());
    }
}
