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

import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageQueryServiceImplTest {

    private PortalPageRepository pageRepository;
    private PortalPageContextRepository contextRepository;
    private PortalPageQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        pageRepository = Mockito.mock(PortalPageRepository.class);
        contextRepository = Mockito.mock(PortalPageContextRepository.class);
        queryService = new PortalPageQueryServiceImpl(pageRepository, contextRepository);
    }

    @Test
    void should_return_portal_pages_with_context_when_both_exist() throws Exception {
        String environmentId = "env-1";
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        PortalPage repoPage = PortalPage.builder().id(uuid).content("content").environmentId(environmentId).build();
        PortalPageContext repoContext = PortalPageContext
            .builder()
            .id(uuid)
            .pageId(uuid)
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(true)
            .build();
        Mockito
            .when(contextRepository.findAllByContextTypeAndEnvironmentId(PortalPageContextType.HOMEPAGE, environmentId))
            .thenReturn(List.of(repoContext));
        Mockito.when(pageRepository.findByIds(List.of(uuid))).thenReturn(List.of(repoPage));
        var result = queryService.findByEnvironmentIdAndContext(environmentId, PortalViewContext.HOMEPAGE);
        assertThat(result).hasSize(1);
        var page = result.getFirst().page();
        assertThat(page).isNotNull();
        assertThat(page.getPageContent().content()).isEqualTo("content");
        var context = result.getFirst().viewDetails();
        assertThat(context.context()).isEqualTo(PortalViewContext.HOMEPAGE);
        assertThat(context.published()).isTrue();
    }

    @Test
    void should_return_portal_page_with_context_when_found_by_id() throws Exception {
        String environmentId = "env-1";
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        PortalPage repoPage = PortalPage.builder().id(uuid).content("content").environmentId(environmentId).build();
        PortalPageContext repoContext = PortalPageContext
            .builder()
            .id(uuid)
            .pageId(uuid)
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(true)
            .build();

        Mockito.when(pageRepository.findById(uuid)).thenReturn(java.util.Optional.of(repoPage));
        Mockito.when(contextRepository.findByPageId(uuid)).thenReturn(repoContext);

        var result = queryService.findById(PageId.of(uuid));
        assertThat(result).isNotNull();
        var page = result.page();
        assertThat(page).isNotNull();
        assertThat(page.getPageContent().content()).isEqualTo("content");
        var context = result.viewDetails();
        assertThat(context.context()).isEqualTo(PortalViewContext.HOMEPAGE);
        assertThat(context.published()).isTrue();
    }

    @Test
    void should_throw_technical_management_exception_when_findById_fails() throws Exception {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";

        Mockito.when(pageRepository.findById(uuid)).thenThrow(new TechnicalException("boom"));

        Assertions.assertThrows(TechnicalManagementException.class, () -> queryService.findById(PageId.of(uuid)));
    }
}
