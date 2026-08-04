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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FetchPortalPageContentUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private PortalNavigationItemSourceDomainServiceInMemory sourceDomainService;
    private FetchPortalPageContentUseCase useCase;

    @BeforeEach
    void setUp() {
        var storage = new ArrayList<PortalNavigationItem>();
        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();

        var domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            new ApiCrudServiceInMemory(),
            sourceDomainService
        );
        useCase = new FetchPortalPageContentUseCase(queryService, domainService, sourceDomainService);
    }

    private PortalNavigationPage aSourcedPage(PortalNavigationItemSource source) {
        var content = GraviteeMarkdownPageContent.create(ORG_ID, ENV_ID, "# Default content");
        pageContentCrudService.create(content);

        var page = (PortalNavigationPage) PortalNavigationItem.from(
            CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.PAGE)
                .title("Sourced Page")
                .segment("sourced-page")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(content.getId())
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .source(source)
                .build(),
            ORG_ID,
            ENV_ID,
            null
        );
        crudService.create(page);
        return page;
    }

    private PortalNavigationItemSource.PortalNavigationItemSourceBuilder aSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}");
    }

    @Test
    void should_fetch_content_overwrite_page_content_and_clear_previous_error() {
        var page = aSourcedPage(aSource().lastFetchError("previous error").build());

        var output = useCase.execute(new FetchPortalPageContentUseCase.Input(ENV_ID, page.getId().toString()));

        var source = output.updatedItem().getSource();
        assertThat(source).isNotNull();
        assertThat(source.getLastFetchedAt()).isNotNull();
        assertThat(source.getLastFetchError()).isNull();
        assertThat(pageContentCrudService.storage())
            .singleElement()
            .satisfies(content ->
                assertThat(((GraviteeMarkdownPageContent) content).getContent().value()).isEqualTo(
                    PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN
                )
            );
    }

    @Test
    void should_record_fetch_error_and_keep_existing_content_when_fetch_fails() {
        var page = aSourcedPage(aSource().build());
        sourceDomainService.failNextFetchWith(new TechnicalDomainException("fetch went wrong"));

        var output = useCase.execute(new FetchPortalPageContentUseCase.Input(ENV_ID, page.getId().toString()));

        var source = output.updatedItem().getSource();
        assertThat(source).isNotNull();
        assertThat(source.getLastFetchedAt()).isNull();
        assertThat(source.getLastFetchError()).isEqualTo("Unable to fetch content from source type http-fetcher.");
        assertThat(pageContentCrudService.storage())
            .singleElement()
            .satisfies(content -> assertThat(((GraviteeMarkdownPageContent) content).getContent().value()).isEqualTo("# Default content"));
    }

    @Test
    void should_record_fetch_error_even_when_the_exception_has_no_message() {
        var page = aSourcedPage(aSource().build());
        sourceDomainService.failNextFetchWith(new RuntimeException());

        var output = useCase.execute(new FetchPortalPageContentUseCase.Input(ENV_ID, page.getId().toString()));

        assertThat(output.updatedItem().getSource().getLastFetchError()).isNotBlank();
    }

    @Test
    void should_throw_when_item_not_found() {
        assertThatThrownBy(() ->
            useCase.execute(new FetchPortalPageContentUseCase.Input(ENV_ID, "00000000-0000-0000-0000-000000000099"))
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);
    }

    @Test
    void should_throw_when_page_has_no_source() {
        var page = aSourcedPage(null);

        assertThatThrownBy(() -> useCase.execute(new FetchPortalPageContentUseCase.Input(ENV_ID, page.getId().toString())))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("no external source");
    }

    @Test
    void should_throw_when_item_is_not_a_page() {
        var folder = PortalNavigationItem.from(
            CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("Folder")
                .segment("folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build(),
            ORG_ID,
            ENV_ID,
            null
        );
        crudService.create(folder);

        assertThatThrownBy(() -> useCase.execute(new FetchPortalPageContentUseCase.Input(ENV_ID, folder.getId().toString())))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("no external source");
    }
}
