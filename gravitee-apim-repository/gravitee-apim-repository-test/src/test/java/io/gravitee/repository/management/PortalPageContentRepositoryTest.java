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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalPageContent;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalPageContentRepositoryTest extends AbstractManagementRepositoryTest {

    @Inject
    protected PortalPageContentRepository portalPageContentRepository;

    @Override
    protected String getTestCasesPath() {
        return "/data/portalpagecontent-tests/";
    }

    @Test
    public void should_find_all_page_contents_for_type() throws Exception {
        List<PortalPageContent> contents = portalPageContentRepository.findAllByType(PortalPageContent.Type.GRAVITEE_MARKDOWN);

        assertThat(contents).isNotNull();
        assertThat(contents).hasSize(2);
        assertThat(contents).anyMatch(c -> "page-content-1".equals(c.getId()));
        assertThat(contents).anyMatch(c -> "page-content-2".equals(c.getId()));
    }

    @Test
    public void should_create_and_delete_page_content() throws Exception {
        PortalPageContent content = PortalPageContent.builder()
            .id("new-page-content")
            .type(PortalPageContent.Type.GRAVITEE_MARKDOWN)
            .configuration("{ \"pageId\": \"contact\" }")
            .content("# Contact us")
            .build();

        PortalPageContent created = portalPageContentRepository.create(content);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(content.getId());

        portalPageContentRepository.delete(content.getId());
        var maybeFound = portalPageContentRepository.findById(content.getId());
        assertThat(maybeFound.isPresent()).isFalse();
    }

    @Test
    public void should_update_page_content() throws Exception {
        PortalPageContent existing = portalPageContentRepository.findById("page-content-1").orElse(null);
        assertThat(existing).isNotNull();

        PortalPageContent toUpdate = PortalPageContent.builder()
            .id(existing.getId())
            .type(existing.getType())
            .configuration(existing.getConfiguration())
            .content("# Updated content")
            .build();

        portalPageContentRepository.update(toUpdate);

        PortalPageContent updated = portalPageContentRepository.findById(existing.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getContent()).isEqualTo("# Updated content");
    }

    @Test
    public void should_delete_all_by_type() throws Exception {
        // Ensure there are items of the type
        List<PortalPageContent> contentsBefore = portalPageContentRepository.findAllByType(PortalPageContent.Type.GRAVITEE_MARKDOWN);
        assertThat(contentsBefore).isNotNull();
        assertThat(contentsBefore).isNotEmpty();

        portalPageContentRepository.deleteByType(PortalPageContent.Type.GRAVITEE_MARKDOWN);

        Set<PortalPageContent> contentsAfter = portalPageContentRepository.findAll();
        assertThat(contentsAfter).isNotNull();
        assertThat(contentsAfter).doesNotContainAnyElementsOf(contentsBefore);
    }

    @Test
    public void should_find_all_page_contents() throws Exception {
        Set<PortalPageContent> all = portalPageContentRepository.findAll();

        assertThat(all).isNotNull();
        assertThat(all.size()).isGreaterThanOrEqualTo(2);
        assertThat(all).anyMatch(c -> "page-content-1".equals(c.getId()));
        assertThat(all).anyMatch(c -> "page-content-2".equals(c.getId()));
    }
}
