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
package io.gravitee.apim.infra.query_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentQueryServiceImplTest {

    @Mock
    PortalPageContentRepository repository;

    PortalPageContentQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PortalPageContentQueryServiceImpl(repository);
    }

    @Nested
    class FindById {

        @Test
        void should_return_portal_page_content_when_found() throws TechnicalException {
            // Given
            var contentId = "00000000-0000-0000-0000-000000000001";
            var repoContent = io.gravitee.repository.management.model.PortalPageContent.builder()
                .id(contentId)
                .organizationId("DEFAULT_ORG")
                .environmentId("DEFAULT_ENV")
                .type(io.gravitee.repository.management.model.PortalPageContent.Type.GRAVITEE_MARKDOWN)
                .content("# Welcome\n\nThis is a sample page content.")
                .build();

            when(repository.findById(contentId)).thenReturn(Optional.of(repoContent));

            // When
            var result = service.findById(PortalPageContentId.of(contentId));

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(GraviteeMarkdownPageContent.class);
            assertThat(result.get().getId()).isEqualTo(PortalPageContentId.of(contentId));
            assertThat(result.get().getOrganizationId()).isEqualTo("DEFAULT_ORG");
            assertThat(result.get().getEnvironmentId()).isEqualTo("DEFAULT_ENV");
            assertThat(((GraviteeMarkdownPageContent) result.get()).getContent().getRaw()).isEqualTo("# Welcome\n\nThis is a sample page content.");
        }

        @Test
        void should_return_empty_when_content_not_found() throws TechnicalException {
            // Given
            var contentId = "00000000-0000-0000-0000-000000000001";
            when(repository.findById(contentId)).thenReturn(Optional.empty());

            // When
            var result = service.findById(PortalPageContentId.of(contentId));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            // Given
            var contentId = "00000000-0000-0000-0000-000000000001";
            when(repository.findById(contentId)).thenThrow(new TechnicalException("Database error"));

            // When & Then
            assertThatThrownBy(() -> service.findById(PortalPageContentId.of(contentId)))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while finding portal page content by id 00000000-0000-0000-0000-000000000001")
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }
}
