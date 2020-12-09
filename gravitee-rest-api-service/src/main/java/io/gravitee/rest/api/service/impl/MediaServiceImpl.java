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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Guillaume Gillon
 */
@Component
public class MediaServiceImpl implements MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String savePortalMedia(MediaEntity mediaEntity) {
        return this.saveApiMedia(null, mediaEntity);
    }

    @Override
    public String saveApiMedia(String api, MediaEntity mediaEntity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(mediaEntity.getData());
            String hashString = DatatypeConverter.printHexBinary(hash);
            String id = mediaEntity.getId() != null && UUID.fromString(mediaEntity.getId()) != null ? mediaEntity.getId() : RandomString.generate();

            Optional<Media> checkMedia = null;

            if (api != null) {
                checkMedia = mediaRepository.findByHashAndApiAndType(hashString, api, mediaEntity.getType());
            } else {
                checkMedia = mediaRepository.findByHashAndType(hashString, mediaEntity.getType());
            }

            if (checkMedia.isPresent()) {
                return checkMedia.get().getHash();
            } else {
                Media media = convert(mediaEntity);

                media.setId(id);
                media.setHash(hashString);
                media.setSize((long) mediaEntity.getData().length);
                media.setApi(api);
                media.setData(mediaEntity.getData());
                mediaRepository.create(media);

                return hashString;
            }

        } catch (TechnicalException | NoSuchAlgorithmException ex) {
            LOGGER.error("An error occurs while trying to create {}", mediaEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + mediaEntity, ex);
        }
    }

    @Override
    public MediaEntity findByHash(String hash) {
        Optional<Media> mediaData = mediaRepository.findByHashAndType(hash, "image");
        return mediaData.isPresent() ? convert(mediaData.get()) : null;
    }

    @Override
    public MediaEntity findByHashAndApiId(String hash, String apiId) {
        Optional<Media> mediaData = mediaRepository.findByHashAndApiAndType(hash, apiId, "image");
        return mediaData.isPresent() ? convert(mediaData.get()) : null;
    }

    @Override
    public MediaEntity findByHash(String id, boolean ignoreType) {
        Optional<Media> mediaData;
        if (ignoreType) {
            mediaData = mediaRepository.findByHash(id);
        } else {
            mediaData = mediaRepository.findByHashAndType(id, "image");
        }
        return mediaData.isPresent() ? convert(mediaData.get()) : null;
    }

    @Override
    public MediaEntity findByHashAndApi(String id, String api, boolean ignoreType) {
        Optional<Media> mediaData;
        if (ignoreType) {
            mediaData = mediaRepository.findByHashAndApi(id, api);
        } else {
            mediaData = mediaRepository.findByHashAndApiAndType(id, api, "image");
        }
        return mediaData.isPresent() ? convert(mediaData.get()) : null;
    }

    @Override
    public List<MediaEntity> findAllWithoutContent(List<PageMediaEntity> pageMediaEntities) {
        return this.findAllWithoutContent(pageMediaEntities, null);
    }

    @Override
    public List<MediaEntity> findAllWithoutContent(List<PageMediaEntity> pageMediaEntities, String api) {
        List<MediaEntity> result = new ArrayList<>();
        if (pageMediaEntities != null && !pageMediaEntities.isEmpty()) {
            for (PageMediaEntity pme : pageMediaEntities) {
                final Optional<Media> foundMedia = mediaRepository.findByHashAndApi(pme.getMediaHash(), api, false);
                if (foundMedia.isPresent()) {
                    MediaEntity me = this.convert(foundMedia.get());
                    me.setFileName(pme.getMediaName());
                    me.setUploadDate(pme.getAttachedAt());
                    result.add(me);
                }
            }
        }
        return result;

    }

    @Override
    public Long getMediaMaxSize() {
        return Long.valueOf(configService.getPortalConfig().getPortal().getUploadMedia().getMaxSizeInOctet());
    }

    @Override
    public List<MediaEntity> findAllByApiId(String apiId) {
        return mediaRepository.findAllByApi(apiId)
            .stream()
            .map(media -> convert(media)).collect(Collectors.toList());
    }

    @Override
    public String createWithDefinition(String api, String mediaDefinition) {
        try {
            final MediaEntity media = convertToEntity(mediaDefinition);
            return saveApiMedia(api, media);
        } catch (JsonProcessingException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the media {}", mediaDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the Media definition.");
        }
    }

    @Override
    public void deleteAllByApi(String apiId) {
        mediaRepository.deleteAllByApi(apiId);
    }

    private MediaEntity convertToEntity(String mediaDefinition) throws JsonProcessingException {
        final MediaEntity media = objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(mediaDefinition, MediaEntity.class);
        return media;
    }

    private static Media convert(MediaEntity imageEntity) {
        Media media = new Media();
        media.setFileName(imageEntity.getFileName());
        media.setSize(imageEntity.getSize());
        media.setType(imageEntity.getType());
        media.setSubType(imageEntity.getSubType());
        media.setId(imageEntity.getId());
        //media.setData(new ByteArrayInputStream(imageEntity.getData()));
        return media;
    }

    private static MediaEntity convert(Media media) {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(media.getId());
        mediaEntity.setData(media.getData());
        mediaEntity.setType(media.getType());
        mediaEntity.setSubType(media.getSubType());
        mediaEntity.setFileName(media.getFileName());
        mediaEntity.setSize(media.getSize());
        mediaEntity.setUploadDate(media.getCreatedAt());
        mediaEntity.setHash(media.getHash());
        return mediaEntity;
    }
}
