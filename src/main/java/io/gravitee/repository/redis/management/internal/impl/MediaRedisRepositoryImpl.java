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

import io.gravitee.repository.media.model.Media;
import io.gravitee.repository.redis.management.internal.MediaRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMedia;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class MediaRedisRepositoryImpl extends AbstractRedisRepository implements MediaRedisRepository {

    private static final String REDIS_KEY = "media";
    private final static String REDIS_KEY_API = REDIS_KEY + ":api" ;
    private final static String REDIS_KEY_PORTAL = REDIS_KEY + ":portal";

    public String save(RedisMedia media) {

        media.setCreatedAt(new Date());
        redisTemplate.opsForHash().put(REDIS_KEY, media.getId(), media);

        String key = null;
        if (media.getApi() != null) {
            key = REDIS_KEY_API + ":" + media.getType() + ":" + media.getApi();
        } else {
            key = REDIS_KEY_PORTAL+ ":" + media.getType();
        }

        redisTemplate.opsForSet().add(key, media.getId());

        return media.getId();
    }

    @Override
    public Optional<RedisMedia> findMediaBy(String hash, String mediaType) {
        return this.findMediaBy(hash, null, mediaType);
    }

    @Override
    public Optional<RedisMedia> findMediaBy(String hash, String api, String mediaType) {

        String key = null;
        if(api == null) {
            key = REDIS_KEY_PORTAL+ ":" + mediaType;
        } else {
            key = REDIS_KEY_API+ ":" + mediaType+ ":" + api;
        }

        Set<Object> mediaSet = redisTemplate.opsForSet().members(key);
        List<Object> mediaObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, mediaSet);

        return mediaObjects.stream()
                .map(event -> convert(event, RedisMedia.class))
                .filter(event -> event.getHash().equals(hash))
                .findFirst();
    }

    @Override
    public long totalSizeFor(String api, String mediaType) {

        String key = null;
        if(api == null) {
            key = REDIS_KEY_PORTAL+ ":" + mediaType;
        } else {
            key = REDIS_KEY_API+ ":" + mediaType+ ":" + api;
        }
        Set<Object> mediaSet = redisTemplate.opsForSet().members(key);

        List<Object> mediaObjects = redisTemplate.opsForHash().multiGet(REDIS_KEY, mediaSet);

        return mediaObjects.stream()
                .map(event -> convert(event, RedisMedia.class))
                .mapToLong(event -> event.getSize()).sum();
    }

    @Override
    public void deleteApiMediaFor(String hash, String api, String mediaType) {
        Optional<RedisMedia> redisMedia = findMediaBy(hash, api, mediaType);

        redisTemplate.opsForHash().delete(REDIS_KEY, redisMedia.get().getId());

        if(api == null) {
            redisTemplate.opsForSet().remove(REDIS_KEY_PORTAL+ ":" + mediaType, redisMedia.get().getId());
        } else {
            redisTemplate.opsForSet().remove(REDIS_KEY_API+ ":" + mediaType+ ":" + api, redisMedia.get().getId());
        }
    }

}
