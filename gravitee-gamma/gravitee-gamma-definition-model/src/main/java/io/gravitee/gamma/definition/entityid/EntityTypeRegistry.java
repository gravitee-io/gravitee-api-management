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
package io.gravitee.gamma.definition.entityid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single registry of entity-id types shared across Gamma modules (ADR-037). It records, per type, the token,
 * its identity family and (for scoped types) its parent, which is enough for any consumer to parse and resolve an id
 * from the registry alone. Construction rejects duplicate tokens and scoped types whose parent is not registered.
 *
 * <p>{@link #DEFAULT} is the canonical set. It supersedes the per-module token constants (e.g. the authz
 * {@code mcp.} / {@code api.} / {@code agent.} prefixes), which map onto the granular tokens here.
 */
public final class EntityTypeRegistry {

    public static final EntityTypeRegistry DEFAULT = new EntityTypeRegistry(
        List.of(
            // Catalog kinds.
            EntityType.nameSeeded("model"),
            EntityType.nameSeeded("knowledge"),
            EntityType.nameSeeded("skill"),
            EntityType.nameSeeded("mcp-server"),
            EntityType.nameSeeded("agent"),
            EntityType.nameSeeded("api-tool"),
            // Children of an MCP server: mcp-tool.<server>.<tool>.
            EntityType.scoped("mcp-tool", "mcp-server"),
            EntityType.scoped("mcp-prompt", "mcp-server"),
            EntityType.scoped("mcp-resource", "mcp-server"),
            // Deployed gateways.
            EntityType.nameSeeded("mcp-proxy"),
            EntityType.nameSeeded("llm-proxy"),
            EntityType.nameSeeded("a2a-proxy"),
            // Workload identity, rebuilt by the gateway PEP from the clientId.
            EntityType.recomputable("agent-identity")
        )
    );

    private final Map<String, EntityType> byToken;

    public EntityTypeRegistry(List<EntityType> types) {
        Map<String, EntityType> map = new LinkedHashMap<>();
        for (EntityType type : types) {
            if (map.putIfAbsent(type.token(), type) != null) {
                throw new IllegalArgumentException("duplicate entity-id token: " + type.token());
            }
        }
        for (EntityType type : map.values()) {
            if (type.isScoped() && !map.containsKey(type.parentToken())) {
                throw new IllegalArgumentException(
                    "scoped type '" + type.token() + "' references unknown parent '" + type.parentToken() + "'"
                );
            }
        }
        this.byToken = Map.copyOf(map);
    }

    public Optional<EntityType> find(String token) {
        return Optional.ofNullable(byToken.get(token));
    }

    /**
     * Parse {@code entityId} and resolve its registered type, so a consumer gets the family and parent from the
     * registry alone. Empty if the type token is not registered.
     *
     * @throws IllegalArgumentException if the string is not {@code <type>.<slug>}
     */
    public Optional<EntityType> resolve(String entityId) {
        return find(EntityId.parse(entityId).type());
    }

    public boolean contains(String token) {
        return byToken.containsKey(token);
    }

    public List<EntityType> all() {
        return List.copyOf(byToken.values());
    }
}
