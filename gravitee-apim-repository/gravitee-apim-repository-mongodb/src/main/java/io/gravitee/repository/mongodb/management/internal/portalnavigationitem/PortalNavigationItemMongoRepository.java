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
package io.gravitee.repository.mongodb.management.internal.portalnavigationitem;

import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.mongodb.management.internal.model.PortalNavigationItemMongo;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PortalNavigationItemMongoRepository extends MongoRepository<PortalNavigationItemMongo, String> {
    Set<PortalNavigationItemMongo> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId);

    Set<PortalNavigationItemMongo> findAllByAreaAndOrganizationIdAndEnvironmentId(
        PortalNavigationItem.Area area,
        String organizationId,
        String environmentId
    );

    Optional<PortalNavigationItemMongo> findById(String id);

    @Query(value = "{ 'organizationId' : ?0 }", delete = true)
    void deleteByOrganizationId(String organizationId);

    @Query(value = "{ 'environmentId' : ?0 }", delete = true)
    void deleteByEnvironmentId(String environmentId);
}
