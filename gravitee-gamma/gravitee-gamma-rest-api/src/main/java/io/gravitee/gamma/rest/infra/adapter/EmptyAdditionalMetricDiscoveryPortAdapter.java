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

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricDiscoveryPort;

/**
 * Stands in for the discovery pass until it exists (OBS-74): a pass that finds nothing, run at each call. The list
 * then serves built-in and declared entries only, with no {@code lastSeen}.
 *
 * @author GraviteeSource Team
 */
public class EmptyAdditionalMetricDiscoveryPortAdapter implements AdditionalMetricDiscoveryPort {

    @Override
    public AdditionalMetricDiscovery discover(String organizationId, String environmentId) {
        return AdditionalMetricDiscovery.empty(TimeProvider.instantNow());
    }
}
