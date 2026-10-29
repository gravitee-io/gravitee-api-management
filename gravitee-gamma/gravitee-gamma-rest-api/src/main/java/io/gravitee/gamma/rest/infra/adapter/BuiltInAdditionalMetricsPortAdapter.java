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

import io.gravitee.apim.core.analytics_engine.query_service.BuiltInAdditionalMetricsQueryService;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.BuiltInAdditionalMetricsPort;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * {@link BuiltInAdditionalMetricsPort} on the platform's analytics engine, which computes the set from its field
 * resolvers and logs-path adapters rather than from a list of its own.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class BuiltInAdditionalMetricsPortAdapter implements BuiltInAdditionalMetricsPort {

    private final BuiltInAdditionalMetricsQueryService queryService;

    @Override
    public Map<String, Set<String>> builtInNamesByRawKey() {
        return queryService.namesByRawKey();
    }
}
