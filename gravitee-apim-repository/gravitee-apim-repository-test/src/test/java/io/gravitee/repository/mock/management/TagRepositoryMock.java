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
package io.gravitee.repository.mock.management;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TagRepositoryMock extends AbstractRepositoryMock<TagRepository> {

    public TagRepositoryMock() {
        super(TagRepository.class);
    }

    @Override
    protected void prepare(TagRepository tagRepository) throws Exception {
        final Tag tag = mock(Tag.class);
        when(tag.getName()).thenReturn("Tag name");
        when(tag.getDescription()).thenReturn("Description for the new tag");
        when(tag.getRestrictedGroups()).thenReturn(asList("g1", "groupNew"));
        when(tag.getReferenceId()).thenReturn("DEFAULT");
        when(tag.getReferenceType()).thenReturn(TagReferenceType.ORGANIZATION);

        final Tag tag2 = mock(Tag.class);
        when(tag2.getId()).thenReturn("products");
        when(tag2.getName()).thenReturn("Products");
        when(tag2.getDescription()).thenReturn("Description for products tag");
        when(tag2.getRestrictedGroups()).thenReturn(asList("group1", "group2"));
        when(tag2.getReferenceId()).thenReturn("DEFAULT");
        when(tag2.getReferenceType()).thenReturn(TagReferenceType.ORGANIZATION);

        final Tag tag2Updated = mock(Tag.class);
        when(tag2Updated.getName()).thenReturn("New product");
        when(tag2Updated.getDescription()).thenReturn("New description");
        when(tag2Updated.getRestrictedGroups()).thenReturn(singletonList("group"));
        when(tag2Updated.getReferenceId()).thenReturn("DEFAULT");
        when(tag2Updated.getReferenceType()).thenReturn(TagReferenceType.ORGANIZATION);

        final Set<Tag> tags = newSet(tag, tag2, mock(Tag.class));
        final Set<Tag> tagsAfterDelete = newSet(tag, tag2);
        final Set<Tag> tagsAfterAdd = newSet(tag, tag2, mock(Tag.class), mock(Tag.class));

        when(tagRepository.findByReference("DEFAULT", TagReferenceType.ORGANIZATION))
            .thenReturn(tags, tagsAfterAdd, tags, tagsAfterDelete, tags);

        when(tagRepository.create(any(Tag.class))).thenReturn(tag);

        when(tagRepository.findById("new-tag")).thenReturn(of(tag));
        when(tagRepository.findById("products")).thenReturn(of(tag2), of(tag2Updated));

        when(tagRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        final Tag tag3 = mock(Tag.class);
        when(tag3.getName()).thenReturn("Other");
        when(tag3.getDescription()).thenReturn("Description for other tag");
        when(tag3.getRestrictedGroups()).thenReturn(emptyList());
        when(tag3.getReferenceId()).thenReturn("OTHER");
        when(tag3.getReferenceType()).thenReturn(TagReferenceType.ORGANIZATION);

        when(tagRepository.findByIdAndReference("other", "OTHER", TagReferenceType.ORGANIZATION)).thenReturn(Optional.of(tag3));
    }
}
