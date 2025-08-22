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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.rest.api.service.exceptions.PortalPageNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PortalPageCrudServiceImplTest {

    @Mock
    private PortalPageRepository portalPageRepository;

    private PortalPageCrudService portalPageCrudService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        portalPageCrudService = new PortalPageCrudServiceImpl(portalPageRepository);
    }

    @Test
    void shouldCreatePortalPage() throws Exception {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("test content"));
        var entity = mock(io.gravitee.repository.management.model.PortalPage.class);
        when(portalPageRepository.create(any())).thenReturn(entity);
        when(entity.getId()).thenReturn(page.id().id().toString());
        when(entity.getContent()).thenReturn("test content");
        when(entity.getContexts()).thenReturn(null);

        PortalPage result = portalPageCrudService.create(page);
        assertThat(result.pageContent().content()).isEqualTo("test content");
    }

    @Test
    void shouldFindPortalPageById() throws Exception {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("find content"));
        var entity = mock(io.gravitee.repository.management.model.PortalPage.class);
        when(portalPageRepository.findById(page.id().id().toString())).thenReturn(java.util.Optional.of(entity));
        when(entity.getId()).thenReturn(page.id().id().toString());
        when(entity.getContent()).thenReturn("find content");
        when(entity.getContexts()).thenReturn(null);

        PortalPage result = portalPageCrudService.getById(page.id());
        assertThat(result.pageContent().content()).isEqualTo("find content");
    }

    @Test
    void shouldAssignContextToPortalPage() throws Exception {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("context content"));
        PortalViewContext context = PortalViewContext.HOMEPAGE;
        var entity = mock(io.gravitee.repository.management.model.PortalPage.class);
        when(portalPageRepository.findById(page.id().id().toString())).thenReturn(java.util.Optional.of(entity));
        when(entity.getId()).thenReturn(page.id().id().toString());
        when(entity.getContent()).thenReturn("context content");
        when(entity.getContexts()).thenReturn(List.of("HOMEPAGE"));

        PortalPage result = portalPageCrudService.setPortalViewContextPage(context, page);
        assertThat(result.pageContent().content()).isEqualTo("context content");
    }

    @Test
    void shouldThrowNotFoundException() throws Exception {
        PageId pageId = PageId.random();
        when(portalPageRepository.findById(pageId.id().toString())).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> portalPageCrudService.getById(pageId))
            .isInstanceOf(PortalPageNotFoundException.class)
            .hasMessageContaining(pageId.id().toString());
    }

    @Test
    void shouldThrowTechnicalDomainException() throws Exception {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("fail content"));
        when(portalPageRepository.create(any())).thenThrow(new TechnicalException("error"));
        assertThatThrownBy(() -> portalPageCrudService.create(page))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("error");
    }
}
