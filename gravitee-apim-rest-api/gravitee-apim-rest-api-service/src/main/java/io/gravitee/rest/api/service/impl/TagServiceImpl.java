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

import static io.gravitee.repository.management.model.Audit.AuditProperties.TAG;
import static io.gravitee.repository.management.model.Tag.AuditEvent.*;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.model.UpdateTagEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateTagNameException;
import io.gravitee.rest.api.service.exceptions.TagNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TagServiceImpl extends AbstractService implements TagService {

    private final Logger LOGGER = LoggerFactory.getLogger(TagServiceImpl.class);

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private GroupService groupService;

    @Override
    public List<TagEntity> findByReference(String referenceId, TagReferenceType referenceType) {
        try {
            LOGGER.debug("Find all tags");
            return tagRepository
                .findByReference(referenceId, repoTagReferenceType(referenceType))
                .stream()
                .map(this::convert)
                .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all tags", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all tags", ex);
        }
    }

    @Override
    public TagEntity findByIdAndReference(String tagId, String referenceId, TagReferenceType referenceType) {
        try {
            LOGGER.debug("Find tag by ID: {}", tagId);
            Optional<Tag> optTag = tagRepository.findByIdAndReference(tagId, referenceId, repoTagReferenceType(referenceType));

            if (!optTag.isPresent()) {
                throw new TagNotFoundException(tagId);
            }

            return convert(optTag.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find tag by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find tag by ID", ex);
        }
    }

    @Override
    public TagEntity create(final ExecutionContext executionContext, NewTagEntity tag, String referenceId, TagReferenceType referenceType) {
        return create(executionContext, singletonList(tag), referenceId, referenceType).get(0);
    }

    @Override
    public TagEntity update(
        final ExecutionContext executionContext,
        UpdateTagEntity tag,
        String referenceId,
        TagReferenceType referenceType
    ) {
        return update(executionContext, singletonList(tag), referenceId, referenceType).get(0);
    }

    @Override
    public List<TagEntity> create(
        final ExecutionContext executionContext,
        final List<NewTagEntity> tagEntities,
        String referenceId,
        TagReferenceType referenceType
    ) {
        // First we prevent the duplicate tag name
        final List<String> tagNames = tagEntities.stream().map(NewTagEntity::getName).collect(toList());

        final Optional<TagEntity> optionalTag = findByReference(referenceId, referenceType)
            .stream()
            .filter(tag -> tagNames.contains(tag.getName()))
            .findAny();

        if (optionalTag.isPresent()) {
            throw new DuplicateTagNameException(optionalTag.get().getName());
        }

        final List<TagEntity> savedTags = new ArrayList<>(tagEntities.size());
        tagEntities.forEach(
            tagEntity -> {
                try {
                    Tag tag = convert(tagEntity, referenceId, referenceType);
                    savedTags.add(convert(tagRepository.create(tag)));
                    auditService.createOrganizationAuditLog(
                        executionContext,
                        executionContext.getOrganizationId(),
                        Collections.singletonMap(TAG, tag.getId()),
                        TAG_CREATED,
                        new Date(),
                        null,
                        tag
                    );
                } catch (TechnicalException ex) {
                    LOGGER.error("An error occurs while trying to create tag {}", tagEntity.getName(), ex);
                    throw new TechnicalManagementException("An error occurs while trying to create tag " + tagEntity.getName(), ex);
                }
            }
        );
        return savedTags;
    }

    @Override
    public List<TagEntity> update(
        final ExecutionContext executionContext,
        final List<UpdateTagEntity> tagEntities,
        String referenceId,
        TagReferenceType referenceType
    ) {
        final List<TagEntity> savedTags = new ArrayList<>(tagEntities.size());
        tagEntities.forEach(
            tagEntity -> {
                try {
                    Tag tag = convert(tagEntity);
                    Optional<Tag> tagOptional = tagRepository.findByIdAndReference(
                        tag.getId(),
                        referenceId,
                        repoTagReferenceType(referenceType)
                    );
                    if (tagOptional.isPresent()) {
                        Tag existingTag = tagOptional.get();
                        tag.setReferenceId(existingTag.getReferenceId());
                        tag.setReferenceType(existingTag.getReferenceType());
                        savedTags.add(convert(tagRepository.update(tag)));
                        auditService.createOrganizationAuditLog(
                            executionContext,
                            executionContext.getOrganizationId(),
                            Collections.singletonMap(TAG, tag.getId()),
                            TAG_UPDATED,
                            new Date(),
                            tagOptional.get(),
                            tag
                        );
                    }
                } catch (TechnicalException ex) {
                    LOGGER.error("An error occurs while trying to update tag {}", tagEntity.getName(), ex);
                    throw new TechnicalManagementException("An error occurs while trying to update tag " + tagEntity.getName(), ex);
                }
            }
        );
        return savedTags;
    }

    @Override
    public void delete(final ExecutionContext executionContext, final String tagId, String referenceId, TagReferenceType referenceType) {
        try {
            Optional<Tag> tagOptional = tagRepository.findByIdAndReference(tagId, referenceId, repoTagReferenceType(referenceType));
            if (tagOptional.isPresent()) {
                tagRepository.delete(tagId);
                // delete all reference on APIs
                apiService.deleteTagFromAPIs(executionContext, tagId);
                auditService.createOrganizationAuditLog(
                    executionContext,
                    executionContext.getOrganizationId(),
                    Collections.singletonMap(TAG, tagId),
                    TAG_DELETED,
                    new Date(),
                    null,
                    tagOptional.get()
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete tag {}", tagId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete tag " + tagId, ex);
        }
    }

    @Override
    public Set<String> findByUser(final String user, String referenceId, TagReferenceType referenceType) {
        final List<TagEntity> tags = findByReference(referenceId, referenceType);
        if (isAdmin()) {
            return tags.stream().map(TagEntity::getId).collect(toSet());
        } else {
            final Set<String> restrictedTags = tags
                .stream()
                .filter(tag -> tag.getRestrictedGroups() != null && !tag.getRestrictedGroups().isEmpty())
                .map(TagEntity::getId)
                .collect(toSet());

            final Set<String> groups = groupService.findByUser(user).stream().map(GroupEntity::getId).collect(toSet());

            return tags
                .stream()
                .filter(
                    tag ->
                        !restrictedTags.contains(tag.getId()) ||
                        (tag.getRestrictedGroups() != null && anyMatch(tag.getRestrictedGroups(), groups))
                )
                .map(TagEntity::getId)
                .collect(toSet());
        }
    }

    private boolean anyMatch(final List<String> restrictedGroups, final Set<String> groups) {
        for (final String restrictedGroup : restrictedGroups) {
            if (groups.contains(restrictedGroup)) {
                return true;
            }
        }
        return false;
    }

    private Tag convert(final NewTagEntity tagEntity, String referenceId, TagReferenceType referenceType) {
        final Tag tag = new Tag();
        tag.setId(IdGenerator.generate(tagEntity.getName()));
        tag.setName(tagEntity.getName());
        tag.setDescription(tagEntity.getDescription());
        tag.setRestrictedGroups(tagEntity.getRestrictedGroups());
        tag.setReferenceId(referenceId);
        tag.setReferenceType(repoTagReferenceType(referenceType));
        return tag;
    }

    private Tag convert(final UpdateTagEntity tagEntity) {
        final Tag tag = new Tag();
        tag.setId(tagEntity.getId());
        tag.setName(tagEntity.getName());
        tag.setDescription(tagEntity.getDescription());
        tag.setRestrictedGroups(tagEntity.getRestrictedGroups());
        return tag;
    }

    private TagEntity convert(final Tag tag) {
        final TagEntity tagEntity = new TagEntity();
        tagEntity.setId(tag.getId());
        tagEntity.setName(tag.getName());
        tagEntity.setDescription(tag.getDescription());
        tagEntity.setRestrictedGroups(tag.getRestrictedGroups());
        return tagEntity;
    }

    private io.gravitee.repository.management.model.TagReferenceType repoTagReferenceType(TagReferenceType referenceType) {
        return io.gravitee.repository.management.model.TagReferenceType.valueOf(referenceType.name());
    }
}
