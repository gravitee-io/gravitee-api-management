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
package io.gravitee.repository.mongodb.management.internal.notification;

import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationConfigMongo;
import io.gravitee.repository.mongodb.management.internal.model.PortalNotificationConfigPkMongo;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface PortalNotificationConfigMongoRepository
    extends MongoRepository<PortalNotificationConfigMongo, PortalNotificationConfigPkMongo> {
    @Query("{ 'hooks': ?0, 'id.referenceType': ?1, 'id.referenceId': ?2 }")
    Set<PortalNotificationConfigMongo> findByReferenceAndHook(String hook, String referenceType, String referenceId);

    @Query(value = "{ 'id.user': ?0 }", delete = true)
    void deleteByUser(String user);

    @Query(value = "{ 'id.referenceType': ?0, 'id.referenceId': ?1 }", delete = true)
    void deleteByReference(String referenceType, String referenceId);
}
