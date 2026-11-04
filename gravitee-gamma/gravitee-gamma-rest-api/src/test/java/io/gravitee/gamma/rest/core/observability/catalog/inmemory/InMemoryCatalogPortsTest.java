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
package io.gravitee.gamma.rest.core.observability.catalog.inmemory;

import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.port.AdditionalMetricDiscoveryPortContractTest;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;
import java.time.Instant;
import java.util.List;

/** The contract on the in-memory port the domain tests use. */
class InMemoryCatalogPortsTest extends AdditionalMetricDiscoveryPortContractTest {

    private final InMemoryCatalogPorts ports = new InMemoryCatalogPorts();

    /** The configured value type is the one the key states; the port keeps it. Keys outside the grammar get any type. */
    @Override
    protected AdditionalMetricDiscoveryPort portServing(List<Seen> found, boolean truncated) {
        var fields = found
            .stream()
            .map(seen -> new DiscoveredAdditionalMetric(seen.rawKey(), typeStatedBy(seen.rawKey()), seen.lastSeen()))
            .toList();
        return ports.givenDiscovered(fields, truncated);
    }

    @Override
    protected Instant refreshedAt() {
        return InMemoryCatalogPorts.REFRESHED_AT;
    }

    private static AdditionalMetricValueType typeStatedBy(String rawKey) {
        return rawKey.startsWith("keyword_") ? AdditionalMetricValueType.KEYWORD : AdditionalMetricValueType.LONG;
    }
}
