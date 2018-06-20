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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.redis.management.internal.ApiRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApi;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiRedisRepositoryImpl extends AbstractRedisRepository implements ApiRedisRepository {

    private final static String REDIS_KEY = "api";

    @Override
    public RedisApi find(String apiId) {
        Object api = redisTemplate.opsForHash().get(REDIS_KEY, apiId);
        if (api == null) {
            return null;
        }

        return convert(api, RedisApi.class);
    }

    @Override
    public Set<RedisApi> find(List<String> apis) {
        return redisTemplate.opsForHash().multiGet(REDIS_KEY, Collections.unmodifiableCollection(apis)).stream()
                .filter(Objects::nonNull)
                .map(o -> this.convert(o, RedisApi.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Page<RedisApi> search(ApiCriteria criteria, Pageable pageable, ApiFieldExclusionFilter apiFieldExclusionFilter) {
        final Set<String> filterKeys = new HashSet<>();
        final String tempDestination;
        if (criteria == null) {
            tempDestination = "tmp-search";
        } else {
            tempDestination = "tmp-" + Math.abs(criteria.hashCode());
            if (criteria.getIds() != null && !criteria.getIds().isEmpty()) {
                criteria.getIds().forEach(id -> filterKeys.add(REDIS_KEY + ":id:" + id));
                redisTemplate.opsForZSet().unionAndStore(null, filterKeys, tempDestination);
                filterKeys.clear();
                filterKeys.add(tempDestination);
            }
            if (criteria.getGroups() != null && !criteria.getGroups().isEmpty()) {
                criteria.getGroups().forEach(group -> filterKeys.add(REDIS_KEY + ":group:" + group));
                redisTemplate.opsForZSet().unionAndStore(null, filterKeys, tempDestination);
                filterKeys.clear();
                filterKeys.add(tempDestination);
            }
            addCriteria("visibility", criteria.getVisibility(), filterKeys, tempDestination);
            addCriteria("view", criteria.getView(), filterKeys, tempDestination);
            addCriteria("label", criteria.getLabel(), filterKeys, tempDestination);
            addCriteria("name", criteria.getName(), filterKeys, tempDestination);
            addCriteria("state", criteria.getState(), filterKeys, tempDestination);
            addCriteria("version", criteria.getVersion(), filterKeys, tempDestination);
        }

        filterKeys.add(REDIS_KEY + ":name");

        redisTemplate.opsForZSet().intersectAndStore(null, filterKeys, tempDestination);

        Set<Object> keys = redisTemplate.opsForZSet().rangeByScore(tempDestination,
                0, Long.MAX_VALUE);

        long total = keys.size();
        if (pageable != null) {
            keys = redisTemplate.opsForZSet().rangeByScore(tempDestination,
                    0, Long.MAX_VALUE, pageable.from(), pageable.pageSize());
        }
        redisTemplate.opsForZSet().removeRange(tempDestination, 0, -1);
        List<Object> apiObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return new Page<>(
                apiObjects.stream()
                        .map(api -> convert(api, RedisApi.class))
                        .peek(api -> {
                            if (apiFieldExclusionFilter != null) {
                                if (apiFieldExclusionFilter.isDefinition()) {
                                    api.setDefinition(null);
                                }
                                if (apiFieldExclusionFilter.isPicture()) {
                                    api.setPicture(null);
                                }
                            }
                        })
                        .collect(toList()),
                (pageable != null) ? pageable.pageNumber() : 0,
                (pageable != null) ? pageable.pageSize() : 0,
                total);
    }

    private void addCriteria(final String keySuffix, final Object value, Set<String> filterKeys, String tempDestination) {
        if (value != null) {
            filterKeys.add(REDIS_KEY + ":" + keySuffix + ":" + value);
            redisTemplate.opsForZSet().unionAndStore(null, filterKeys, tempDestination);
        }
    }

    @Override
    public RedisApi saveOrUpdate(RedisApi api) {
        RedisApi oldApi = find(api.getId());
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            redisTemplate.opsForHash().put(REDIS_KEY, api.getId(), api);

            if (oldApi != null) {
                redisTemplate.opsForSet().remove(REDIS_KEY + ":visibility:" + oldApi.getVisibility(), api.getId());
                redisTemplate.opsForSet().remove(REDIS_KEY + ":id:" + oldApi.getId(), api.getId());
                redisTemplate.opsForSet().remove(REDIS_KEY + ":state:" + oldApi.getLifecycleState(), api.getId());
                redisTemplate.opsForSet().remove(REDIS_KEY + ":name:" + oldApi.getName(), api.getId());
                redisTemplate.opsForSet().remove(REDIS_KEY + ":version:" + oldApi.getVersion(), api.getId());
                if(oldApi.getGroups() != null) {
                    for (String groupId : oldApi.getGroups()) {
                        redisTemplate.opsForSet().remove(REDIS_KEY + ":group:" + groupId, api.getId());
                    }
                }
                if(oldApi.getLabels() != null) {
                    for (String label : oldApi.getLabels()) {
                        redisTemplate.opsForSet().remove(REDIS_KEY + ":label:" + label, api.getId());
                    }
                }
                if(oldApi.getViews() != null) {
                    for (String view : oldApi.getViews()) {
                        redisTemplate.opsForSet().remove(REDIS_KEY + ":view:" + view, api.getId());
                    }
                }
                redisTemplate.opsForSet().remove(REDIS_KEY + ":name", oldApi.getName());
            }

            redisTemplate.opsForSet().add(REDIS_KEY + ":visibility:" + api.getVisibility(), api.getId());
            redisTemplate.opsForSet().add(REDIS_KEY + ":id:" + api.getId(), api.getId());
            redisTemplate.opsForSet().add(REDIS_KEY + ":state:" + api.getLifecycleState(), api.getId());
            redisTemplate.opsForSet().add(REDIS_KEY + ":version:" + api.getVersion(), api.getId());
            redisTemplate.opsForSet().add(REDIS_KEY + ":name:" + api.getName(), api.getId());
            if(api.getGroups() != null) {
                for (String groupId : api.getGroups()) {
                    redisTemplate.opsForSet().add(REDIS_KEY + ":group:" + groupId, api.getId());
                }
            }
            if(api.getLabels() != null) {
                for (String label : api.getLabels()) {
                    redisTemplate.opsForSet().add(REDIS_KEY + ":label:" + label, api.getId());
                }
            }
            if(api.getViews() != null) {
                for (String view : api.getViews()) {
                    redisTemplate.opsForSet().add(REDIS_KEY + ":view:" + view, api.getId());
                }
            }
            redisTemplate.opsForSet().add(REDIS_KEY + ":name", api.getName());
            return null;
        });
        return api;
    }

    @Override
    public void delete(String apiId) {
        RedisApi api = find(apiId);
        redisTemplate.opsForHash().delete(REDIS_KEY, apiId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":id:" + apiId, apiId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":state:" + api.getLifecycleState(), apiId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":version:" + api.getVersion(), apiId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":name:" + api.getName(), apiId);
        if (api.getVisibility() != null) {
            redisTemplate.opsForSet().remove(REDIS_KEY + ":visibility:" + api.getVisibility(), apiId);
        }
        if (api.getGroups() != null) {
            for (String groupId : api.getGroups()) {
                redisTemplate.opsForSet().remove(REDIS_KEY + ":group:" + groupId, apiId);
            }
        }
        if (api.getLabels() != null) {
            for (String label : api.getLabels()) {
                redisTemplate.opsForSet().remove(REDIS_KEY + ":label:" + label, apiId);
            }
        }
        if (api.getViews() != null) {
            for (String view : api.getViews()) {
                redisTemplate.opsForSet().remove(REDIS_KEY + ":view:" + view, apiId);
            }
        }
        redisTemplate.opsForSet().remove(REDIS_KEY + ":name", api.getName());
    }

}
