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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageCrudServiceImplTest {

    private PortalPageRepository pageRepository;
    private PortalPageCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PortalPageRepository.class);
        service = new PortalPageCrudServiceImpl(pageRepository);
    }

    @Test
    void should_return_empty_list_when_no_ids_provided() {
        assertThat(service.findByIds(List.of())).isEmpty();
    }

    @Test
    void should_return_pages_when_ids_provided() {
        PageId pageId1 = PageId.random();
        var page1 = new PortalPage(pageId1, new GraviteeMarkdown("content1"));
        PageId pageId2 = PageId.random();
        var page2 = new PortalPage(pageId2, new GraviteeMarkdown("content2"));

        when(pageRepository.findByIds(List.of(pageId1.toString(), pageId2.toString()))).thenReturn(
            List.of(
                io.gravitee.repository.management.model.PortalPage.builder()
                    .id(pageId1.toString())
                    .content(page1.getPageContent().content())
                    .build(),
                io.gravitee.repository.management.model.PortalPage.builder()
                    .id(pageId2.toString())
                    .content(page2.getPageContent().content())
                    .build()
            )
        );

        var pages = service.findByIds(List.of(pageId1, pageId2));
        assertThat(pages).hasSize(2).containsExactlyInAnyOrder(page1, page2);
    }

    @Test
    void should_update_page_successfully() throws TechnicalException {
        PageId pageId = PageId.random();
        var page = new PortalPage(pageId, new GraviteeMarkdown("updated-content"));

        var repoPage = io.gravitee.repository.management.model.PortalPage.builder()
            .id(pageId.toString())
            .content(page.getPageContent().content())
            .build();

        doReturn(repoPage).when(pageRepository).update(any());

        var updated = service.update(page);
        assertThat(updated).isEqualTo(page);
    }

    @Test
    void should_throw_technical_domain_exception_when_update_fails() throws TechnicalException {
        PageId pageId = PageId.random();
        var page = new PortalPage(pageId, new GraviteeMarkdown("content-error"));

        doThrow(new TechnicalException("boom")).when(pageRepository).update(any());

        org.junit.jupiter.api.Assertions.assertThrows(TechnicalDomainException.class, () -> service.update(page));
    }

    @Test
    void should_return_page_when_found_by_id() throws TechnicalException {
        PageId pageId = PageId.random();
        var page = new PortalPage(pageId, new GraviteeMarkdown("content-find"));

        var repoPage = io.gravitee.repository.management.model.PortalPage.builder()
            .id(pageId.toString())
            .content(page.getPageContent().content())
            .build();

        doReturn(Optional.of(repoPage)).when(pageRepository).findById(pageId.toString());

        var found = service.findById(pageId);
        assertThat(found).isPresent().contains(page);
    }

    @Test
    void should_throw_technical_domain_exception_when_findById_fails() throws TechnicalException {
        PageId pageId = PageId.random();

        doThrow(new TechnicalException("boom")).when(pageRepository).findById(pageId.toString());

        org.junit.jupiter.api.Assertions.assertThrows(TechnicalDomainException.class, () -> service.findById(pageId));
    }
}
