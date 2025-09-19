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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContextCrudServiceImplTest {

    private PortalPageContextRepository contextRepository;
    private PortalPageContextCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        contextRepository = mock(PortalPageContextRepository.class);
        service = new PortalPageContextCrudServiceImpl(contextRepository);
    }

    @Test
    void should_return_page_ids_when_contexts_found() throws TechnicalException {
        String environmentId = "env-1";
        String id1 = "11111111-1111-1111-1111-111111111111";
        String id2 = "22222222-2222-2222-2222-222222222222";

        var ctx1 = PortalPageContext.builder()
            .id(id1)
            .pageId(id1)
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(true)
            .build();
        var ctx2 = PortalPageContext.builder()
            .id(id2)
            .pageId(id2)
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(false)
            .build();

        when(contextRepository.findAllByContextTypeAndEnvironmentId(PortalPageContextType.HOMEPAGE, environmentId)).thenReturn(
            List.of(ctx1, ctx2)
        );

        var result = service.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, environmentId);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(PageId.of(id1), PageId.of(id2));
    }

    @Test
    void should_throw_technical_domain_exception_when_repository_fails() throws TechnicalException {
        String environmentId = "env-1";

        when(contextRepository.findAllByContextTypeAndEnvironmentId(PortalPageContextType.HOMEPAGE, environmentId)).thenThrow(
            new TechnicalException("boom")
        );

        assertThrows(TechnicalDomainException.class, () ->
            service.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, environmentId)
        );
    }

    @Test
    void should_return_portal_page_view_when_found_by_page_id() {
        String id = "33333333-3333-3333-3333-333333333333";
        var ctx = PortalPageContext.builder()
            .id(id)
            .pageId(id)
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId("env-1")
            .published(true)
            .build();

        when(contextRepository.findByPageId(id)).thenReturn(ctx);

        var view = service.findByPageId(PageId.of(id));
        assertThat(view).isNotNull();
        assertThat(view.context()).isEqualTo(PortalViewContext.HOMEPAGE);
        assertThat(view.published()).isTrue();
    }

    @Test
    void should_return_null_when_no_context_found_for_page_id() {
        String id = "44444444-4444-4444-4444-444444444444";

        when(contextRepository.findByPageId(id)).thenReturn(null);

        var view = service.findByPageId(PageId.of(id));
        assertThat(view).isNull();
    }
}
