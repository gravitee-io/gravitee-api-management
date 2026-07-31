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
package io.gravitee.repository.mongodb.management.internal;

import io.gravitee.repository.mongodb.management.internal.model.GammaDashboardMongo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public interface GammaDashboardMongoRepository extends MongoRepository<GammaDashboardMongo, String> {
    /**
     * Ordered explicitly: Mongo natural order is not stable — a document can move when an update grows it — so an
     * unordered list would let a user reshuffle their own dashboards just by editing one.
     */
    List<GammaDashboardMongo> findByEnvironmentIdOrderByCreatedAtAscIdAsc(String environmentId);

    Optional<GammaDashboardMongo> findByIdAndEnvironmentId(String id, String environmentId);

    void deleteByEnvironmentId(String environmentId);
}
