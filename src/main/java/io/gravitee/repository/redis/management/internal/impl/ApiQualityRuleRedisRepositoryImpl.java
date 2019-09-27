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

import io.gravitee.repository.redis.management.internal.ApiQualityRuleRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApiQualityRule;
import io.gravitee.repository.redis.management.model.RedisMembership;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiQualityRuleRedisRepositoryImpl extends AbstractRedisRepository implements ApiQualityRuleRedisRepository {

    private final static String REDIS_KEY = "apiqualityrule";

    @Override
    public List<RedisApiQualityRule> findByApi(String api) {
        Set<Object> keys = redisTemplate.opsForSet().members(getApiKey(api));
        List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(apiQualityRule -> convert(apiQualityRule, RedisApiQualityRule.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RedisApiQualityRule> findByQualityRule(String qualityRule) {
        Set<Object> keys = redisTemplate.opsForSet().members(getQualityRuleKey(qualityRule));
        List<Object> values = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);
        return values.stream()
                .filter(Objects::nonNull)
                .map(apiQualityRule -> convert(apiQualityRule, RedisApiQualityRule.class))
                .collect(Collectors.toList());
    }

    @Override
    public RedisApiQualityRule findById(String api, String qualityRule) {
        Object apiQualityRuleObj = redisTemplate.opsForHash().get(REDIS_KEY, getId(api, qualityRule));
        return convert(apiQualityRuleObj, RedisApiQualityRule.class);
    }

    @Override
    public void delete(String api, String qualityRule) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            final String id = getId(api, qualityRule);
            redisTemplate.opsForHash().delete(REDIS_KEY, id);
            redisTemplate.opsForSet().remove(getApiKey(api), id);
            redisTemplate.opsForSet().remove(getApiKey(api), id);
            redisTemplate.opsForSet().remove(getQualityRuleKey(qualityRule), id);
            return null;
        });
    }

    @Override
    public void deleteByApi(String api) {
        Set<Object> keys = redisTemplate.opsForSet().members(getApiKey(api));
        redisTemplate.opsForHash().delete(REDIS_KEY, keys.toArray());
        keys.forEach( key -> redisTemplate.opsForSet().remove(getApiKey(api), key));
    }

    @Override
    public void deleteByQualityRule(String qualityRule) {
        Set<Object> keys = redisTemplate.opsForSet().members(getQualityRuleKey(qualityRule));
        redisTemplate.opsForHash().delete(REDIS_KEY, keys.toArray());
        keys.forEach( key -> redisTemplate.opsForSet().remove(getQualityRuleKey(qualityRule), key));
    }

    @Override
    public RedisApiQualityRule saveOrUpdate(final RedisApiQualityRule apiQualityRule) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            final String id = getId(apiQualityRule.getApi(), apiQualityRule.getQualityRule());
            redisTemplate.opsForHash().put(REDIS_KEY, id, apiQualityRule);
            redisTemplate.opsForSet().add(getApiKey(apiQualityRule.getApi()), id);
            redisTemplate.opsForSet().add(getQualityRuleKey(apiQualityRule.getQualityRule()), id);
            return null;
        });
        return apiQualityRule;
    }

    private String getId(String api, String qualityRule) {
        return REDIS_KEY + api + ":" + qualityRule;
    }

    private String getApiKey(String api) {
        return REDIS_KEY + ":api:" + api;
    }

    private String getQualityRuleKey(String qualityRule) {
        return REDIS_KEY + ":quality_rule:" + qualityRule;
    }
}
