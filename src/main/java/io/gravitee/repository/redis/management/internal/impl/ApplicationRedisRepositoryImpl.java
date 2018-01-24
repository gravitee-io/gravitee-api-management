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

import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.redis.management.internal.ApplicationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApplication;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
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
                .filter(Objects::nonNull)
                .map(o -> this.convert(o, RedisApplication.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RedisApplication> findAll(ApplicationStatus... statuses) {
        Map<Object, Object> applications = redisTemplate.opsForHash().entries(REDIS_KEY);

        Set<RedisApplication> applicationSet = applications.values().stream().
                map(object -> convert(object, RedisApplication.class)).
                collect(Collectors.toSet());
        if (statuses != null && statuses.length > 0) {
            List<ApplicationStatus> applicationStatuses = Arrays.asList(statuses);
            return applicationSet.stream().
                    filter(app ->
                            applicationStatuses.contains(ApplicationStatus.valueOf(app.getStatus()))).
                    collect(Collectors.toSet());
        } else {
            return applicationSet;
        }
    }

    @Override
    public RedisApplication saveOrUpdate(RedisApplication application) {
        //remove old "searchByName" set if necessary
        RedisApplication oldApplication = find(application.getId());
        if (oldApplication != null && !oldApplication.getName().equals(application.getName())) {
            redisTemplate.opsForSet().remove(REDIS_KEY + ":search-by:name:" + oldApplication.getName().toUpperCase(), oldApplication.getId());
        }

        redisTemplate.opsForHash().put(REDIS_KEY, application.getId(), application);
        if(application.getGroups() != null) {
            for (String groupId : application.getGroups()) {
                redisTemplate.opsForSet().add(REDIS_KEY + ":group:" + groupId, application.getId());
            }
        }

        if (application.getClientId() != null) {
            redisTemplate.opsForSet().add(REDIS_KEY + ":client_id:" + application.getClientId(), application.getId());
        }

        redisTemplate.opsForSet().add(REDIS_KEY + ":search-by:name:" + application.getName().toUpperCase(), application.getId());

        return application;
    }


    @Override
    public Set<RedisApplication> findByGroups(List<String> groups, ApplicationStatus... statuses) {
        Set<Object> keys = new HashSet<>();
        groups.forEach(group->keys.addAll(redisTemplate.opsForSet().members(REDIS_KEY + ":group:" + group)));
        List<Object> apiObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        Set<RedisApplication> applications = apiObjects.stream().
                map(event -> convert(event, RedisApplication.class)).
                collect(Collectors.toSet());

        if (statuses != null && statuses.length > 0) {
            List<ApplicationStatus> applicationStatuses = Arrays.asList(statuses);
            return applications.stream().
                    filter(app ->
                            applicationStatuses.contains(ApplicationStatus.valueOf(app.getStatus()))).
                    collect(Collectors.toSet());
        } else {
            return applications;
        }
    }

    @Override
    public Set<RedisApplication> findByName(final String partialName) {

        List<String> matchedNames = redisTemplate.execute((RedisCallback<List<String>>) redisConnection -> {
            ScanOptions options = ScanOptions.scanOptions().match(REDIS_KEY + ":search-by:name:*" + partialName.toUpperCase() + "*").build();
            Cursor<byte[]> cursor = redisConnection.scan(options);
            List<String> result = new ArrayList<>();
            if (cursor != null) {
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next()));
                }
            }
            return result;
        });

        if (matchedNames == null || matchedNames.isEmpty() ) {
            return Collections.emptySet();
        }
        Set<Object> applicationIds = new HashSet<>();
        matchedNames.forEach(matchedName -> applicationIds.addAll(redisTemplate.opsForSet().members(matchedName)));

        return find(applicationIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

    @Override
    public RedisApplication findByClientId(String clientId) {
        Set<Object> members = redisTemplate.opsForSet().members(REDIS_KEY + ":client_id:" + clientId);
        if (members.isEmpty()) {
            return null;
        }

        return find((String) members.iterator().next());
    }

    @Override
    public void delete(String applicationId) {
        RedisApplication redisApplication = find(applicationId);
        redisTemplate.opsForHash().delete(REDIS_KEY, applicationId);
        if (redisApplication.getGroups() != null) {
            for (String groupId : redisApplication.getGroups()) {
                redisTemplate.opsForSet().remove(REDIS_KEY + ":group:" + groupId, applicationId);
            }
        }
        redisTemplate.opsForSet().remove(REDIS_KEY + ":search-by:name:" + redisApplication.getName().toUpperCase(), applicationId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":client_id:" + redisApplication.getClientId(), applicationId);
    }

}
