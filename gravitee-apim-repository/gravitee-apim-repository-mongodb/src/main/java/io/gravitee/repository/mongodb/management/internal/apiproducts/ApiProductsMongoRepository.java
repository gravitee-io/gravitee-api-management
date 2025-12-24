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
package io.gravitee.repository.mongodb.management.internal.apiproducts;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.mongodb.management.internal.model.ApiProductMongo;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Internal MongoDB repository for API Product operations.
 *
 * @author GraviteeSource Team
 */
@Repository
public interface ApiProductsMongoRepository extends MongoRepository<ApiProductMongo, String>, ApiProductsMongoRepositoryCustom {
    Optional<ApiProductMongo> findByEnvironmentIdAndName(String environmentId, String name);
    Set<ApiProduct> findByEnvironmentId(String environmentId) throws TechnicalException;
}
