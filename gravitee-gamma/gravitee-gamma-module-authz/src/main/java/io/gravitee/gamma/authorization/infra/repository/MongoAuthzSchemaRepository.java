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
package io.gravitee.gamma.authorization.infra.repository;

import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import io.gravitee.gamma.authorization.infra.repository.document.AuthzSchemaMongo;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@RequiredArgsConstructor
public class MongoAuthzSchemaRepository implements AuthzSchemaRepository {

    private final MongoOperations mongo;

    @Override
    public Optional<String> find(String environmentId) {
        var query = new Query(Criteria.where("_id").is(environmentId));
        return Optional.ofNullable(mongo.findOne(query, AuthzSchemaMongo.class)).map(AuthzSchemaMongo::schemaText);
    }

    @Override
    public void save(String environmentId, String schemaText, Instant updatedAt) {
        mongo.save(new AuthzSchemaMongo(environmentId, schemaText, updatedAt));
    }

    @Override
    public boolean delete(String environmentId) {
        var query = new Query(Criteria.where("_id").is(environmentId));
        return mongo.remove(query, AuthzSchemaMongo.class).getDeletedCount() > 0;
    }
}
