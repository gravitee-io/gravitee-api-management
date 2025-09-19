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
package io.gravitee.apim.infra.crud_service.document;

import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.infra.crud_service.documentation.PageRevisionCrudServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.model.PageRevision;
import java.util.Date;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageRevisionCrudServiceImplTest {

    @Mock
    PageRevisionRepository pageRevisionRepository;

    @Captor
    ArgumentCaptor<PageRevision> pageRevisionArgumentCaptor;

    PageRevisionCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PageRevisionCrudServiceImpl(pageRevisionRepository);
    }

    @Nested
    class Create {

        @Test
        void should_create_a_page_revision() throws TechnicalException {
            var date = new Date();
            String PAGE_ID = "page-id";
            service.create(Page.builder().id(PAGE_ID).name("page name").createdAt(date).updatedAt(date).content("nice content").build());

            var expectedPageRevision = PageRevision.builder()
                .createdAt(date)
                .pageId(PAGE_ID)
                .name("page name")
                .content("nice content")
                .revision(1)
                .build();

            verify(pageRevisionRepository).create(pageRevisionArgumentCaptor.capture());
            Assertions.assertThat(pageRevisionArgumentCaptor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", expectedPageRevision.getName())
                .hasFieldOrPropertyWithValue("createdAt", expectedPageRevision.getCreatedAt())
                .hasFieldOrPropertyWithValue("pageId", expectedPageRevision.getPageId())
                .hasFieldOrPropertyWithValue("content", expectedPageRevision.getContent())
                .hasFieldOrPropertyWithValue("revision", expectedPageRevision.getRevision());

            Assertions.assertThat(pageRevisionArgumentCaptor.getValue().getHash()).isNotNull();
        }
    }
}
