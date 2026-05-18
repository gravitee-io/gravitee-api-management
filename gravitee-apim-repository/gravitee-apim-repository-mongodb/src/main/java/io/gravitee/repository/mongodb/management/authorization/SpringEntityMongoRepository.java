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
package io.gravitee.repository.mongodb.management.authorization;

import io.gravitee.apim.authorization.domain.EntityKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringEntityMongoRepository extends MongoRepository<EntityDocument, String> {
    Optional<EntityDocument> findByEnvironmentIdAndId(String environmentId, String id);

    Optional<EntityDocument> findByEnvironmentIdAndEntityId(String environmentId, String entityId);

    List<EntityDocument> findAllByEnvironmentId(String environmentId);

    List<EntityDocument> findAllByEnvironmentIdAndKind(String environmentId, EntityKind kind);

    List<EntityDocument> findAllByEnvironmentIdAndEntityIdStartingWith(String environmentId, String entityIdPrefix);

    long deleteByEnvironmentIdAndId(String environmentId, String id);

    long deleteByEnvironmentIdAndEntityId(String environmentId, String entityId);
}
