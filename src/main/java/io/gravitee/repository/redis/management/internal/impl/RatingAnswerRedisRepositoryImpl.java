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

import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.redis.management.internal.RatingAnswerRedisRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingAnswerRedisRepositoryImpl extends AbstractRedisRepository implements RatingAnswerRedisRepository {

    private final static String REDIS_KEY = "ratinganswer";

    @Override
    public RatingAnswer findById(final String ratingAnswerId) {
        Object ratingAnswer = redisTemplate.opsForHash().get(REDIS_KEY, ratingAnswerId);
        if (ratingAnswer == null) {
            return null;
        }

        return convert(ratingAnswer, RatingAnswer.class);
    }

    @Override
    public RatingAnswer saveOrUpdate(final RatingAnswer ratingAnswer) {
        redisTemplate.opsForHash().put(REDIS_KEY, ratingAnswer.getId(), ratingAnswer);
        redisTemplate.opsForSet().add(REDIS_KEY + ":rating:" + ratingAnswer.getRating(), ratingAnswer.getId());
        return ratingAnswer;
    }

    @Override
    public void delete(final String ratingAnswer) {
        redisTemplate.opsForHash().delete(REDIS_KEY, ratingAnswer);
    }

    @Override
    public List<RatingAnswer> findByRating(final String rating) {
        final Set<Object> keys = redisTemplate.opsForSet().members(REDIS_KEY + ":rating:" + rating);
        final List<Object> pageObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, keys);

        return pageObjects.stream()
                .map(ratingAnswer -> convert(ratingAnswer, RatingAnswer.class))
                .collect(toList());
    }
}
