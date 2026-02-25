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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalPageContentFixtures;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentMapperTest {

    private PortalPageContentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = PortalPageContentMapper.INSTANCE;
    }

    @Test
    void should_map_gravitee_markdown_page_content() {
        // Given
        var contentId = PortalPageContentId.of("12345678-1234-1234-1234-123456789abc");
        var markdownContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            contentId,
            "ORG",
            "ENV",
            "# Welcome\n\nThis is a test page."
        );

        // When
        var result = mapper.map(markdownContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("12345678-1234-1234-1234-123456789abc");
        assertThat(result.getContent()).isEqualTo("# Welcome\n\nThis is a test page.");
        assertThat(result.getType()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
    }

    @Test
    void should_map_portal_page_content_id_to_string() {
        // Given
        var contentId = PortalPageContentId.of("abcd1234-5678-9012-3456-789012345678");

        // When
        var result = mapper.map(contentId);

        // Then
        assertThat(result).isEqualTo("abcd1234-5678-9012-3456-789012345678");
    }

    @Test
    void should_map_portal_page_content_polymorphically() {
        // Given
        var contentId = PortalPageContentId.random();
        var markdownContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(contentId, "ORG", "ENV", "Test content");

        // When
        var result = mapper.map((io.gravitee.apim.core.portal_page.model.PortalPageContent) markdownContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(contentId.json());
        assertThat(result.getContent()).isEqualTo("Test content");
        assertThat(result.getType()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
    }
}
