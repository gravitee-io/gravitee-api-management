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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AuthzEntityIdExtractor {

    public static final AuthzEntityIdExtractor INSTANCE = new AuthzEntityIdExtractor();

    public static final String API_PREFIX = "api.";
    public static final String MCP_PREFIX = "mcp.";
    public static final String AGENT_PREFIX = "agent.";
    public static final String LLM_PREFIX = "llm.";
    static final String TOOLS_CALL_METHOD = "tools/call";

    public static boolean isAutoDerived(final String entityId) {
        if (entityId == null) {
            return false;
        }
        return (
            entityId.startsWith(API_PREFIX) ||
            entityId.startsWith(MCP_PREFIX) ||
            entityId.startsWith(AGENT_PREFIX) ||
            entityId.startsWith(LLM_PREFIX)
        );
    }

    public static String toResourceEngineUid(final String entityId) {
        return "Resource::\"" + entityId + "\"";
    }

    public Set<String> extract(ReactableApi<?> api) {
        if (api == null) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        String identifier = api.getId();
        ids.add(API_PREFIX + identifier);

        if (api.getDefinition() instanceof io.gravitee.definition.model.v4.Api v4 && v4.getType() == ApiType.MCP_PROXY) {
            List<Flow> flows = v4.getFlows();
            if (flows != null) {
                for (Flow flow : flows) {
                    extractToolName(flow).ifPresent(tool -> ids.add(MCP_PREFIX + identifier + "." + tool));
                }
            }
        }
        return ids;
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
