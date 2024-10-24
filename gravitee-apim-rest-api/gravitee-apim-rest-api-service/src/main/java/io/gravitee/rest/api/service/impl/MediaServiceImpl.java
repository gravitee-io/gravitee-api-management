/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.repository.management.api.search.MediaCriteria;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiMediaNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume Gillon
 * @author GraviteeSource Team
 */
@Component
public class MediaServiceImpl extends AbstractService implements MediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);
    private static final String MEDIA_TYPE_IMAGE = "image";

    private final MediaRepository mediaRepository;

    private final ConfigService configService;

    private final ObjectMapper objectMapper;

    public MediaServiceImpl(@Lazy MediaRepository mediaRepository, ConfigService configService, ObjectMapper objectMapper) {
        this.mediaRepository = mediaRepository;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String savePortalMedia(ExecutionContext executionContext, MediaEntity mediaEntity) {
        return this.saveApiMedia(executionContext, null, mediaEntity);
    }

    @Override
    public String saveApiMedia(ExecutionContext context, String api, MediaEntity mediaEntity) {
        try {
            // disable sonar as md5 is not used for security purpose here,
            // and we don't want to slow down the startup process
            // by adding an upgrader that would perform hashing against a potentially large amount of data
            MessageDigest digest = MessageDigest.getInstance("MD5"); // NOSONAR
            byte[] hash = digest.digest(mediaEntity.getData());
            String hashString = DatatypeConverter.printHexBinary(hash);
            String id = mediaEntity.getId() != null ? mediaEntity.getId() : UuidString.generateRandom();

            Optional<Media> checkMedia = mediaRepository.findByHash(
                hashString,
                MediaCriteria
                    .builder()
                    .organization(context.getOrganizationId())
                    .environment(context.getEnvironmentId())
                    .api(api)
                    .mediaType(mediaEntity.getType())
                    .build()
            );

            if (checkMedia.isPresent()) {
                return checkMedia.get().getHash();
            } else {
                Media media = convert(mediaEntity);

                media.setId(id);
                media.setHash(hashString);
                media.setSize((long) mediaEntity.getData().length);
                media.setApi(api);
                media.setData(mediaEntity.getData());
                media.setEnvironment(context.getEnvironmentId());
                media.setOrganization(context.getOrganizationId());
                mediaRepository.create(media);

                return hashString;
            }
        } catch (TechnicalException | NoSuchAlgorithmException ex) {
            LOGGER.error("An error has occurred while trying to create media " + mediaEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying create media");
        }
    }

    @Override
    public MediaEntity findByHash(ExecutionContext context, String hash) {
        try {
            return mediaRepository
                .findByHash(
                    hash,
                    MediaCriteria
                        .builder()
                        .organization(context.getOrganizationId())
                        .environment(context.getEnvironmentId())
                        .mediaType(MEDIA_TYPE_IMAGE)
                        .build()
                )
                .map(MediaServiceImpl::convert)
                .orElse(null);
        } catch (TechnicalException e) {
            LOGGER.error("An error has occurred trying to find media with hash " + hash, e);
            throw new TechnicalManagementException("An error has occurred trying to find media");
        }
    }

    @Override
    public MediaEntity findByHashAndApiId(String hash, String apiId) {
        try {
            return mediaRepository
                .findByHash(hash, MediaCriteria.builder().api(apiId).mediaType(MEDIA_TYPE_IMAGE).build())
                .map(MediaServiceImpl::convert)
                .orElse(null);
        } catch (TechnicalException e) {
            LOGGER.error("An error as occurred trying to find media for API " + apiId + " with hash " + hash, e);
            throw new TechnicalManagementException("An error as occurred trying to find media");
        }
    }

    @Override
    public MediaEntity findByHash(ExecutionContext context, String hash, boolean ignoreType) {
        try {
            return mediaRepository
                .findByHash(
                    hash,
                    MediaCriteria
                        .builder()
                        .organization(context.getOrganizationId())
                        .environment(context.getEnvironmentId())
                        .mediaType(ignoreType ? null : MEDIA_TYPE_IMAGE)
                        .build()
                )
                .map(MediaServiceImpl::convert)
                .orElse(null);
        } catch (TechnicalException e) {
            LOGGER.error("An error as occurred trying to find media with hash " + hash, e);
            throw new TechnicalManagementException("An error has occurred trying to find media");
        }
    }

    @Override
    public MediaEntity findByHashAndApi(String hash, String api, boolean ignoreType) {
        try {
            return mediaRepository
                .findByHash(hash, MediaCriteria.builder().api(api).mediaType(ignoreType ? null : MEDIA_TYPE_IMAGE).build())
                .map(MediaServiceImpl::convert)
                .orElse(null);
        } catch (TechnicalException e) {
            LOGGER.error("An error as occurred trying to find media for API " + api + " with hash " + hash, e);
            throw new TechnicalManagementException("An error as occurred trying to find media");
        }
    }

    @Override
    public List<MediaEntity> findAllWithoutContent(ExecutionContext context, List<PageMediaEntity> pageMediaEntities) {
        return this.findAllWithoutContent(
                pageMediaEntities,
                MediaCriteria.builder().organization(context.getOrganizationId()).environment(context.getEnvironmentId()).build()
            );
    }

    @Override
    public List<MediaEntity> findAllWithoutContent(List<PageMediaEntity> pageMediaEntities, String api) {
        return this.findAllWithoutContent(pageMediaEntities, MediaCriteria.builder().api(api).build());
    }

    private List<MediaEntity> findAllWithoutContent(List<PageMediaEntity> pageMediaEntities, MediaCriteria mediaCriteria) {
        try {
            List<MediaEntity> result = new ArrayList<>();
            if (pageMediaEntities != null && !pageMediaEntities.isEmpty()) {
                for (PageMediaEntity pme : pageMediaEntities) {
                    final Optional<Media> foundMedia = mediaRepository.findByHash(pme.getMediaHash(), mediaCriteria, false);
                    if (foundMedia.isPresent()) {
                        MediaEntity me = convert(foundMedia.get());
                        me.setFileName(pme.getMediaName());
                        me.setUploadDate(pme.getAttachedAt());
                        result.add(me);
                    }
                }
            }
            return result;
        } catch (TechnicalException e) {
            LOGGER.error("An error as occurred trying to find medias with criteria " + mediaCriteria, e);
            throw new TechnicalManagementException("An error as occurred trying to find medias");
        }
    }

    @Override
    public Long getMediaMaxSize(final ExecutionContext executionContext) {
        return Long.valueOf(configService.getPortalSettings(executionContext).getPortal().getUploadMedia().getMaxSizeInOctet());
    }

    @Override
    public List<MediaEntity> findAllByApiId(final String apiId) {
        try {
            return mediaRepository.findAllByApi(apiId).stream().map(MediaServiceImpl::convert).toList();
        } catch (TechnicalException e) {
            LOGGER.error("An error as occurred trying to find medias for API " + apiId, e);
            throw new TechnicalManagementException("An error as occurred trying to find medias");
        }
    }

    @Override
    public String createWithDefinition(ExecutionContext executionContext, String api, String mediaDefinition) {
        try {
            final MediaEntity media = convertToEntity(mediaDefinition);
            return saveApiMedia(executionContext, api, media);
        } catch (JsonProcessingException e) {
            LOGGER.error("An error as occurred while trying to JSON deserialize the media " + mediaDefinition, e);
            throw new TechnicalManagementException("An error has occurred while trying to create media");
        }
    }

    @Override
    public void deleteAllByApi(String apiId) {
        try {
            mediaRepository.deleteAllByApi(apiId);
        } catch (TechnicalException e) {
            LOGGER.error("An error has occurred while trying delete medias for API " + apiId, e);
            throw new TechnicalManagementException("An error occurred trying to delete medias");
        }
    }

    @Override
    public void deleteByHashAndApi(String hash, String apiId) {
        try {
            final MediaCriteria mediaCriteria = MediaCriteria.builder().api(apiId).build();
            Media media = mediaRepository.findByHash(hash, mediaCriteria).orElseThrow(() -> new ApiMediaNotFoundException(hash, apiId));
            mediaRepository.deleteByHashAndApi(media.getHash(), apiId);
        } catch (TechnicalException e) {
            LOGGER.error("An error has occurred trying to delete media for API " + apiId + " with hash " + hash, e);
            throw new TechnicalManagementException("An error has occurred trying to delete media");
        }
    }

    @Override
    public void deletePortalMediaByHash(ExecutionContext executionContext, String hash) {
        try {
            final MediaCriteria mediaCriteria = MediaCriteria
                .builder()
                .organization(executionContext.getOrganizationId())
                .environment(executionContext.getEnvironmentId())
                .build();

            Media media = mediaRepository.findByHash(hash, mediaCriteria).orElseThrow(() -> new TechnicalManagementException(hash));

            mediaRepository.deleteByHashAndEnvironment(media.getHash(), executionContext.getEnvironmentId());
        } catch (TechnicalException e) {
            LOGGER.error("An error has occurred trying to delete media with hash " + hash, e);
            throw new TechnicalManagementException("An error has occurred trying to delete media");
        }
    }

    private MediaEntity convertToEntity(String mediaDefinition) throws JsonProcessingException {
        return objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(mediaDefinition, MediaEntity.class);
    }

    private static Media convert(MediaEntity imageEntity) {
        Media media = new Media();
        media.setFileName(imageEntity.getFileName());
        media.setSize(imageEntity.getSize());
        media.setType(imageEntity.getType());
        media.setSubType(imageEntity.getSubType());
        media.setId(imageEntity.getId());
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
