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
package io.gravitee.gamma.authorization.service;

import io.gravitee.gamma.authorization.api.AuthzValidators;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record CreateOrReplaceAuthzEntityCommand(
    @NotBlank String environmentId,
    @NotBlank String entityId,
    @NotNull AuthzEntityKind kind,
    String entityType,
    Map<String, Object> attributes,
    List<String> parents,
    @NotBlank String source
) {
    public CreateOrReplaceAuthzEntityCommand {
        // Backwards-compatible: legacy callers (no entityType) fall back to the kind-default.
        // Done before validate so the value seen by hibernate-validator matches the canonical
        // constructor parameter list.
        if (entityType == null || entityType.isBlank()) {
            entityType = kind != null ? kind.defaultEntityType() : null;
        }
        AuthzValidators.validateCtor(
            CreateOrReplaceAuthzEntityCommand.class,
            environmentId,
            entityId,
            kind,
            entityType,
            attributes,
            parents,
            source
        );
        attributes = Map.copyOf(attributes);
        parents = List.copyOf(parents);
    }

    /**
     * Legacy 6-arg constructor for callers that pre-date the typed entityType rollout.
     * Delegates to the canonical constructor with a null {@code entityType}, which is then
     * normalised to the kind-default by the compact constructor.
     */
    public CreateOrReplaceAuthzEntityCommand(
        String environmentId,
        String entityId,
        AuthzEntityKind kind,
        Map<String, Object> attributes,
        List<String> parents,
        String source
    ) {
        this(environmentId, entityId, kind, null, attributes, parents, source);
    }
}
