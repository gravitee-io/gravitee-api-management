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
package io.gravitee.management.service.impl;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.NewTagEntity;
import io.gravitee.management.model.UpdateTagEntity;
import io.gravitee.management.model.TagEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.AuditService;
import io.gravitee.management.service.TagService;
import io.gravitee.management.service.exceptions.DuplicateTagNameException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.TAG;
import static io.gravitee.repository.management.model.Tag.AuditEvent.TAG_CREATED;
import static io.gravitee.repository.management.model.Tag.AuditEvent.TAG_DELETED;
import static io.gravitee.repository.management.model.Tag.AuditEvent.TAG_UPDATED;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TagServiceImpl extends TransactionalService implements TagService {

    private final Logger LOGGER = LoggerFactory.getLogger(TagServiceImpl.class);

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private AuditService auditService;

    @Override
    public List<TagEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            return tagRepository.findAll()
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all tags", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all tags", ex);
        }
    }

    @Override
    public List<TagEntity> create(final List<NewTagEntity> tagEntities) {
        // First we prevent the duplicate tag name
        final List<String> tagNames = tagEntities.stream()
                .map(NewTagEntity::getName)
                .collect(Collectors.toList());

        final Optional<TagEntity> optionalTag = findAll().stream()
                .filter(tag -> tagNames.contains(tag.getName()))
                .findAny();

        if (optionalTag.isPresent()) {
            throw new DuplicateTagNameException(optionalTag.get().getName());
        }

        final List<TagEntity> savedTags = new ArrayList<>(tagEntities.size());
        tagEntities.forEach(tagEntity -> {
            try {
                Tag tag = convert(tagEntity);
                savedTags.add(convert(tagRepository.create(tag)));
                auditService.createPortalAuditLog(
                        Collections.singletonMap(TAG, tag.getId()),
                        TAG_CREATED,
                        new Date(),
                        null,
                        tag);
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to create tag {}", tagEntity.getName(), ex);
                throw new TechnicalManagementException("An error occurs while trying to create tag " + tagEntity.getName(), ex);
            }
        });
        return savedTags;
    }

    @Override
    public List<TagEntity> update(final List<UpdateTagEntity> tagEntities) {
        final List<TagEntity> savedTags = new ArrayList<>(tagEntities.size());
        tagEntities.forEach(tagEntity -> {
            try {
                Tag tag = convert(tagEntity);
                Optional<Tag> tagOptional = tagRepository.findById(tag.getId());
                if (tagOptional.isPresent()) {
                    savedTags.add(convert(tagRepository.update(tag)));
                    auditService.createPortalAuditLog(
                            Collections.singletonMap(TAG, tag.getId()),
                            TAG_UPDATED,
                            new Date(),
                            tagOptional.get(),
                            tag);
                }
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to update tag {}", tagEntity.getName(), ex);
                throw new TechnicalManagementException("An error occurs while trying to update tag " + tagEntity.getName(), ex);
            }
        });
        return savedTags;
    }

    @Override
    public void delete(final String tagId) {
        try {
            Optional<Tag> tagOptional = tagRepository.findById(tagId);
            if (tagOptional.isPresent()) {
                tagRepository.delete(tagId);
                auditService.createPortalAuditLog(
                        Collections.singletonMap(TAG, tagId),
                        TAG_DELETED,
                        new Date(),
                        null,
                        tagOptional.get());
                // delete all reference on APIs
                apiService.deleteTagFromAPIs(tagId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete tag {}", tagId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete tag " + tagId, ex);
        }
    }

    private Tag convert(final NewTagEntity tagEntity) {
        final Tag tag = new Tag();
        tag.setId(IdGenerator.generate(tagEntity.getName()));
        tag.setName(tagEntity.getName());
        tag.setDescription(tagEntity.getDescription());
        return tag;
    }

    private Tag convert(final UpdateTagEntity tagEntity) {
        final Tag tag = new Tag();
        tag.setId(tagEntity.getId());
        tag.setName(tagEntity.getName());
        tag.setDescription(tagEntity.getDescription());
        return tag;
    }

    private TagEntity convert(final Tag tag) {
        final TagEntity tagEntity = new TagEntity();
        tagEntity.setId(tag.getId());
        tagEntity.setName(tag.getName());
        tagEntity.setDescription(tag.getDescription());
        return tagEntity;
    }
}
