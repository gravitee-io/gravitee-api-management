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
package io.gravitee.repository.redis.management.internal.impl;

import io.gravitee.repository.redis.management.internal.ApplicationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApplication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApplicationRedisRepositoryImpl extends AbstractRedisRepository implements ApplicationRedisRepository {

    private final static String REDIS_KEY = "application";

    @Override
    public RedisApplication find(String applicationId) {
        Object api = redisTemplate.opsForHash().get(REDIS_KEY, applicationId);
        if (api == null) {
            return null;
        }

        return convert(api, RedisApplication.class);
    }

    @Override
    public Set<RedisApplication> find(List<String> applicationIds) {
        return redisTemplate.opsForHash().multiGet(REDIS_KEY, Collections.unmodifiableCollection(applicationIds)).stream()
                .map(o -> this.convert(o, RedisApplication.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApplication> findAll() {
        Map<Object, Object> applications = redisTemplate.opsForHash().entries(REDIS_KEY);

        return applications.values()
                .stream()
                .map(object -> convert(object, RedisApplication.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisApplication saveOrUpdate(RedisApplication application) {
        redisTemplate.opsForHash().put(REDIS_KEY, application.getId(), application);
        if(application.getGroup() != null) {
            redisTemplate.opsForSet().add(REDIS_KEY + ":group:" + application.getGroup(), application.getId());
        }
        return application;
    }


    @Override
    public Set<RedisApplication> findByGroups(List<String> groups) {
        Set<Object> keys = new HashSet<>();
        groups.forEach(group->keys.addAll(redisTemplate.opsForSet().members(REDIS_KEY + ":group:" + group)));
        List<Object> apiObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return apiObjects.stream()
                .map(event -> convert(event, RedisApplication.class))
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(String applicationId) {
        RedisApplication redisApplication = find(applicationId);
        redisTemplate.opsForHash().delete(REDIS_KEY, applicationId);
        if (redisApplication.getGroup() != null) {
            redisTemplate.opsForSet().remove(REDIS_KEY + ":group:" + redisApplication.getGroup(), applicationId);
        }
    }

}
