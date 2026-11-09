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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import io.gravitee.definition.model.v4.ApiType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The protocols a guardrail can declare in its {@code scope.protocol}, and the API type each one runs on.
 *
 * <p>The gateway applies the same table when it deploys a Guardian ({@code GuardianProtocolValidator} in
 * gravitee-reactor-guardian): change both together.
 */
public enum GuardrailProtocol {
    LLM(ApiType.LLM_PROXY),
    MCP(ApiType.MCP_PROXY),
    A2A(ApiType.A2A_PROXY);

    private final ApiType apiType;

    GuardrailProtocol(ApiType apiType) {
        this.apiType = apiType;
    }

    public static Optional<GuardrailProtocol> fromScope(String protocol) {
        return Arrays.stream(values())
            .filter(value -> value.name().equals(protocol))
            .findFirst();
    }

    public ApiType apiType() {
        return apiType;
    }

    /** The plugin manifest property listing the flow phases a policy supports for this API type. */
    public String manifestKey() {
        return apiType.name().toLowerCase(Locale.ROOT);
    }
}
