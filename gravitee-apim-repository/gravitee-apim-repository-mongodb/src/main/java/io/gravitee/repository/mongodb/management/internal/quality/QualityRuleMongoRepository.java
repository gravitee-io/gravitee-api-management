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
package io.gravitee.repository.mongodb.management.internal.quality;

import io.gravitee.repository.mongodb.management.internal.model.QualityRuleMongo;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface QualityRuleMongoRepository extends MongoRepository<QualityRuleMongo, String> {
    @Query("{ 'referenceType': ?0, 'referenceId': ?1 }")
    List<QualityRuleMongo> findByReference(String referenceType, String referenceId);

    @Query(value = "{'referenceId': ?0,  'referenceType': ?1, }", delete = true)
    List<QualityRuleMongo> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType);
}
