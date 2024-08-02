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
package io.gravitee.repository.mongodb.management.internal.dictionary;

import io.gravitee.repository.mongodb.management.internal.model.DictionaryMongo;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface DictionaryMongoRepository extends MongoRepository<DictionaryMongo, String> {
    @Query("{ environmentId: {$in: ?0} }")
    List<DictionaryMongo> findByEnvironments(Set<String> environments);

    @Query(value = "{ environmentId: ?0 }", fields = "{ _id : 1 }", delete = true)
    List<DictionaryMongo> deleteByEnvironmentId(String environmentId);
}
