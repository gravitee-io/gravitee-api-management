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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.infra.adapter.PortalPageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPageContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageCrudServiceImplTest {

    private PortalPageRepository pageRepository;
    private PortalPageContextRepository contextRepository;
    private PortalPageAdapter portalPageAdapter;
    private PortalPageCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PortalPageRepository.class);
        contextRepository = mock(PortalPageContextRepository.class);
        portalPageAdapter = mock(PortalPageAdapter.class);
        service = new PortalPageCrudServiceImpl(pageRepository, contextRepository, portalPageAdapter);
    }

    @Test
    void should_return_empty_list_when_no_contexts_found() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of());
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).isEmpty();
    }

    @Test
    void should_return_empty_list_when_no_pages_found() throws Exception {
        PortalPageContext ctx = new PortalPageContext();
        ctx.setPageId("page1");
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of(ctx));
        when(pageRepository.findById("page1")).thenReturn(Optional.empty());
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).isEmpty();
    }

    @Test
    void should_return_pages_for_contexts() throws Exception {
        PortalPageContext ctx1 = new PortalPageContext();
        ctx1.setPageId("page1");
        PortalPageContext ctx2 = new PortalPageContext();
        ctx2.setPageId("page2");
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of(ctx1, ctx2));
        io.gravitee.repository.management.model.PortalPage repoPage1 = mock(io.gravitee.repository.management.model.PortalPage.class);
        io.gravitee.repository.management.model.PortalPage repoPage2 = mock(io.gravitee.repository.management.model.PortalPage.class);
        when(pageRepository.findById("page1")).thenReturn(Optional.of(repoPage1));
        when(pageRepository.findById("page2")).thenReturn(Optional.of(repoPage2));
        PortalPage corePage1 = mock(PortalPage.class);
        PortalPage corePage2 = mock(PortalPage.class);
        when(portalPageAdapter.toEntity(repoPage1)).thenReturn(corePage1);
        when(portalPageAdapter.toEntity(repoPage2)).thenReturn(corePage2);
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).containsExactlyInAnyOrder(corePage1, corePage2);
    }

    @Test
    void should_handle_exception_and_return_empty_list() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenThrow(new TechnicalException("fail"));
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).isEmpty();
    }

    @Test
    void should_return_true_if_context_exists() throws Exception {
        PortalPageContext ctx = new PortalPageContext();
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of(ctx));
        assertThat(service.portalViewContextExists("env1", PortalViewContext.HOMEPAGE)).isTrue();
    }

    @Test
    void should_return_false_if_context_does_not_exist() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of());
        assertThat(service.portalViewContextExists("env1", PortalViewContext.HOMEPAGE)).isFalse();
    }

    @Test
    void should_return_false_if_exception_thrown_in_exists() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenThrow(new TechnicalException("fail"));
        assertThat(service.portalViewContextExists("env1", PortalViewContext.HOMEPAGE)).isFalse();
    }

    @Test
    void should_skip_pages_when_find_by_id_throws_technical_exception() throws Exception {
        PortalPageContext ctx1 = new PortalPageContext();
        ctx1.setPageId("page1");
        PortalPageContext ctx2 = new PortalPageContext();
        ctx2.setPageId("page2");
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of(ctx1, ctx2));
        when(pageRepository.findById("page1")).thenThrow(new TechnicalException());
        when(pageRepository.findById("page2")).thenReturn(Optional.empty());
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).isEmpty();
    }

    @Test
    void should_return_empty_list_when_find_all_by_context_type_returns_null() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(null);
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).isEmpty();
    }

    @Test
    void should_return_empty_list_when_find_all_by_context_type_returns_empty_list() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of());
        assertThat(service.byPortalViewContext("env1", PortalViewContext.HOMEPAGE)).isEmpty();
    }

    @Test
    void should_return_false_when_find_all_by_context_type_and_environment_id_returns_null() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(null);
        assertThat(service.portalViewContextExists("env1", PortalViewContext.HOMEPAGE)).isFalse();
    }

    @Test
    void should_return_false_when_find_all_by_context_type_and_environment_id_returns_empty() throws Exception {
        when(contextRepository.findAllByContextTypeAndEnvironmentId(any(), any())).thenReturn(List.of());
        assertThat(service.portalViewContextExists("env1", PortalViewContext.HOMEPAGE)).isFalse();
    }
}
