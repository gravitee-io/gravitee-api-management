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
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.redis.management.internal.RatingRedisRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingRedisRepositoryImpl extends AbstractRedisRepository implements RatingRedisRepository {

    private final static String REDIS_KEY = "rating";

    @Override
    public Set<Rating> findAll() {
        final Map<Object, Object> ratings = redisTemplate.opsForHash().entries(REDIS_KEY);

        return ratings.values()
                .stream()
                .map(object -> convert(object, Rating.class))
                .collect(Collectors.toSet());
    }

    @Override
    public Rating findById(final String ratingId) {
        Object rating = redisTemplate.opsForHash().get(REDIS_KEY, ratingId);
        if (rating == null) {
            return null;
        }

        return convert(rating, Rating.class);
    }

    @Override
    public Rating saveOrUpdate(final Rating rating) {
        redisTemplate.opsForHash().put(REDIS_KEY, rating.getId(), rating);
        redisTemplate.opsForSet().add(REDIS_KEY + ":api:" + rating.getApi(), rating.getId());
        redisTemplate.opsForSet().add(REDIS_KEY + ":api:" + rating.getApi() + ":user:" + rating.getUser(), rating.getId());
        return rating;
    }

    @Override
    public void delete(final String rating) {
        redisTemplate.opsForHash().delete(REDIS_KEY, rating);
    }

    @Override
    public List<Rating> findByApi(final String api) {
        final Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":api:" + api);
        final List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(rating -> convert(rating, Rating.class))
                .collect(toList());
    }

    @Override
    public Rating findByApiAndUser(final String api, final String user) {
        final Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":api:" + api + ":user:" + user);
        final List<Object> ratings = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return ratings.stream()
                .map(rating -> convert(rating, Rating.class))
                .findFirst().get();
    }

    @Override
    public Page<Rating> findByApi(final String api, final PageRequest pageRequest) {
        final Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":api:" + api);
        final List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        final int limit = pageRequest.getPageNumber() * pageRequest.getPageSize();
        final List<Rating> ratings = pageObjects.
                stream().
                map(rating -> convert(rating, Rating.class)).
                sorted(comparing(Rating::getCreatedAt).reversed()).
                skip(limit - pageRequest.getPageSize()).
                limit(limit).
                collect(toList());

        return new Page<>(ratings, pageRequest.getPageNumber(), ratings.size(), pageObjects.size());
    }
}
