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

import io.gravitee.repository.redis.management.internal.ViewRedisRepository;
import io.gravitee.repository.redis.management.model.RedisView;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ViewRedisRepositoryImpl extends AbstractRedisRepository implements ViewRedisRepository {

    private final static String REDIS_KEY = "view";

    @Override
    public Set<RedisView> findAll() {
        final Map<Object, Object> views = redisTemplate.opsForHash().entries(REDIS_KEY);

        return views.values()
                .stream()
                .map(object -> convert(object, RedisView.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisView findById(final String viewId) {
        Object view = redisTemplate.opsForHash().get(REDIS_KEY, viewId);
        if (view == null) {
            return null;
        }

        return convert(view, RedisView.class);
    }

    @Override
    public RedisView saveOrUpdate(final RedisView view) {
        redisTemplate.opsForHash().put(REDIS_KEY, view.getId(), view);
        return view;
    }

    @Override
    public void delete(final String view) {
        redisTemplate.opsForHash().delete(REDIS_KEY, view);
    }
}
