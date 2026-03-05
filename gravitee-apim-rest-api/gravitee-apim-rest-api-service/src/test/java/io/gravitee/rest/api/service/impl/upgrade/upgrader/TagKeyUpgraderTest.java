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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.Tag;
import io.gravitee.repository.management.model.TagReferenceType;
import java.util.List;
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
class TagKeyUpgraderTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagKeyUpgrader upgrader;

    @Test
    void shouldMigrateTags() throws Exception {
        var tag = Tag.builder()
            .id("tag-key")
            .key(null)
            .name("Tag Name")
            .description("description")
            .restrictedGroups(List.of())
            .referenceId("DEFAULT")
            .referenceType(TagReferenceType.ORGANIZATION)
            .build();

        when(tagRepository.findAll()).thenReturn(Set.of(tag));
        assertThat(upgrader.upgrade()).isTrue();

        var tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).create(tagCaptor.capture());
        verify(tagRepository).delete("tag-key");

        var migratedTag = tagCaptor.getValue();
        assertThat(migratedTag.getKey()).isEqualTo("tag-key");
        assertThat(migratedTag.getId()).isNotEqualTo("tag-key").matches("^[0-9a-fA-F-]{36}$");
        assertThat(migratedTag.getName()).isEqualTo("Tag Name");
    }

    @Test
    void shouldReturnFalseOnException() throws Exception {
        when(tagRepository.findAll()).thenThrow(new RuntimeException("error"));
        assertThat(upgrader.upgrade()).isFalse();
    }
}
