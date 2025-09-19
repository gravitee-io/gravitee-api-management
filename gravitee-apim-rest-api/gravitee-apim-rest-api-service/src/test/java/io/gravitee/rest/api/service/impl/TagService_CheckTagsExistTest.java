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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.exceptions.TagNotFoundException;
import io.gravitee.rest.api.service.v4.ApiTagService;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TagService_CheckTagsExistTest {

    private static final String ORG_ID = "DEFAULT";

    @InjectMocks
    private TagServiceImpl tagService = new TagServiceImpl();

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ApiTagService apiTagService;

    @Mock
    private AuditService auditService;

    @Mock
    private GroupService groupService;

    @BeforeEach
    public void init() {
        reset(tagRepository);
    }

    @Test
    public void should_not_throw_exception_if_empty_tag_ids_parameter() throws TechnicalException {
        when(tagRepository.findByIdsAndReference(new HashSet<>(), ORG_ID, TagReferenceType.ORGANIZATION)).thenReturn(Set.of());
        tagService.checkTagsExist(new HashSet<>(), ORG_ID, io.gravitee.rest.api.model.TagReferenceType.ORGANIZATION);
    }

    @Test
    public void should_throw_error_if_no_tags_found() throws TechnicalException {
        when(tagRepository.findByIdsAndReference(Set.of("tag-1", "tag-2"), ORG_ID, TagReferenceType.ORGANIZATION)).thenReturn(Set.of());
        assertThrows(TagNotFoundException.class, () ->
            tagService.checkTagsExist(Set.of("tag-1", "tag-2"), ORG_ID, io.gravitee.rest.api.model.TagReferenceType.ORGANIZATION)
        );
    }

    @Test
    public void should_throw_error_if_only_one_tag_found() throws TechnicalException {
        Tag tag1 = getRepositoryTag("tag-1");
        Set<String> tags = new HashSet<>();
        tags.add("tag-1");
        tags.add("tag-2");

        when(tagRepository.findByIdsAndReference(tags, ORG_ID, TagReferenceType.ORGANIZATION)).thenReturn(Set.of(tag1));

        assertThrows(TagNotFoundException.class, () ->
            tagService.checkTagsExist(tags, ORG_ID, io.gravitee.rest.api.model.TagReferenceType.ORGANIZATION)
        );

        verify(tagRepository, times(1)).findByIdsAndReference(any(), any(), any());
    }

    @Test
    public void should_return_tag_entities_when_all_tags_valid() throws TechnicalException {
        Tag tag1 = getRepositoryTag("tag-1");
        Tag tag2 = getRepositoryTag("tag-2");

        when(tagRepository.findByIdsAndReference(Set.of("tag-1", "tag-2"), ORG_ID, TagReferenceType.ORGANIZATION)).thenReturn(
            Set.of(tag1, tag2)
        );

        tagService.checkTagsExist(Set.of("tag-1", "tag-2"), ORG_ID, io.gravitee.rest.api.model.TagReferenceType.ORGANIZATION);

        verify(tagRepository, times(1)).findByIdsAndReference(any(), any(), any());
    }

    private Tag getRepositoryTag(String id) {
        var tag = new Tag();
        tag.setId(id);
        tag.setName(id);
        tag.setReferenceType(TagReferenceType.ORGANIZATION);
        tag.setReferenceId(ORG_ID);

        return tag;
    }
}
