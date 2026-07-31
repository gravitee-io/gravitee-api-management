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
package io.gravitee.repository.mongodb.management.internal.aiworkspacecomponent;

import io.gravitee.repository.management.model.AiWorkspaceComponentType;
import io.gravitee.repository.mongodb.management.internal.model.AiWorkspaceComponentMongo;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public interface AiWorkspaceComponentMongoRepository extends MongoRepository<AiWorkspaceComponentMongo, String> {
    @Query(value = "{ 'apiProductId': ?0 }", sort = "{ '_id': 1 }")
    List<AiWorkspaceComponentMongo> findByApiProductId(String apiProductId);

    @Query(value = "{ 'apiProductId': ?0, 'componentType': ?1 }", sort = "{ '_id': 1 }")
    List<AiWorkspaceComponentMongo> findByApiProductIdAndComponentType(String apiProductId, AiWorkspaceComponentType componentType);

    @Query(value = "{ 'refId': ?0 }", sort = "{ '_id': 1 }")
    List<AiWorkspaceComponentMongo> findByRefId(String refId);

    @Query(value = "{ 'apiProductId': ?0 }", delete = true)
    void deleteByApiProductId(String apiProductId);
}
