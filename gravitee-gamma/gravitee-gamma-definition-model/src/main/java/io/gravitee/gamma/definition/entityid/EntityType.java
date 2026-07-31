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

/**
 * A registered entity-id type (ADR-037): the first segment of an {@code entityId}, its identity family, and, for
 * entities that only exist under a parent, the parent's type. A scoped type produces hierarchical ids such as
 * {@code mcp-tool.<server>.<tool>}.
 *
 * @param token the type token (the first segment of the entityId, e.g. {@code mcp-server})
 * @param family how the slug is produced
 * @param parentToken the parent type token for scoped types, or {@code null} for top-level types
 */
public record EntityType(String token, IdentityFamily family, String parentToken) {
    public EntityType {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        if (!token.equals(EntitySlug.toSlug(token))) {
            throw new IllegalArgumentException("token must be a valid slug: " + token);
        }
        if (family == null) {
            throw new IllegalArgumentException("family must not be null");
        }
    }

    public boolean isScoped() {
        return parentToken != null;
    }

    /** Build a top-level id for this type, slugging the name. Fails for scoped types (use {@link #id(String, String)}). */
    public EntityId id(String name) {
        if (isScoped()) {
            throw new IllegalArgumentException("type '" + token + "' is scoped, a parent slug is required");
        }
        return EntityId.of(token, name);
    }

    /** Build a scoped id {@code <token>.<parentSlug>.<slug>}. Fails for top-level types (use {@link #id(String)}). */
    public EntityId id(String parentSlug, String name) {
        if (!isScoped()) {
            throw new IllegalArgumentException("type '" + token + "' is not scoped, no parent slug expected");
        }
        return EntityId.scoped(token, parentSlug, name);
    }

    public static EntityType nameSeeded(String token) {
        return new EntityType(token, IdentityFamily.NAME_SEEDED, null);
    }

    public static EntityType scoped(String token, String parentToken) {
        return new EntityType(token, IdentityFamily.NAME_SEEDED, parentToken);
    }

    public static EntityType recomputable(String token) {
        return new EntityType(token, IdentityFamily.RECOMPUTABLE, null);
    }
}
