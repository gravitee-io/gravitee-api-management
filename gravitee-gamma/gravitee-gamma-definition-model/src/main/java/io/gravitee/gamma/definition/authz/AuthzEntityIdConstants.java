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
package io.gravitee.gamma.definition.authz;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AuthzEntityIdConstants {

    public static final int MAX_ENTITY_ID_LENGTH = 255;

    /**
     * GAPL entity-id grammar: dot-separated lowercase segments. Colons are allowed inside
     * segments because GAPL accepts them in unquoted ids (e.g. {@code repo:backend},
     * {@code db:production}) and several authz-engine playground scenarios use that form.
     */
    public static final String FORMAT_REGEX = "^[a-z0-9_:-]+(?:\\.[a-z0-9_:-]+)*$";

    public static final String ENGINE_TYPE_PRINCIPAL = "Principal";
    public static final String ENGINE_TYPE_RESOURCE = "Resource";

    public static final String API_PREFIX = "api.";
    public static final String MCP_PREFIX = "mcp.";
    public static final String AGENT_PREFIX = "agent.";

    public static final String TOOLS_CALL_METHOD = "tools/call";

    private static final Set<String> AUTO_DERIVED_PREFIXES = Set.of(API_PREFIX, MCP_PREFIX, AGENT_PREFIX);

    public static boolean isAutoDerived(String entityId) {
        if (entityId == null) {
            return false;
        }
        for (String prefix : AUTO_DERIVED_PREFIXES) {
            if (entityId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // Transitional bridge: maps a _kind attribute value (principals) or an entityId prefix segment
    // (resources) to the canonical engine type name. Mirrors the FE entity-kind-registry.ts. The
    // schema is the source of truth for which types are valid; this only types legacy/sync data
    // that carries _kind/prefix rather than an explicit entityType.
    private static final Map<String, String> ENGINE_TYPE_BY_HINT = new HashMap<>();

    static {
        registerEngineType("User", "user");
        registerEngineType("Group", "group");
        registerEngineType("Role", "role");
        registerEngineType("ServiceAccount", "serviceaccount", "service-account", "service_account");
        registerEngineType("AgentIdentity", "agent-identity", "agentidentity");
        registerEngineType("MCPServer", "mcp", "mcpserver");
        registerEngineType("MCPTool", "mcptool");
        registerEngineType("Model", "model", "llm", "llmmodel", "llmroute");
        registerEngineType("Agent", "agent", "a2a", "a2aagent");
        registerEngineType("API", "api");
        registerEngineType("Event", "event");
        registerEngineType("Resource", "resource");
        registerEngineType("Action", "action");
    }

    private static void registerEngineType(String engineType, String... hints) {
        for (String hint : hints) {
            ENGINE_TYPE_BY_HINT.put(hint.toLowerCase(Locale.ROOT), engineType);
        }
    }

    /** A {@code _kind} value or entityId prefix segment → canonical engine type, or {@code null} when unknown. */
    public static String engineTypeForHint(String hint) {
        if (hint == null || hint.isBlank()) {
            return null;
        }
        return ENGINE_TYPE_BY_HINT.get(hint.toLowerCase(Locale.ROOT));
    }

    /**
     * Canonical engine UID wire form: {@code Type::"id"} (the quoted form the engine's parser expects).
     * No escaping is applied: entityIds are constrained by {@link #FORMAT_REGEX} (lowercase, digits,
     * {@code _ : - .}) and types are canonical names, so neither can contain a quote/backslash.
     */
    public static String toEngineUid(String entityType, String entityId) {
        return entityType + "::\"" + entityId + "\"";
    }

    /**
     * Normalize a reference UID to the canonical quoted form {@code Type::"id"} so the engine parser
     * accepts it directly. Re-quotes the unquoted legacy {@code Type::id}; leaves an already-quoted
     * UID, a typeless bare id (no {@code ::}), and null untouched. The type/id split is the last
     * {@code ::}, mirroring the engine's own parse (a namespaced type keeps its inner {@code ::}).
     */
    public static String normalizeParentUid(String uid) {
        if (uid == null || uid.contains("::\"") || !uid.contains("::")) {
            return uid;
        }
        int sep = uid.lastIndexOf("::");
        String type = uid.substring(0, sep);
        String id = uid.substring(sep + 2);
        if (type.isBlank() || id.isBlank()) {
            // Not a normalizable Type::id (e.g. "Type::" or "::id") — leave it for the parser to reject.
            return uid;
        }
        return toEngineUid(type, id);
    }

    private AuthzEntityIdConstants() {}
}
