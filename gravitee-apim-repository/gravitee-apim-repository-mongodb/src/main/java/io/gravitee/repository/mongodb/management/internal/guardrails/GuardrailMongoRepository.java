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
package io.gravitee.repository.mongodb.management.internal.guardrails;

import io.gravitee.repository.mongodb.management.internal.model.GuardrailMongo;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public interface GuardrailMongoRepository extends MongoRepository<GuardrailMongo, String>, GuardrailMongoRepositoryCustom {
    List<GuardrailMongo> findByEnvironmentIdAndIdIn(String environmentId, Set<String> guardrailIds);

    @Query(value = "{ 'environmentId': ?0 }", fields = "{ _id : 1 }", delete = true)
    List<GuardrailMongo> deleteByEnvironmentId(String environmentId);

    /**
     * A list rather than an Optional on purpose: the unique index arrives with this change, so a store
     * written before it can still hold two Guardrails of one name and Spring Data would throw on the
     * second instead of letting the caller refuse the name.
     */
    List<GuardrailMongo> findByEnvironmentIdAndNameIgnoreCase(String environmentId, String name);
}
