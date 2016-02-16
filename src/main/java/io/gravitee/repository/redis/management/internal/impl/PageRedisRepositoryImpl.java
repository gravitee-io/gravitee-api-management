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

import io.gravitee.repository.redis.management.internal.PageRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class PageRedisRepositoryImpl extends AbstractRedisRepository implements PageRedisRepository {

    private final static String REDIS_KEY = "page";

    @Override
    public RedisPage find(String pageId) {
        Object page = redisTemplate.opsForHash().get(REDIS_KEY, pageId);
        if (page == null) {
            return null;
        }

        return convert(page, RedisPage.class);
    }

    @Override
    public RedisPage saveOrUpdate(RedisPage page) {
        redisTemplate.opsForHash().put(REDIS_KEY, page.getId(), page);
        redisTemplate.opsForSet().add(REDIS_KEY + ":api:" + page.getApi(), page.getId());
        return page;
    }

    @Override
    public void delete(String pageId) {
        RedisPage page = find(pageId);
        redisTemplate.opsForHash().delete(REDIS_KEY, pageId);
        redisTemplate.opsForSet().remove(REDIS_KEY + ":api:" + page.getApi(), pageId);
    }

    @Override
    public Set<RedisPage> findByApi(String api) {
        Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":api:" + api);
        List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(event -> convert(event, RedisPage.class))
                .collect(Collectors.toSet());
    }
}
