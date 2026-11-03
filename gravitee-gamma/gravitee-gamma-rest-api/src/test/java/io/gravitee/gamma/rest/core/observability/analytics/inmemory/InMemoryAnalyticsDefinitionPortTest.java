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
package io.gravitee.gamma.rest.core.observability.analytics.inmemory;

import io.gravitee.gamma.rest.core.observability.analytics.port.AnalyticsDefinitionPortContractTest;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;

class InMemoryAnalyticsDefinitionPortTest extends AnalyticsDefinitionPortContractTest {

    private final InMemoryAnalyticsDefinitionPort port = new InMemoryAnalyticsDefinitionPort();

    InMemoryAnalyticsDefinitionPortTest() {
        port.givenMetric("HTTP_REQUESTS", List.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP), "API", "HTTP_STATUS");
        port.givenMetric("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM), "API", "LLM_PROXY_MODEL");
        port.givenMetric("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE), "NATIVE_TOPIC");
        port.givenMetric("AUTHZ_DECISIONS", List.of(ApiType.AUTHZ), "AUTHZ_DECISION");
    }

    @Override
    protected AnalyticsDefinitionPort port() {
        return port;
    }
}
