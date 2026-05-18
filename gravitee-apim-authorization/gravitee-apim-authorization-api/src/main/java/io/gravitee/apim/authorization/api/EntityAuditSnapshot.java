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
package io.gravitee.apim.authorization.api;

import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import java.time.Instant;
import java.util.List;

public record EntityAuditSnapshot(
    String id,
    String entityId,
    EntityKind kind,
    List<String> parents,
    String source,
    String environmentId,
    Instant createdAt,
    Instant updatedAt
) implements AuthzAuditSnapshot {
    public EntityAuditSnapshot {
        parents = parents == null ? List.of() : List.copyOf(parents);
    }

    public static EntityAuditSnapshot of(Entity entity) {
        if (entity == null) {
            return null;
        }
        return new EntityAuditSnapshot(
            entity.id(),
            entity.entityId(),
            entity.kind(),
            entity.parents(),
            entity.source(),
            entity.environmentId(),
            entity.createdAt(),
            entity.updatedAt()
        );
    }
}
