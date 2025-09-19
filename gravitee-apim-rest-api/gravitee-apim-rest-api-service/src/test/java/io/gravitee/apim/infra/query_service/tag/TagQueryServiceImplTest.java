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
package io.gravitee.apim.infra.query_service.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.repository.management.api.TagRepository;
import io.gravitee.repository.management.model.TagReferenceType;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TagQueryServiceImplTest {

    TagRepository tagRepository;
    TagQueryServiceImpl service;

    private static final String ORGANIZATION_ID = "organization-id";

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        service = new TagQueryServiceImpl(tagRepository);
    }

    @Nested
    class FindByName {

        @Test
        @SneakyThrows
        void should_find_groups_matching_the_event_provided() {
            when(tagRepository.findByReference(any(String.class), eq(TagReferenceType.ORGANIZATION))).thenAnswer(invocation ->
                Set.of(
                    aTag("1").referenceId(invocation.getArgument(0)).name("tag-1").build(),
                    aTag("2").referenceId(invocation.getArgument(0)).name("tag-2").build()
                )
            );

            var tags = service.findByName(ORGANIZATION_ID, "tag-2");

            Assertions.assertThat(tags).hasSize(1).extracting(Tag::getId).containsExactly("2");
        }

        @Test
        @SneakyThrows
        void should_adapt_groups() {
            when(tagRepository.findByReference(any(String.class), eq(TagReferenceType.ORGANIZATION))).thenAnswer(invocation ->
                Set.of(aTag("1").referenceId(invocation.getArgument(0)).name("tag-1").build())
            );

            var tags = service.findByName(ORGANIZATION_ID, "tag-1");

            Assertions.assertThat(tags)
                .hasSize(1)
                .containsExactly(
                    Tag.builder()
                        .id("1")
                        .name("tag-1")
                        .description("group-1-description")
                        .restrictedGroups(List.of("group-1-restricted-group"))
                        .referenceId(ORGANIZATION_ID)
                        .referenceType(io.gravitee.apim.core.tag.model.Tag.TagReferenceType.ORGANIZATION)
                        .build()
                );
        }
    }

    private io.gravitee.repository.management.model.Tag.TagBuilder aTag(String id) {
        return io.gravitee.repository.management.model.Tag.builder()
            .id(id)
            .name("group-1")
            .description("group-1-description")
            .restrictedGroups(List.of("group-1-restricted-group"))
            .referenceId(ORGANIZATION_ID)
            .referenceType(TagReferenceType.ORGANIZATION);
    }
}
