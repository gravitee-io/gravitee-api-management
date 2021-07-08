/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.internal.identityprovideractivation;

import io.gravitee.repository.mongodb.management.internal.model.IdentityProviderActivationMongo;
import io.gravitee.repository.mongodb.management.internal.model.IdentityProviderActivationPkMongo;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface IdentityProviderActivationMongoRepository
    extends MongoRepository<IdentityProviderActivationMongo, IdentityProviderActivationPkMongo> {
    @Query("{ '_id.identityProviderId': ?0 }")
    List<IdentityProviderActivationMongo> findAllByIdentityProviderId(String identityProviderId);

    @Query("{ '_id.referenceId': ?0, '_id.referenceType': ?1 }")
    List<IdentityProviderActivationMongo> findAllByReferenceIdAndReferenceType(String referenceId, String referenceType);

    @Query(value = "{ '_id.identityProviderId': ?0 }", delete = true)
    void deleteByIdentityProviderId(String identityProviderId);

    @Query(value = "{ '_id.referenceId': ?0, '_id.referenceType': ?1 }", delete = true)
    void deleteByReferenceIdAndReferenceType(String referenceId, String referenceType);
}
