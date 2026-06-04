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
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
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
        // Derive a granular engine type from the _kind hint (principals) or the entityId prefix
        // (resources) when the caller didn't supply one; fall back to the kind-default umbrella.
        // A supplied entityType is kept verbatim. Done before validate so the validator sees the
        // canonical value.
        if (entityType == null || entityType.isBlank()) {
            entityType = deriveEntityType(kind, entityId, attributes);
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

    private static String deriveEntityType(AuthzEntityKind kind, String entityId, Map<String, Object> attributes) {
        Object kindHint = attributes == null ? null : attributes.get("_kind");
        String byKind = kindHint instanceof String s ? AuthzEntityIdConstants.engineTypeForHint(s) : null;
        if (byKind != null) {
            return byKind;
        }
        if (entityId != null) {
            int dot = entityId.indexOf('.');
            if (dot > 0) {
                String byPrefix = AuthzEntityIdConstants.engineTypeForHint(entityId.substring(0, dot));
                if (byPrefix != null) {
                    return byPrefix;
                }
            }
        }
        return kind != null ? kind.defaultEntityType() : null;
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
