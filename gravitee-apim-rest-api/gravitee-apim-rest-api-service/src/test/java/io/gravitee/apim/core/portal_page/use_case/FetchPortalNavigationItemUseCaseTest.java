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
import static org.assertj.core.groups.Tuple.tuple;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FetchPortalNavigationItemUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private PortalNavigationItemSourceDomainServiceInMemory sourceDomainService;
    private FetchPortalNavigationItemUseCase useCase;

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
        useCase = new FetchPortalNavigationItemUseCase(
            queryService,
            new PortalNavigationSourcedItemsDomainService(queryService),
            domainService,
            sourceDomainService
        );
    }

    @Test
    void should_throw_when_item_not_found() {
        assertThatThrownBy(() ->
            useCase.execute(new FetchPortalNavigationItemUseCase.Input(ENV_ID, "00000000-0000-0000-0000-000000000099"))
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);
    }

    @Nested
    class SourcedPage {

        @Test
        void should_fetch_content_overwrite_page_content_and_clear_previous_error() {
            var page = aPage("Sourced Page", null, aSource().lastFetchError("previous error").build());

            var output = execute(page);

            var source = output.item().getSource();
            assertThat(output.summary()).isNull();
            assertThat(source).isNotNull();
            assertThat(source.getLastFetchedAt()).isNotNull();
            assertThat(source.getLastFetchError()).isNull();
            assertThat(fetchedContentOf(page)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
        }

        @Test
        void should_record_fetch_error_and_keep_existing_content_when_fetch_fails() {
            var page = aPage("Sourced Page", null, aSource().build());
            sourceDomainService.failNextFetchWith(new TechnicalDomainException("fetch went wrong"));

            var output = execute(page);

            var source = output.item().getSource();
            assertThat(source).isNotNull();
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(source.getLastFetchError()).isEqualTo("Unable to fetch content from source type http-fetcher.");
            assertThat(source.getLastFetchError()).doesNotContain("example.com");
            assertThat(fetchedContentOf(page)).isEqualTo("# Default content");
        }

        @Test
        void should_record_fetch_error_even_when_the_exception_has_no_message() {
            var page = aPage("Sourced Page", null, aSource().build());
            sourceDomainService.failNextFetchWith(new RuntimeException());

            assertThat(execute(page).item().getSource().getLastFetchError()).isNotBlank();
        }

        @Test
        void should_throw_when_page_has_no_source() {
            var page = aPage("Inline Page", null, null);

            assertThatThrownBy(() -> execute(page))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("has no external source configured");
        }
    }

    @Nested
    class Subtree {

        @Test
        void should_fetch_every_sourced_page_descendant_and_skip_pages_without_source() {
            var folder = aFolder("Guides", null);
            var subFolder = aFolder("Advanced", folder.getId());
            var sourcedPage = aPage("Sourced Page", folder.getId(), aSource().build());
            var nestedSourcedPage = aPage("Nested Sourced Page", subFolder.getId(), aSource().build());
            aPage("Inline Page", folder.getId(), null);

            var summary = execute(folder).summary();

            assertThat(summary.results())
                .extracting(
                    FetchPortalNavigationItemUseCase.PageFetchResult::navigationItemId,
                    FetchPortalNavigationItemUseCase.PageFetchResult::success
                )
                .containsExactlyInAnyOrder(tuple(sourcedPage.getId().id(), true), tuple(nestedSourcedPage.getId().id(), true));
            assertThat(summary.succeeded()).isEqualTo(2);
            assertThat(summary.failed()).isZero();
            assertThat(sourcedPage.getSource().getLastFetchedAt()).isNotNull();
            assertThat(nestedSourcedPage.getSource().getLastFetchedAt()).isNotNull();
            assertThat(fetchedContentOf(sourcedPage)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
            assertThat(fetchedContentOf(nestedSourcedPage)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
        }

        @Test
        void should_report_failure_without_blocking_other_pages_when_one_fetch_fails() {
            var folder = aFolder("Guides", null);
            aPage("Failing Page", folder.getId(), aSource().build());
            aPage("Working Page", folder.getId(), aSource().build());
            sourceDomainService.failNextFetchWith(new TechnicalDomainException("fetch went wrong"));

            var summary = execute(folder).summary();

            assertThat(summary.succeeded()).isEqualTo(1);
            assertThat(summary.failed()).isEqualTo(1);
            assertThat(summary.results())
                .filteredOn(result -> !result.success())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.error()).isEqualTo("Unable to fetch content from source type http-fetcher.");
                    assertThat(result.error()).doesNotContain("example.com");
                });
        }

        @Test
        void should_throw_when_no_descendant_carries_a_source() {
            var folder = aFolder("Guides", null);
            aPage("Inline Page", folder.getId(), null);

            assertThatThrownBy(() -> execute(folder))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("No page below navigation item");
        }

        @Test
        void should_not_claim_a_sourced_folder_has_no_source() {
            var folder = aFolder("Guides", null, aSource().build());

            assertThatThrownBy(() -> execute(folder))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("No page below navigation item")
                .hasMessageNotContaining("has no external source configured");
        }
    }

    private FetchPortalNavigationItemUseCase.Output execute(PortalNavigationItem item) {
        return useCase.execute(new FetchPortalNavigationItemUseCase.Input(ENV_ID, item.getId().toString()));
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId) {
        return aFolder(title, parentId, null);
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId, PortalNavigationItemSource source) {
        var folder = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .source(source)
            .build();
        crudService.create(folder);
        return folder;
    }

    private PortalNavigationPage aPage(String title, PortalNavigationItemId parentId, PortalNavigationItemSource source) {
        var content = GraviteeMarkdownPageContent.create(ORG_ID, ENV_ID, "# Default content");
        pageContentCrudService.create(content);

        var page = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(content.getId())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .source(source)
            .build();
        crudService.create(page);
        return page;
    }

    private PortalNavigationItemSource.PortalNavigationItemSourceBuilder aSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}");
    }

    private String fetchedContentOf(PortalNavigationPage page) {
        return pageContentCrudService
            .storage()
            .stream()
            .filter(content -> content.getId().equals(page.getPortalPageContentId()))
            .findFirst()
            .map(content -> ((GraviteeMarkdownPageContent) content).getContent().value())
            .orElseThrow();
    }
}
