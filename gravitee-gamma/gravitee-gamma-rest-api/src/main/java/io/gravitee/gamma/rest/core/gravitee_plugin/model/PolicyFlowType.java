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
 * The flows a policy plugin declares in its manifest that it can run in.
 *
 * <p>A guardrail flow carries the API type a guardrail runs on and the {@code scope.protocol} designating it. The
 * gateway applies the same table when it deploys a Guardian ({@code GuardianProtocolValidator} in
 * gravitee-reactor-guardian): change both together.
 */
public enum PolicyFlowType {
    HTTP_PROXY("proxy", null, null),
    HTTP_MESSAGE("message", null, null),
    NATIVE_KAFKA(null, null, null),
    MCP_PROXY(null, ApiType.MCP_PROXY, "MCP"),
    LLM_PROXY(null, ApiType.LLM_PROXY, "LLM"),
    A2A_PROXY(null, ApiType.A2A_PROXY, "A2A"),
    GUARDIAN(null, null, null);

    private final String deprecatedManifestKey;
    private final ApiType guardrailApiType;
    private final String scopeProtocol;

    PolicyFlowType(String deprecatedManifestKey, ApiType guardrailApiType, String scopeProtocol) {
        this.deprecatedManifestKey = deprecatedManifestKey;
        this.guardrailApiType = guardrailApiType;
        this.scopeProtocol = scopeProtocol;
    }

    /** The guardrail flow a {@code scope.protocol} designates, compared case-sensitively. */
    public static Optional<PolicyFlowType> fromScopeProtocol(String protocol) {
        return Arrays.stream(values())
            .filter(flow -> flow.scopeProtocol != null && flow.scopeProtocol.equals(protocol))
            .findFirst();
    }

    /** The manifest property listing the phases of this flow ({@code llm_proxy=REQUEST,RESPONSE}). */
    public String manifestKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The property an older plugin declares the phases with, read when {@link #manifestKey()} declares nothing. */
    public Optional<String> deprecatedManifestKey() {
        return Optional.ofNullable(deprecatedManifestKey);
    }

    /** The API type a guardrail declaring this flow runs on, for a guardrail flow. */
    public Optional<ApiType> guardrailApiType() {
        return Optional.ofNullable(guardrailApiType);
    }
}
