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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.rest.api.model.NewTagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DuplicateTagKeyException;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class TagServiceTest {

    private static final String REFERENCE_ID = "DEFAULT";
    private static final TagReferenceType REFERENCE_TYPE = TagReferenceType.ORGANIZATION;

    @InjectMocks
    private TagServiceImpl tagService = new TagServiceImpl();

    @Mock
    private TagRepository tagRepository;

    @Mock
    private AuditService auditService;

    @Test
    public void should_create_tag_when_key_does_not_exist() throws TechnicalException {
        var newKey = "new-tag-key";
        var newTag = new NewTagEntity();
        newTag.setKey(newKey);
        newTag.setName("New Tag Name");
        newTag.setDescription("New Tag Description");

        var executionContext = new ExecutionContext(REFERENCE_ID, null);

        when(tagRepository.findByReference(REFERENCE_ID, io.gravitee.repository.management.model.TagReferenceType.ORGANIZATION)).thenReturn(
            Collections.emptySet()
        );

        var savedTag = new Tag();
        savedTag.setId("new-id");
        savedTag.setKey(newKey);
        savedTag.setName(newTag.getName());
        savedTag.setReferenceId(REFERENCE_ID);
        savedTag.setReferenceType(io.gravitee.repository.management.model.TagReferenceType.ORGANIZATION);

        when(tagRepository.create(any())).thenReturn(savedTag);

        var result = tagService.create(executionContext, newTag, REFERENCE_ID, REFERENCE_TYPE);

        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(newKey);
        assertThat(result.getName()).isEqualTo(newTag.getName());

        var tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).create(tagCaptor.capture());
        var tagToCreate = tagCaptor.getValue();
        assertThat(tagToCreate.getKey()).isEqualTo(newKey);
        assertThat(tagToCreate.getName()).isEqualTo(newTag.getName());
        assertThat(tagToCreate.getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(tagToCreate.getReferenceType()).isEqualTo(io.gravitee.repository.management.model.TagReferenceType.ORGANIZATION);

        verify(auditService).createOrganizationAuditLog(eq(executionContext), any());
    }

    @Test
    public void should_throw_DuplicateTagKeyException_when_key_already_exists() throws TechnicalException {
        var existingKey = "existing-tag-key";
        var newTag = new NewTagEntity();
        newTag.setKey(existingKey);
        newTag.setName("New Tag Name");

        var existingTag = new Tag();
        existingTag.setId("existing-id");
        existingTag.setKey(existingKey);
        existingTag.setReferenceId(REFERENCE_ID);
        existingTag.setReferenceType(io.gravitee.repository.management.model.TagReferenceType.ORGANIZATION);

        when(tagRepository.findByReference(REFERENCE_ID, io.gravitee.repository.management.model.TagReferenceType.ORGANIZATION)).thenReturn(
            Set.of(existingTag)
        );

        assertThatThrownBy(() ->
            tagService.create(new ExecutionContext(REFERENCE_ID, null), newTag, REFERENCE_ID, REFERENCE_TYPE)
        ).isInstanceOf(DuplicateTagKeyException.class);

        verify(tagRepository, never()).create(any());
        verifyNoInteractions(auditService);
    }
}
