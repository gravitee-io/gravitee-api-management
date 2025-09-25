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
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.PortalPageContextCrudServiceInMemory;
import inmemory.PortalPageCrudServiceInMemory;
import inmemory.PortalPageQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.domain_service.CheckContextExistsDomainService;
import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageFactory;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalPageUseCaseTest {

    private PortalPageCrudServiceInMemory portalPageCrudService;
    private PortalPageContextCrudServiceInMemory portalPageContextCrudService;
    private PortalPageQueryServiceInMemory portalPageQueryService;
    private GetPortalPageUseCase useCase;

    @BeforeEach
    void setUp() {
        portalPageCrudService = new PortalPageCrudServiceInMemory();
        portalPageContextCrudService = new PortalPageContextCrudServiceInMemory();
        portalPageQueryService = new PortalPageQueryServiceInMemory();
        CheckContextExistsDomainService checkContextExistsDomainService = new CheckContextExistsDomainService(portalPageContextCrudService);

        useCase = new GetPortalPageUseCase(checkContextExistsDomainService, portalPageQueryService);
    }

    @Test
    void should_return_first_page_for_given_context_without_published_filter() {
        PageId pageId1 = PageId.random();
        PageId pageId2 = PageId.random();
        PortalPage page1 = PortalPageFactory.create(pageId1, new GraviteeMarkdown("content1"));
        PortalPage page2 = PortalPageFactory.create(pageId2, new GraviteeMarkdown("content2"));
        portalPageCrudService.initWith(List.of(page1, page2));

        portalPageQueryService.initWith(
            List.of(
                new PortalPageWithViewDetails(page1, new PortalPageView(PortalViewContext.HOMEPAGE, true)),
                new PortalPageWithViewDetails(page2, new PortalPageView(PortalViewContext.HOMEPAGE, false))
            )
        );

        String environmentId = "DEFAULT";
        var ctx = PortalPageContext.builder()
            .pageId(pageId1.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .build();
        portalPageContextCrudService.initWith(List.of(ctx));

        var output = useCase.execute(
            new GetPortalPageUseCase.Input(environmentId, PortalViewContext.HOMEPAGE, Collections.singletonList(ExpandsViewContext.CONTENT))
        );

        assertThat(output.pages()).isNotNull();
        assertThat(output.pages().getFirst().page()).isEqualTo(page1);
        assertThat(output.pages().getFirst().viewDetails().context()).isEqualTo(PortalViewContext.HOMEPAGE);
    }

    @Test
    void should_filter_by_published_true() {
        PageId pageId1 = PageId.random();
        PageId pageId2 = PageId.random();
        PortalPage page1 = PortalPageFactory.create(pageId1, new GraviteeMarkdown("content1"));
        PortalPage page2 = PortalPageFactory.create(pageId2, new GraviteeMarkdown("content2"));
        portalPageCrudService.initWith(List.of(page1, page2));

        portalPageQueryService.initWith(
            List.of(
                new PortalPageWithViewDetails(page1, new PortalPageView(PortalViewContext.HOMEPAGE, false)),
                new PortalPageWithViewDetails(page2, new PortalPageView(PortalViewContext.HOMEPAGE, true))
            )
        );

        String environmentId = "DEFAULT";
        var ctx = PortalPageContext.builder()
            .pageId(pageId2.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .build();
        portalPageContextCrudService.initWith(List.of(ctx));

        var output = useCase.execute(
            new GetPortalPageUseCase.Input(environmentId, PortalViewContext.HOMEPAGE, Collections.singletonList(ExpandsViewContext.CONTENT))
        );

        assertThat(output.pages().getFirst().page()).isEqualTo(page1);
        assertThat(output.pages().getFirst().viewDetails().published()).isFalse();
    }

    @Test
    void should_select_published_page_when_multiple_candidates() {
        PageId pageId1 = PageId.random();
        PageId pageId2 = PageId.random();
        PortalPage page1 = PortalPageFactory.create(pageId1, new GraviteeMarkdown("content1"));
        PortalPage page2 = PortalPageFactory.create(pageId2, new GraviteeMarkdown("content2"));
        portalPageCrudService.initWith(List.of(page1, page2));

        portalPageQueryService.initWith(
            List.of(
                new PortalPageWithViewDetails(page1, new PortalPageView(PortalViewContext.HOMEPAGE, false)),
                new PortalPageWithViewDetails(page2, new PortalPageView(PortalViewContext.HOMEPAGE, true))
            )
        );

        String environmentId = "DEFAULT";
        var ctx = PortalPageContext.builder()
            .pageId(pageId1.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .build();
        portalPageContextCrudService.initWith(List.of(ctx));

        var output = useCase.execute(
            new GetPortalPageUseCase.Input(environmentId, PortalViewContext.HOMEPAGE, Collections.singletonList(ExpandsViewContext.CONTENT))
        );

        assertThat(output.pages().getFirst().page()).isEqualTo(page1);
        assertThat(output.pages().getFirst().viewDetails().published()).isFalse();
    }

    @Test
    void should_fail_when_context_does_not_exist() {
        assertThrows(Exception.class, () ->
            useCase.execute(
                new GetPortalPageUseCase.Input("DEFAULT", PortalViewContext.HOMEPAGE, Collections.singletonList(ExpandsViewContext.CONTENT))
            )
        );
    }

    @Test
    void should_fail_when_invalid_page_type() {
        String environmentId = "DEFAULT";
        portalPageContextCrudService.initWith(List.of());
        var dummyPageId = PageId.random();
        var ctx = PortalPageContext.builder()
            .pageId(dummyPageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .build();
        portalPageContextCrudService.initWith(List.of(ctx));

        assertThrows(IllegalArgumentException.class, () ->
            useCase.execute(
                new GetPortalPageUseCase.Input(
                    environmentId,
                    PortalViewContext.valueOf("UNKNOWN"),
                    Collections.singletonList(ExpandsViewContext.CONTENT)
                )
            )
        );
    }
}
