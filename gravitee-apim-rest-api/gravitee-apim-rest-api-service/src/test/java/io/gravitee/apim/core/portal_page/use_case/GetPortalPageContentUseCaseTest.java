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
package io.gravitee.apim.core.portal_page.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalPageContentUseCaseTest {

    private static final PortalPageContentId CONTENT_ID = PortalPageContentId.of("00000000-0000-0000-0000-000000000001");

    private GetPortalPageContentUseCase useCase;

    @BeforeEach
    void setUp() {
        PortalPageContentQueryServiceInMemory queryService = new PortalPageContentQueryServiceInMemory();
        useCase = new GetPortalPageContentUseCase(queryService);

        queryService.initWith(PortalPageContentFixtures.samplePortalPageContents());
    }

    @Test
    void should_return_portal_page_content_when_found() {
        // Given
        // When
        var result = useCase.execute(new GetPortalPageContentUseCase.Input(CONTENT_ID));
        // Then
        assertThat(result.content()).isInstanceOf(GraviteeMarkdownPageContent.class);
        assertThat(result.content().getId()).isEqualTo(CONTENT_ID);
        assertThat(((GraviteeMarkdownPageContent) result.content()).getContent().getRaw()).isEqualTo("# Welcome\n\nThis is a sample page content.");
    }

    @Test
    void should_throw_when_content_not_found() {
        // Given
        var unknownId = PortalPageContentId.of("00000000-0000-0000-0000-000000000002");

        // When & Then
        assertThatThrownBy(() -> useCase.execute(new GetPortalPageContentUseCase.Input(unknownId)))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found")
            .extracting(ex -> ((PageContentNotFoundException) ex).getId())
            .isEqualTo(unknownId.json());
    }
}
