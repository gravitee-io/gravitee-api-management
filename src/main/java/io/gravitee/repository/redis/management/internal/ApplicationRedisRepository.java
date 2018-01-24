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

import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.redis.management.model.RedisApplication;

import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApplicationRedisRepository {

    RedisApplication find(String application);

    Set<RedisApplication> find(List<String> applications);

    Set<RedisApplication> findAll(ApplicationStatus ... statuses);

    Set<RedisApplication> findByGroups(List<String> groupIds, ApplicationStatus ... statuses);

    Set<RedisApplication> findByName(String partialName);

    RedisApplication findByClientId(String clientId);

    RedisApplication saveOrUpdate(RedisApplication application);

    void delete(String application);

}
