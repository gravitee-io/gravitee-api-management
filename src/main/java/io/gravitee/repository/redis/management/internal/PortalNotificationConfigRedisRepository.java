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
package io.gravitee.repository.redis.management.internal;

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.redis.management.model.RedisPortalNotificationConfig;

import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public interface PortalNotificationConfigRedisRepository {

    RedisPortalNotificationConfig find(String user, String referenceType, String referenceId);
    RedisPortalNotificationConfig create(RedisPortalNotificationConfig cfg);
    RedisPortalNotificationConfig update(RedisPortalNotificationConfig cfg);
    void delete(String user, String referenceType, String referenceId);
    Set<RedisPortalNotificationConfig> findByReference(NotificationReferenceType type, String referenceId);
}
