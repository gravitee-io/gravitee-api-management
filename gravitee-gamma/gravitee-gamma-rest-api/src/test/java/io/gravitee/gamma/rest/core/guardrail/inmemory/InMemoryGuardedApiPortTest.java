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
package io.gravitee.gamma.rest.core.guardrail.inmemory;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardrail.port.GuardedApiPortContractTest;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;

/** The double the domain tests run on honours the port contract. */
class InMemoryGuardedApiPortTest extends GuardedApiPortContractTest {

    private final InMemoryGuardedApiPort port = new InMemoryGuardedApiPort();

    @Override
    protected GuardedApiPort port() {
        return port;
    }

    @Override
    protected void givenApi(String environmentId, String apiId, String name, Flow internalFlow) {
        port.givenApi(
            GuardedApi.builder()
                .id(apiId)
                .environmentId(environmentId)
                .name(name)
                .type(ApiType.LLM_PROXY)
                .supportsGuardrails(true)
                .internalFlow(internalFlow)
                .build()
        );
    }
}
