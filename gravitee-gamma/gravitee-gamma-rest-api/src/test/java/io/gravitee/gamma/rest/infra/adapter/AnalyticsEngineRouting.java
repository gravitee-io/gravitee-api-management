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

import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.infra.query_service.analytics_engine.AuthzAnalyticsQueryService;
import io.gravitee.apim.infra.query_service.analytics_engine.AuthzTrafficAnalyticsQueryService;
import io.gravitee.apim.infra.query_service.analytics_engine.EventMetricsAnalyticsQueryService;
import io.gravitee.apim.infra.query_service.analytics_engine.HTTPDataPlaneAnalyticsQueryService;
import io.gravitee.apim.infra.query_service.analytics_engine.MessageDataPlaneQueryService;
import io.gravitee.apim.infra.query_service.analytics_engine.NativeApiAnalyticsQueryService;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.List;

/** The engine's routing of metrics to query services, as shipped, over a repository no test here reads. */
final class AnalyticsEngineRouting {

    private AnalyticsEngineRouting() {}

    static AnalyticsQueryContextProvider provider() {
        var repository = mock(AnalyticsRepository.class);
        return new AnalyticsQueryContextProvider(
            List.of(
                new HTTPDataPlaneAnalyticsQueryService(repository),
                new MessageDataPlaneQueryService(repository),
                new NativeApiAnalyticsQueryService(repository),
                new EventMetricsAnalyticsQueryService(repository),
                new AuthzAnalyticsQueryService(repository),
                new AuthzTrafficAnalyticsQueryService(repository)
            )
        );
    }
}
