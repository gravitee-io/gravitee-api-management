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
package io.gravitee.gamma.authorization.rest.dto;

import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record AuthzEntityRequest(
    @NotBlank
    @Size(max = AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH)
    @Pattern(regexp = AuthzEntityIdConstants.FORMAT_REGEX)
    String entityId,
    @NotNull AuthzEntityKind kind,
    /**
     * Engine type name (e.g. {@code "User"}, {@code "Doc"}). Optional — when omitted, the
     * server falls back to {@link AuthzEntityKind#defaultEntityType()} ({@code "Principal"} /
     * {@code "Resource"}) so legacy clients remain unaffected. Drives the engine UID emitted
     * to the PDP ({@code <entityType>::"<entityId>"}) and the type a GAPL policy can
     * match (e.g. {@code principal == User::"alice"}).
     */
    String entityType,
    Map<String, Object> attributes,
    List<String> parents,
    String source
) {
    /**
     * Legacy 5-arg constructor for callers that pre-date the typed entityType rollout.
     * {@code entityType} defaults to null and is later normalised to the kind-default by the
     * service layer.
     */
    public AuthzEntityRequest(
        String entityId,
        AuthzEntityKind kind,
        Map<String, Object> attributes,
        List<String> parents,
        String source
    ) {
        this(entityId, kind, null, attributes, parents, source);
    }
}
