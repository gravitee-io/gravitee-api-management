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
package io.gravitee.rest.api.service.impl.search.lucene.utils;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LuceneTransformerUtils {

    public static String generateApiType(Api api) {
        boolean isTcpApi =
            api.getType() == ApiType.PROXY &&
            Objects.nonNull(api.getApiListeners()) &&
            api
                .getApiListeners()
                .stream()
                .anyMatch(listener -> listener.getType() == ListenerType.TCP);
        return generateApiType(api.getDefinitionVersion(), api.getType(), isTcpApi);
    }

    public static String generateApiType(GenericApiEntity api) {
        if (api instanceof ApiEntity) {
            return generateApiType((ApiEntity) api);
        }
        if (api instanceof io.gravitee.rest.api.model.v4.agent.AgentApiEntity) {
            // AgentApiEntity carries AGENT as its (REST) discriminator, but it is indexed like a V4 API of type AGENT
            // ("V4_AGENT") so it stays consistent with the core indexer and the api_type search filter.
            return generateApiType(DefinitionVersion.V4, ApiType.AGENT);
        }

        return api.getDefinitionVersion().name();
    }

    private static String generateApiType(ApiEntity api) {
        boolean isTcpApi =
            api.getType() == ApiType.PROXY &&
            Objects.nonNull(api.getListeners()) &&
            api
                .getListeners()
                .stream()
                .anyMatch(listener -> listener.getType() == ListenerType.TCP);
        return generateApiType(api.getDefinitionVersion(), api.getType(), isTcpApi);
    }

    /**
     * The indexed {@code api_type} value ("&lt;definitionVersion&gt;_&lt;TYPE&gt;") for a non-TCP api type. Use this to
     * build search filters/exclusions so callers stay in sync with the indexer's key format.
     */
    public static String generateApiType(DefinitionVersion definitionVersion, ApiType apiType) {
        return generateApiType(definitionVersion, apiType, false);
    }

    private static String generateApiType(DefinitionVersion definitionVersion, ApiType apiType, boolean isTcpApi) {
        if (definitionVersion == DefinitionVersion.V4) {
            String type = switch (apiType) {
                case A2A_PROXY -> "A2A_PROXY";
                case AUTHZ -> "AUTHZ";
                case EDGE -> "EDGE";
                case LLM_PROXY -> "LLM_PROXY";
                case MCP_PROXY -> "MCP_PROXY";
                case MESSAGE -> "MESSAGE";
                case AGENT -> "AGENT";
                case NATIVE -> "KAFKA";
                case PROXY -> isTcpApi ? "TCP_PROXY" : "HTTP_PROXY";
            };
            return definitionVersion.name() + "_" + type;
        }
        return definitionVersion.name();
    }
}
