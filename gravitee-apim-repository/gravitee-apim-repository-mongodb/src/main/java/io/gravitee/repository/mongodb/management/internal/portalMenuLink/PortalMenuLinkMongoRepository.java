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
package io.gravitee.repository.mongodb.management.internal.portalMenuLink;

import io.gravitee.repository.mongodb.management.internal.model.PortalMenuLinkMongo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public interface PortalMenuLinkMongoRepository extends MongoRepository<PortalMenuLinkMongo, String> {
    @Query(value = "{ 'environmentId': ?0 }", delete = true)
    void deleteByEnvironmentId(String environmentId);

    @Query(value = "{ 'environmentId': ?0 }", sort = "{ order : 1 }")
    List<PortalMenuLinkMongo> findByEnvironmentIdSortByOrder(String environmentId);

    @Query(value = "{ 'environmentId': ?0, 'visibility': ?1 }", sort = "{ order : 1 }")
    List<PortalMenuLinkMongo> findByEnvironmentIdAndVisibilitySortByOrder(String environmentId, String visibility);

    @Query("{ '_id': ?0, 'environmentId': ?1 }")
    Optional<PortalMenuLinkMongo> findByIdAndEnvironmentId(String portalMenuLinkId, String environmentId);
}
