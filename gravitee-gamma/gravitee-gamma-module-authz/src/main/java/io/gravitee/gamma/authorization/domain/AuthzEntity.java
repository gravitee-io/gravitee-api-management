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
package io.gravitee.gamma.authorization.domain;

import io.gravitee.gamma.authorization.api.AuthzValidators;
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AuthzEntity(
    @NotBlank String id,
    @NotBlank
    @Size(max = AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH)
    @Pattern(regexp = AuthzEntityIdConstants.FORMAT_REGEX)
    String entityId,
    @NotNull AuthzEntityKind kind,
    @NotBlank String entityType,
    @NotNull Map<String, Object> attributes,
    @NotNull List<String> parents,
    @NotBlank String source,
    @NotBlank String environmentId,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt
) {
    public AuthzEntity {
        // Backwards-compat: pre-typed-entityType rows persisted with a null entityType column
        // are normalised to the kind-default on read so older snapshots keep flowing through.
        if ((entityType == null || entityType.isBlank()) && kind != null) {
            entityType = kind.defaultEntityType();
        }
        AuthzValidators.validateCtor(
            AuthzEntity.class,
            id,
            entityId,
            kind,
            entityType,
            attributes,
            parents,
            source,
            environmentId,
            createdAt,
            updatedAt
        );
        attributes = Map.copyOf(attributes);
        parents = List.copyOf(parents);
    }

    /**
     * Legacy 9-arg constructor for callers that pre-date the typed entityType rollout.
     * Delegates with null {@code entityType}, normalised to the kind-default by the compact
     * constructor.
     */
    public AuthzEntity(
        String id,
        String entityId,
        AuthzEntityKind kind,
        Map<String, Object> attributes,
        List<String> parents,
        String source,
        String environmentId,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, entityId, kind, null, attributes, parents, source, environmentId, createdAt, updatedAt);
    }
}
