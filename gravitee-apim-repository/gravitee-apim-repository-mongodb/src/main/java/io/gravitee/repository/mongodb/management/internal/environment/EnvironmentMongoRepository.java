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
package io.gravitee.repository.mongodb.management.internal.environment;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.mongodb.management.internal.model.EnvironmentMongo;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface EnvironmentMongoRepository extends MongoRepository<EnvironmentMongo, String> {
    @Query("{ organizationId: ?0}")
    Set<EnvironmentMongo> findByOrganizationId(String organizationId);

    @Query("{ hrids: {$in: ?0} }")
    Set<EnvironmentMongo> findByHrids(Set<String> hrids);

    @Query("{ organizationId: {$in: ?0} }")
    Set<EnvironmentMongo> findByOrganizations(Set<String> organizations);

    @Query("{ organizationId: {$in: ?0}, hrids: {$in: ?1} }")
    Set<EnvironmentMongo> findByOrganizationsAndHrids(Set<String> organizations, Set<String> hrids);

    @Query("{ cockpitId: ?0 }")
    Optional<EnvironmentMongo> findByCockpitId(String cockpitId);

    @Query(value = "{ id: {$in: ?0} }", fields = "{ organizationId: 1}")
    Set<EnvironmentMongo> findOrganizationIdsByEnvironments(Set<String> ids) throws TechnicalException;
}
