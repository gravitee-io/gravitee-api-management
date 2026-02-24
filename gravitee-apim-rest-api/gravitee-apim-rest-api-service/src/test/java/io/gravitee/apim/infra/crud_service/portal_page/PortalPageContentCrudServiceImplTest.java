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
package io.gravitee.apim.infra.crud_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.infra.adapter.PortalPageContentAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalPageContent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentCrudServiceImplTest {

    private static final String ORGANIZATION_ID = "DEFAULT_ORG";
    private static final String ENVIRONMENT_ID = "DEFAULT_ENV";

    @Mock
    PortalPageContentRepository repository;

    PortalPageContentCrudServiceImpl service;

    PortalPageContentAdapter portalPageContentAdapter = PortalPageContentAdapter.INSTANCE;

    @Captor
    ArgumentCaptor<PortalPageContent> captor;

    @BeforeEach
    void setUp() {
        service = new PortalPageContentCrudServiceImpl(repository);
    }

    @Nested
    class CreatePageContent {

        @BeforeEach
        void setUp() throws TechnicalException {
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_create_a_page_content() throws TechnicalException {
            // Given
            final var pageContentId = PortalPageContentId.random();
            final var portalPageContent = new GraviteeMarkdownPageContent(
                pageContentId,
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                new io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContent("# Welcome\n\nThis is a sample page content.")
            );
            final var repoContent = portalPageContentAdapter.toRepository(portalPageContent);

            // When
            service.create(portalPageContent);

            // Then
            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(repoContent);
        }

        @Test
        void should_create_a_default_page_content() throws TechnicalException {
            String defaultContent;
            try {
                final var resource = new ClassPathResource("templates/default-portal-page-content.md");
                defaultContent = resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load default portal page template", e);
            }

            // When
            service.createDefault(ORGANIZATION_ID, ENVIRONMENT_ID);

            // Then
            verify(repository).create(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(PortalPageContent.Type.GRAVITEE_MARKDOWN);
            assertThat(captor.getValue().getContent()).isEqualTo(defaultContent);
            assertThat(captor.getValue().getOrganizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(captor.getValue().getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        }
    }

    @Nested
    class UpdatePageContent {

        @Test
        void should_update_a_page_content() throws TechnicalException {
            // Given
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            final var pageContentId = PortalPageContentId.random();
            final var portalPageContent = new GraviteeMarkdownPageContent(
                pageContentId,
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                new io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContent("# Updated Content\n\nThis is the updated content.")
            );
            final var repoContent = portalPageContentAdapter.toRepository(portalPageContent);

            // When
            service.update(portalPageContent);

            // Then
            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(repoContent);
        }
    }
}
