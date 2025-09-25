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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.PortalPageContextCrudServiceInMemory;
import inmemory.PortalPageQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalPageViewPublicationStatusUseCaseTest {

    private final PortalPageContextCrudServiceInMemory portalPageContextCrudService = new PortalPageContextCrudServiceInMemory();

    private final PortalPageQueryServiceInMemory portalPageQueryService = new PortalPageQueryServiceInMemory();

    private UpdatePortalPageViewPublicationStatusUseCase cut;

    @BeforeEach
    void setUp() {
        portalPageContextCrudService.reset();
        portalPageQueryService.reset();
        cut = new UpdatePortalPageViewPublicationStatusUseCase(portalPageContextCrudService, portalPageQueryService);
    }

    @Test
    void should_publish_page() {
        var pageId = PageId.random();
        var repoCtx = PortalPageContext.builder()
            .pageId(pageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId("env")
            .published(false)
            .build();

        portalPageContextCrudService.initWith(List.of(repoCtx));

        var updatedDetails = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        var page = new PortalPage(pageId, new GraviteeMarkdown("content"));
        var expected = new PortalPageWithViewDetails(page, updatedDetails);
        portalPageQueryService.initWith(List.of(expected));

        var output = cut.execute(new UpdatePortalPageViewPublicationStatusUseCase.Input(pageId, true));

        assertSame(expected, output.portalPage());
    }

    @Test
    void should_unpublish_page() {
        var pageId = PageId.random();
        var repoCtx = PortalPageContext.builder()
            .pageId(pageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId("env")
            .published(true)
            .build();

        portalPageContextCrudService.initWith(List.of(repoCtx));

        var updatedDetails = new PortalPageView(PortalViewContext.HOMEPAGE, false);
        var page = new PortalPage(pageId, new GraviteeMarkdown("content"));
        var expected = new PortalPageWithViewDetails(page, updatedDetails);
        portalPageQueryService.initWith(List.of(expected));

        var output = cut.execute(new UpdatePortalPageViewPublicationStatusUseCase.Input(pageId, false));

        assertSame(expected, output.portalPage());
    }

    @Test
    void should_throw_when_page_not_found() {
        var pageId = PageId.random();

        assertThrows(io.gravitee.apim.core.portal_page.exception.PortalPageSpecificationException.class, () ->
            cut.execute(new UpdatePortalPageViewPublicationStatusUseCase.Input(pageId, true))
        );
    }

    @Test
    void should_throw_when_publishing_already_published() {
        var pageId = PageId.random();
        var repoCtx = PortalPageContext.builder()
            .pageId(pageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId("env")
            .published(true)
            .build();

        portalPageContextCrudService.initWith(List.of(repoCtx));

        assertThrows(io.gravitee.apim.core.portal_page.exception.IllegalPublicationStateException.class, () ->
            cut.execute(new UpdatePortalPageViewPublicationStatusUseCase.Input(pageId, true))
        );
    }

    @Test
    void should_throw_when_unpublishing_already_unpublished() {
        var pageId = PageId.random();
        var repoCtx = PortalPageContext.builder()
            .pageId(pageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId("env")
            .published(false)
            .build();

        portalPageContextCrudService.initWith(List.of(repoCtx));

        assertThrows(io.gravitee.apim.core.portal_page.exception.IllegalPublicationStateException.class, () ->
            cut.execute(new UpdatePortalPageViewPublicationStatusUseCase.Input(pageId, false))
        );
    }
}
