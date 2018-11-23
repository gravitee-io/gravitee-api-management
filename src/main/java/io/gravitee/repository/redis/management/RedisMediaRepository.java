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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import io.gravitee.repository.redis.management.internal.MediaRedisRepository;
import io.gravitee.repository.redis.management.model.RedisMedia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author Guillaume GILLON
 */
@Repository
public class RedisMediaRepository implements MediaRepository {

    @Autowired
    private MediaRedisRepository mediaRedisRepository;

    @Override
    public String save(Media media) throws TechnicalException {

         return mediaRedisRepository.save(convert(media));
    }

    @Override
    public Optional<Media> findByHash(String hash, String mediaType) {
        return this.findByHash(hash, null, mediaType);
    }

    @Override
    public Optional<Media> findByHash(String hash, String api, String mediaType) {
        return Optional.ofNullable(convert(mediaRedisRepository.findMediaBy(hash, api, mediaType).orElse(null)));
    }

//    @Override
//    public void delete(String hash, String mediaType) {
//        this.deleteApiFor(hash, null, mediaType);
//    }

//    @Override
//    public void deleteApiFor(String hash, String api, String mediaType) {
//        mediaRedisRepository.deleteApiMediaFor(hash, api, mediaType);
//    }

//    @Override
//    public long totalSizeFor(String api, String mediaType) {
//        return mediaRedisRepository.totalSizeFor(api, mediaType);
//    }

    private Media convert(RedisMedia redisMedia) {
        if (redisMedia == null) {
            return null;
        }

        Media media = new Media();

        media.setId(redisMedia.getId());

        media.setType(redisMedia.getType());
        media.setSubType(redisMedia.getSubType());
        media.setFileName(redisMedia.getFileName());
        media.setCreatedAt(redisMedia.getCreatedAt());
        media.setSize(redisMedia.getSize());
        media.setData(redisMedia.getData());
        media.setApi(redisMedia.getApi());
        media.setHash(redisMedia.getHash());

        return media;
    }

    private RedisMedia convert(Media media) {
        if (media == null) {
            return null;
        }

        RedisMedia redisMedia = new RedisMedia();

        redisMedia.setId(media.getId());

        redisMedia.setType(media.getType());
        redisMedia.setSubType(media.getSubType());
        redisMedia.setFileName(media.getFileName());
        redisMedia.setCreatedAt(media.getCreatedAt());
        redisMedia.setSize(media.getSize());

        redisMedia.setApi(media.getApi());
        redisMedia.setHash(media.getHash());
        redisMedia.setData(media.getData());

        return redisMedia;
    }
}
