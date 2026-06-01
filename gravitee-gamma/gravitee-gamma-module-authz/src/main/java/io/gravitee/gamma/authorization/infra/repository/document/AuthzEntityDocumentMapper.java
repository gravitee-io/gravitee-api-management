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
package io.gravitee.gamma.authorization.infra.repository.document;

import io.gravitee.gamma.authorization.domain.AuthzEntity;

public final class AuthzEntityDocumentMapper {

    private AuthzEntityDocumentMapper() {}

    public static AuthzEntityMongo toDocument(AuthzEntity entity) {
        return new AuthzEntityMongo(
            entity.id(),
            entity.entityId(),
            entity.kind(),
            entity.entityType(),
            entity.attributes(),
            entity.parents(),
            entity.source(),
            entity.environmentId(),
            entity.createdAt(),
            entity.updatedAt()
        );
    }

    public static AuthzEntity toDomain(AuthzEntityMongo doc) {
        // entityType may be null on rows persisted before the typed-entity-type rollout — the
        // AuthzEntity compact constructor normalises that null to the kind-default, so old
        // documents transparently surface as Principal::"<id>" / Resource::"<id>".
        return new AuthzEntity(
            doc.id(),
            doc.entityId(),
            doc.kind(),
            doc.entityType(),
            doc.attributes() != null ? doc.attributes() : java.util.Map.of(),
            doc.parents() != null ? doc.parents() : java.util.List.of(),
            doc.source(),
            doc.environmentId(),
            doc.createdAt(),
            doc.updatedAt()
        );
    }
}
