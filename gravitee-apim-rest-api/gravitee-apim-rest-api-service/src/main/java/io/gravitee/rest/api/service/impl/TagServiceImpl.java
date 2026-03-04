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

import static io.gravitee.repository.management.model.Audit.AuditProperties.TAG;
import static io.gravitee.repository.management.model.Tag.AuditEvent.TAG_CREATED;
import static io.gravitee.repository.management.model.Tag.AuditEvent.TAG_DELETED;
import static io.gravitee.repository.management.model.Tag.AuditEvent.TAG_UPDATED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.model.UpdateTagEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DuplicateTagKeyException;
import io.gravitee.rest.api.service.exceptions.TagNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiTagService;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class TagServiceImpl extends AbstractService implements TagService {

    @Lazy
    @Autowired
    private TagRepository tagRepository;

    @Autowired
    @Lazy
    private ApiTagService apiTagService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private GroupService groupService;

    @Override
    public List<TagEntity> findByReference(String referenceId, TagReferenceType referenceType) {
        try {
            log.debug("Find all tags");
            return tagRepository
                .findByReference(referenceId, repoTagReferenceType(referenceType))
                .stream()
                .map(this::convert)
                .collect(toList());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find all tags", ex);
        }
    }

    @Override
    public TagEntity findByKeyAndReference(String key, String referenceId, TagReferenceType referenceType) {
        try {
            log.debug("Find tag by key: {}", key);
            Optional<Tag> optTag = tagRepository.findByKeyAndReference(key, referenceId, repoTagReferenceType(referenceType));

            if (optTag.isEmpty()) {
                throw new TagNotFoundException(key);
            }

            return convert(optTag.get());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find tag by key", ex);
        }
    }

    @Override
    public void checkTagsExist(Set<String> keys, String referenceId, TagReferenceType referenceType) throws TechnicalException {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        Set<Tag> foundTags = tagRepository.findByKeysAndReference(keys, referenceId, repoTagReferenceType(referenceType));

        if (foundTags.size() == keys.size()) {
            return;
        }

        Set<String> foundKeys = foundTags.stream().map(Tag::getKey).collect(toSet());

        Set<String> notFoundKeys = keys
            .stream()
            .filter(key -> !foundKeys.contains(key))
            .collect(toSet());

        if (!notFoundKeys.isEmpty()) {
            throw new TagNotFoundException(notFoundKeys.toArray(String[]::new));
        }
    }

    @Override
    public TagEntity create(
        final ExecutionContext executionContext,
        NewTagEntity tagEntity,
        String referenceId,
        TagReferenceType referenceType
    ) {
        final var optionalTag = findByReference(referenceId, referenceType)
            .stream()
            .filter(tag -> tag.getKey().equals(tagEntity.getKey()))
            .findAny();

        if (optionalTag.isPresent()) {
            throw new DuplicateTagKeyException(optionalTag.get().getKey());
        }

        try {
            var tagToSaved = convert(tagEntity, referenceId, referenceType);
            var tagSaved = tagRepository.create(tagToSaved);
            auditService.createOrganizationAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(TAG, tagSaved.getKey()))
                    .event(TAG_CREATED)
                    .createdAt(new Date())
                    .oldValue(null)
                    .newValue(tagSaved)
                    .build()
            );

            return convert(tagSaved);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to create tag " + tagEntity.getName(), ex);
        }
    }

    @Override
    public TagEntity update(
        final ExecutionContext executionContext,
        String tagKey,
        UpdateTagEntity updateTagEntity,
        String referenceId,
        TagReferenceType referenceType
    ) {
        try {
            var tagOptional = tagRepository.findByKeyAndReference(tagKey, referenceId, repoTagReferenceType(referenceType));

            if (tagOptional.isEmpty()) {
                throw new TagNotFoundException(tagKey);
            }

            var existingTag = tagOptional.get();
            var tagToSave = Tag.builder()
                .id(existingTag.getId())
                .key(existingTag.getKey())
                .name(updateTagEntity.getName())
                .description(updateTagEntity.getDescription())
                .restrictedGroups(updateTagEntity.getRestrictedGroups())
                .referenceId(existingTag.getReferenceId())
                .referenceType(existingTag.getReferenceType())
                .build();

            var tagSaved = tagRepository.update(tagToSave);
            auditService.createOrganizationAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(TAG, tagSaved.getKey()))
                    .event(TAG_UPDATED)
                    .createdAt(new Date())
                    .oldValue(existingTag)
                    .newValue(tagSaved)
                    .build()
            );

            return convert(tagSaved);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to update tag " + tagKey, ex);
        }
    }

    @Override
    public void delete(final ExecutionContext executionContext, final String key, String referenceId, TagReferenceType referenceType) {
        try {
            Optional<Tag> tagOptional = tagRepository.findByKeyAndReference(key, referenceId, repoTagReferenceType(referenceType));

            if (tagOptional.isPresent()) {
                String actualTagId = tagOptional.get().getId();
                tagRepository.delete(actualTagId);
                // delete all reference on APIs
                apiTagService.deleteTagFromAPIs(executionContext, actualTagId);
                auditService.createOrganizationAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Collections.singletonMap(TAG, actualTagId))
                        .event(TAG_DELETED)
                        .createdAt(new Date())
                        .oldValue(null)
                        .newValue(tagOptional.get())
                        .build()
                );
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete tag " + key, ex);
        }
    }

    @Override
    public Set<String> findByUser(final String user, String referenceId, TagReferenceType referenceType) {
        final List<TagEntity> tags = findByReference(referenceId, referenceType);
        if (isEnvironmentAdmin()) {
            return tags.stream().map(TagEntity::getKey).collect(toSet());
        } else {
            final Set<String> restrictedTags = tags
                .stream()
                .filter(tag -> tag.getRestrictedGroups() != null && !tag.getRestrictedGroups().isEmpty())
                .map(TagEntity::getKey)
                .collect(toSet());

            final Set<String> groups = groupService.findByUser(user).stream().map(GroupEntity::getId).collect(toSet());

            return tags
                .stream()
                .filter(
                    tag ->
                        !restrictedTags.contains(tag.getKey()) ||
                        (tag.getRestrictedGroups() != null && anyMatch(tag.getRestrictedGroups(), groups))
                )
                .map(TagEntity::getKey)
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
        tag.setId(UUID.random().toString());
        tag.setKey(IdGenerator.generate(tagEntity.getKey()));
        tag.setName(tagEntity.getName());
        tag.setDescription(tagEntity.getDescription());
        tag.setRestrictedGroups(tagEntity.getRestrictedGroups());
        tag.setReferenceId(referenceId);
        tag.setReferenceType(repoTagReferenceType(referenceType));
        return tag;
    }

    private TagEntity convert(final Tag tag) {
        final TagEntity tagEntity = new TagEntity();
        tagEntity.setId(tag.getId());
        tagEntity.setKey(tag.getKey());
        tagEntity.setName(tag.getName());
        tagEntity.setDescription(tag.getDescription());
        tagEntity.setRestrictedGroups(tag.getRestrictedGroups());
        return tagEntity;
    }

    private io.gravitee.repository.management.model.TagReferenceType repoTagReferenceType(TagReferenceType referenceType) {
        return io.gravitee.repository.management.model.TagReferenceType.valueOf(referenceType.name());
    }
}
