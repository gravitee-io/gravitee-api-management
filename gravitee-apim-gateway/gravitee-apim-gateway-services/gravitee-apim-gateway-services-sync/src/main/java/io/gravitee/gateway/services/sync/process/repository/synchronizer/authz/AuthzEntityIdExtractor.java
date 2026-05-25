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

import static io.gravitee.gamma.definition.authz.AuthzEntityIdConstants.API_PREFIX;
import static io.gravitee.gamma.definition.authz.AuthzEntityIdConstants.MCP_PREFIX;
import static io.gravitee.gamma.definition.authz.AuthzEntityIdConstants.TOOLS_CALL_METHOD;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AuthzEntityIdExtractor {

    public static final AuthzEntityIdExtractor INSTANCE = new AuthzEntityIdExtractor();

    public record EntityFragment(String uid, Map<String, Object> attributes, List<String> parents) {}

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

    public Set<EntityFragment> extractWithAttributes(ReactableApi<?> api) {
        if (api == null) {
            return Set.of();
        }
        String identifier = api.getId();
        String apiUid = API_PREFIX + identifier;

        Map<String, Object> apiAttrs = new LinkedHashMap<>();
        apiAttrs.put("apiId", identifier);
        if (api.getName() != null) {
            apiAttrs.put("apiName", api.getName());
        }
        if (api.getApiVersion() != null) {
            apiAttrs.put("apiVersion", api.getApiVersion());
        }

        Set<EntityFragment> fragments = new LinkedHashSet<>();
        fragments.add(new EntityFragment(apiUid, apiAttrs, List.of()));

        if (api.getDefinition() instanceof io.gravitee.definition.model.v4.Api v4 && v4.getType() == ApiType.MCP_PROXY) {
            List<Flow> flows = v4.getFlows();
            if (flows != null) {
                for (Flow flow : flows) {
                    extractToolName(flow).ifPresent(tool ->
                        fragments.add(new EntityFragment(MCP_PREFIX + identifier + "." + tool, Map.of(), List.of(apiUid)))
                    );
                }
            }
        }
        return fragments;
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
