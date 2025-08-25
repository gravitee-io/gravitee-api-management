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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPage;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalPageRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portal-page-tests/";
    }

    @Test
    public void should_find_all() throws TechnicalException {
        var pages = portalPageRepository.findAll();
        assertThat(pages).isNotNull().hasSize(1);
        assertThat(pages).allMatch(p -> "test-page-id".equals(p.getId()));
    }

    @Test
    public void should_assign_and_remove_context() throws TechnicalException {
        String pageId = "test-page-id";
        String context = "test-context";
        portalPageRepository.assignContext(pageId, context);
        var pagesWithContext = portalPageRepository.findByContext(context);
        assertThat(pagesWithContext).isNotNull();
        assertThat(pagesWithContext.stream().anyMatch(p -> pageId.equals(p.getId()))).isTrue();

        portalPageRepository.removeContext(pageId, context);
        var pagesAfterRemove = portalPageRepository.findByContext(context);
        assertThat(pagesAfterRemove.stream().noneMatch(p -> pageId.equals(p.getId()))).isTrue();
    }

    @Test
    public void shouldCreatePortalPage() throws TechnicalException {
        PortalPage page = new PortalPage();
        page.setId("new-page-id");
        page.setContent("new content");
        page.setContexts(java.util.List.of("ctx1", "ctx2"));
        portalPageRepository.create(page);
        var foundOpt = portalPageRepository.findById("new-page-id");
        assertThat(foundOpt).isPresent();
        PortalPage found = foundOpt.get();
        assertThat(found.getId()).isEqualTo("new-page-id");
        assertThat(found.getContent()).isEqualTo("new content");
        assertThat(found.getContexts()).containsExactlyInAnyOrder("ctx1", "ctx2");
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        var pageOpt = portalPageRepository.findById("test-page-id");
        assertThat(pageOpt).isPresent();
        PortalPage page = pageOpt.get();
        assertThat(page.getId()).isEqualTo("test-page-id");
        assertThat(page.getContent()).isEqualTo("Sample content");
    }

    @Test
    public void shouldUpdatePortalPage() throws TechnicalException {
        var pageOpt = portalPageRepository.findById("test-page-id");
        assertThat(pageOpt).isPresent();
        PortalPage page = pageOpt.get();
        page.setContent("updated content");
        page.setContexts(java.util.List.of("ctx3"));
        portalPageRepository.update(page);
        var updatedOpt = portalPageRepository.findById("test-page-id");
        assertThat(updatedOpt).isPresent();
        PortalPage updated = updatedOpt.get();
        assertThat(updated.getContent()).isEqualTo("updated content");
        assertThat(updated.getContexts()).isEqualTo(java.util.List.of("ctx3"));
    }

    @Test
    public void shouldDeletePortalPage() throws TechnicalException {
        portalPageRepository.delete("test-page-id");
        var deletedOpt = portalPageRepository.findById("test-page-id");
        assertThat(deletedOpt).isNotPresent();
    }
}
