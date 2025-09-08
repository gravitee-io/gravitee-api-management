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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class PortalPageQueryServiceImplTest {

    @Mock
    private PortalPageRepository pageRepository;

    @Mock
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
        PortalPageContext repoContext = PortalPageContext.builder()
            .id(uuid)
            .pageId(uuid)
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(true)
            .build();
        Mockito.when(contextRepository.findAllByContextTypeAndEnvironmentId(PortalPageContextType.HOMEPAGE, environmentId)).thenReturn(
            List.of(repoContext)
        );
        Mockito.when(pageRepository.findByIdsWithExpand(eq(List.of(uuid)), any())).thenReturn(List.of(repoPage));
        var result = queryService.findByEnvironmentIdAndContext(
            environmentId,
            PortalViewContext.HOMEPAGE,
            List.of(ExpandsViewContext.CREATED_AT)
        );
        assertThat(result).hasSize(1);
        var page = result.getFirst().page();
        assertThat(page).isNotNull();
        assertThat(page.getPageContent().content()).isEqualTo("content");
        var context = result.getFirst().viewDetails();
        assertThat(context.context()).isEqualTo(PortalViewContext.HOMEPAGE);
        assertThat(context.published()).isTrue();
    }

    @Test
    void should_propagate_expanded_createdAt_and_updatedAt_fields_to_core_response() throws Exception {
        var service = new PortalPageQueryServiceImpl(pageRepository, contextRepository);
        var environmentId = "DEFAULT";

        // Given a context mapping one page id
        var ctx = PortalPageContext.builder()
            .pageId("123e4567-e89b-12d3-a456-426614174000")
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(true)
            .build();
        when(contextRepository.findAllByContextTypeAndEnvironmentId(eq(PortalPageContextType.HOMEPAGE), eq(environmentId))).thenReturn(
            List.of(ctx)
        );

        // And the repository returns the page with timestamps when expand is requested
        var created = new Date(1_000_000L);
        var updated = new Date(2_000_000L);
        var repoPage = PortalPage.builder()
            .id("123e4567-e89b-12d3-a456-426614174000")
            .content("hello")
            .createdAt(created)
            .updatedAt(updated)
            .build();
        when(pageRepository.findByIdsWithExpand(eq(List.of("123e4567-e89b-12d3-a456-426614174000")), any())).thenReturn(List.of(repoPage));

        var result = service.findByEnvironmentIdAndContext(
            environmentId,
            PortalViewContext.HOMEPAGE,
            List.of(ExpandsViewContext.CREATED_AT, ExpandsViewContext.UPDATED_AT)
        );

        assertThat(result).hasSize(1);
        var returned = result.getFirst();
        assertThat(returned.viewDetails().context()).isEqualTo(PortalViewContext.HOMEPAGE);
    }

    @Test
    void should_return_portal_page_with_context_when_found_by_id() throws Exception {
        String environmentId = "env-1";
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        PortalPage repoPage = PortalPage.builder().id(uuid).content("content").environmentId(environmentId).build();
        PortalPageContext repoContext = PortalPageContext.builder()
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
