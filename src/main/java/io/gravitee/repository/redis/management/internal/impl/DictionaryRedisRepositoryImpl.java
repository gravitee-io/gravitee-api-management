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

import io.gravitee.repository.redis.management.internal.DictionaryRedisRepository;
import io.gravitee.repository.redis.management.internal.ViewRedisRepository;
import io.gravitee.repository.redis.management.model.RedisDictionary;
import io.gravitee.repository.redis.management.model.RedisView;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DictionaryRedisRepositoryImpl extends AbstractRedisRepository implements DictionaryRedisRepository {

    private final static String REDIS_KEY = "dictionary";

    @Override
    public Set<RedisDictionary> findAll() {
        final Map<Object, Object> dictionaries = redisTemplate.opsForHash().entries(REDIS_KEY);

        return dictionaries.values()
                .stream()
                .map(object -> convert(object, RedisDictionary.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RedisDictionary findById(final String dictionaryId) {
        Object dictionary = redisTemplate.opsForHash().get(REDIS_KEY, dictionaryId);
        if (dictionary == null) {
            return null;
        }

        return convert(dictionary, RedisDictionary.class);
    }

    @Override
    public RedisDictionary saveOrUpdate(final RedisDictionary dictionary) {
        redisTemplate.opsForHash().put(REDIS_KEY, dictionary.getId(), dictionary);
        return dictionary;
    }

    @Override
    public void delete(final String dictionary) {
        redisTemplate.opsForHash().delete(REDIS_KEY, dictionary);
    }
}
