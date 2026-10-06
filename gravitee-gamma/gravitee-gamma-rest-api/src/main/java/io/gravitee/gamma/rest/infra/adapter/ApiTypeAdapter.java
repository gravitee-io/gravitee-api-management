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

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.Arrays;
import java.util.Optional;

/**
 * The single translation between the API definition type and the observability API kind, in both
 * directions. The wire names differ ({@code PROXY} is {@code HTTP_PROXY}, {@code LLM_PROXY} is {@code LLM}),
 * and each adapter used to carry its own copy: one of them lacked {@code AUTHZ} and dropped it silently.
 *
 * @author GraviteeSource Team
 */
public final class ApiTypeAdapter {

    private ApiTypeAdapter() {}

    public static ApiType toObservability(io.gravitee.definition.model.v4.ApiType definitionType) {
        if (definitionType == null) {
            return null;
        }
        return switch (definitionType) {
            case PROXY -> ApiType.HTTP_PROXY;
            case MESSAGE -> ApiType.MESSAGE;
            case LLM_PROXY -> ApiType.LLM;
            case MCP_PROXY -> ApiType.MCP;
            case A2A_PROXY -> ApiType.A2A;
            case NATIVE -> ApiType.NATIVE;
            case EDGE -> ApiType.EDGE;
            case AUTHZ -> ApiType.AUTHZ;
        };
    }

    /**
     * Empty for {@link ApiType#AUTHZ_DECISION}, the only token that is not an API kind: a decision record
     * belongs to an API of any type.
     */
    public static Optional<io.gravitee.definition.model.v4.ApiType> toDefinition(ApiType apiType) {
        return Arrays.stream(io.gravitee.definition.model.v4.ApiType.values())
            .filter(definitionType -> toObservability(definitionType) == apiType)
            .findFirst();
    }
}
