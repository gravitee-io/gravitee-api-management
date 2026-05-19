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

import io.gravitee.gamma.authorization.api.EntityIdConstants;
import io.gravitee.gamma.authorization.api.Validators;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Entity(
    @NotBlank String id,
    @NotBlank @Size(max = EntityIdConstants.MAX_ENTITY_ID_LENGTH) @Pattern(regexp = EntityIdConstants.FORMAT_REGEX) String entityId,
    @NotNull EntityKind kind,
    @NotNull Map<String, Object> attributes,
    @NotNull List<String> parents,
    @NotBlank String source,
    @NotBlank String environmentId,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt
) {
    public Entity {
        Validators.validateCtor(Entity.class, id, entityId, kind, attributes, parents, source, environmentId, createdAt, updatedAt);
        attributes = Map.copyOf(attributes);
        parents = List.copyOf(parents);
    }
}
