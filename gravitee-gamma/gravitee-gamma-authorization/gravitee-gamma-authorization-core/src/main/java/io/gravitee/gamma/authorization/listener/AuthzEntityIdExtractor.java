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
package io.gravitee.gamma.authorization.listener;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AuthzEntityIdExtractor {

    public static final String API_PREFIX = "api.";
    public static final String MCP_PREFIX = "mcp.";
    public static final String AGENT_PREFIX = "agent.";
    static final String TOOLS_CALL_METHOD = "tools/call";

    public Set<String> extract(Api api) {
        if (api == null) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        String identifier = identifierOf(api);
        ids.add(API_PREFIX + identifier);

        if (api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api v4 && v4.getType() == ApiType.MCP_PROXY) {
            List<Flow> flows = v4.getFlows();
            if (flows != null) {
                for (Flow flow : flows) {
                    extractToolName(flow).ifPresent(tool -> ids.add(MCP_PREFIX + identifier + "." + tool));
                }
            }
        }
        return ids;
    }

    public static String identifierOf(Api api) {
        return api.getCrossId() != null && !api.getCrossId().isBlank() ? api.getCrossId() : api.getId();
    }

    private static Optional<String> extractToolName(Flow flow) {
        Optional<Selector> mcpSelector = flow.selectorByType(SelectorType.MCP);
        if (mcpSelector.isEmpty()) {
            return Optional.empty();
        }
        McpSelector selector = (McpSelector) mcpSelector.get();
        Set<String> methods = selector.getMethods();
        boolean handlesToolsCall = methods == null || methods.isEmpty() || methods.contains(TOOLS_CALL_METHOD);
        if (!handlesToolsCall) {
            return Optional.empty();
        }
        String name = flow.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }
}
