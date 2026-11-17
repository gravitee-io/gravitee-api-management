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

import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import inmemory.ApiCrudServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalNavigationManifestParserInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationBulkImportDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
    private NavigationPortalDomainServiceInMemory navigationPortals;
    private FetchPortalNavigationItemUseCase useCase;
    private ApiCrudServiceInMemory apiCrudService;

    @BeforeEach
    void setUp() throws Exception {
        var storage = new ArrayList<PortalNavigationItem>();
        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();
        apiCrudService = new ApiCrudServiceInMemory();
        apiCrudService.initWith(List.of(Api.builder().id("api-id").environmentId(ENV_ID).build()));
        navigationPortals = new NavigationPortalDomainServiceInMemory();
        navigationPortals
            .portalCrudService()
            .create(
                Portal.of(
                    ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId(),
                    ENV_ID,
                    ORG_ID,
                    "Developer Portal",
                    PortalNavigationStructure.empty(),
                    null,
                    true,
                    URI.create(NavigationPortalDomainServiceInMemory.PORTAL_URL).toURL()
                )
            );

        var domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            apiCrudService,
            sourceDomainService,
            new ApiOwnedNavigationDomainService(queryService, crudService),
            navigationPortals
        );
        var bulkImportDomainService = new PortalNavigationBulkImportDomainService(
            sourceDomainService,
            new PortalNavigationManifestParserInMemory(),
            domainService,
            queryService,
            crudService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            navigationPortals.portalCrudService()
        );
        useCase = new FetchPortalNavigationItemUseCase(
            PortalNavigationUseCaseTestFixtures.access(
                ORG_ID,
                ENV_ID,
                queryService,
                PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
                apiCrudService
            ),
            new PortalNavigationSourcedItemsDomainService(queryService),
            domainService,
            sourceDomainService,
            bulkImportDomainService
        );
    }

    @Test
    void should_reject_foreign_associations_before_stamping_an_attempt_even_if_content_is_missing() {
        var own = aPage("Own", null, aSource().build());
        var foreign = aPage("Foreign", null, null)
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")))
            .portalPageContentId(own.getPortalPageContentId())
            .build();
        crudService.update(foreign);
        pageContentCrudService.reset();

        assertThatThrownBy(() -> execute(own)).isInstanceOf(PageContentNotFoundException.class);

        assertThat(own.getSource().getLastFetchAttemptAt()).isNull();
        assertThat(crudService.storage()).containsExactly(own, foreign);
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_stamp_an_attempt_before_reporting_missing_page_content() {
        var page = aPage("Broken page", null, aSource().lastFetchError("Previous failure").build());
        pageContentCrudService.delete(page.getPortalPageContentId());

        assertThatThrownBy(() -> execute(page)).isInstanceOf(PageContentNotFoundException.class);

        assertThat(page.getSource().getLastFetchAttemptAt()).isNotNull();
        assertThat(page.getSource().getLastFetchedAt()).isNull();
        assertThat(page.getSource().getLastFetchError()).isEqualTo("Previous failure");
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_preserve_folder_abort_on_missing_content_after_recording_the_attempt() {
        var folder = aFolder("Broken folder", null);
        var missing = aPage("Missing", folder.getId(), aSource().build());
        var later = aPage("Later", folder.getId(), aSource().build());
        pageContentCrudService.delete(missing.getPortalPageContentId());

        assertThatThrownBy(() -> execute(folder)).isInstanceOf(PageContentNotFoundException.class);

        assertThat(missing.getSource().getLastFetchAttemptAt()).isNotNull();
        assertThat(later.getSource().getLastFetchAttemptAt()).isNull();
        assertThat(fetchedContentOf(later)).isEqualTo("# Default content");
    }

    @Test
    void should_reimport_and_remove_an_orphan_whose_content_is_missing() {
        var folder = aFolder("Imported", null, aSource().subtreeImport(true).build());
        var orphan = aPage("Removed file", folder.getId(), null);
        pageContentCrudService.delete(orphan.getPortalPageContentId());
        sourceDomainService.givenRemoteFile("/current.md", "# Current");

        var result = execute(folder).summary();

        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, orphan.getId())).isNull();
        assertThat(folder.getSource().getLastFetchAttemptAt()).isNotNull();
        assertThat(pageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_not_fetch_an_existing_item_through_another_scope() {
        var page = aPage("API guide", null, aSource().build());
        page.changeOwner(new NavigationItemReference.ApiReference("api-id"));

        assertThatThrownBy(() ->
            useCase.execute(new FetchPortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, page.getId().json(), PORTAL_REFERENCE))
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);

        assertThat(page.getSource().getLastFetchAttemptAt()).isNull();
        assertThat(fetchedContentOf(page)).isEqualTo("# Default content");
    }

    @Test
    void should_throw_when_item_not_found() {
        assertThatThrownBy(() ->
            useCase.execute(
                new FetchPortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, "00000000-0000-0000-0000-000000000099", PORTAL_REFERENCE)
            )
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
        void should_refresh_a_page_owned_by_an_api_without_changing_its_owner_or_publishing_it() {
            var apiReference = new NavigationItemReference.ApiReference("api-id");
            var page = aPage("Sourced Page", null, aSource().build());
            var apiPage = page.toBuilder().reference(apiReference).published(false).build();
            crudService.update(apiPage);

            execute(apiPage);

            var stored = queryService.findByIdAndEnvironmentId(ENV_ID, page.getId());
            assertThat(stored.getReference()).isEqualTo(apiReference);
            assertThat(stored.getPublished()).isFalse();
            assertThat(stored.getParentId()).isNull();
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

    @Nested
    class ImportedFolder {

        @Test
        void should_reimport_the_subtree_of_an_import_managed_folder() {
            var folder = aFolder("Imported Docs", null, aSource().subtreeImport(true).build());
            sourceDomainService.givenRemoteFile("/docs/guide.md", "# Guide");

            var summary = execute(folder).summary();

            assertThat(summary.succeeded()).isEqualTo(1);
            assertThat(summary.failed()).isZero();
            assertThat(summary.results())
                .singleElement()
                .satisfies(result -> assertThat(result.title()).isEqualTo("guide"));
            var docsFolder = queryService
                .findByParentIdAndEnvironmentId(ENV_ID, folder.getId())
                .stream()
                .filter(item -> "docs".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
            assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, docsFolder.getId()))
                .singleElement()
                .satisfies(item -> assertThat(item.getTitle()).isEqualTo("guide"));
        }

        @Test
        void should_walk_descendants_of_a_sourced_folder_the_import_did_not_create() {
            // A folder sourced by hand keeps the historical fetch semantics: its own source is
            // never fetched, so its hand-made children can never be replaced by a remote tree
            var folder = aFolder("Legacy Folder", null, aSource().build());
            var child = aPage("Sourced Child", folder.getId(), aSource().build());
            sourceDomainService.givenRemoteFile("/docs/guide.md", "# Guide");

            var summary = execute(folder).summary();

            assertThat(summary.results())
                .singleElement()
                .satisfies(result -> assertThat(result.title()).isEqualTo("Sourced Child"));
            assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, folder.getId())).containsExactly(child);
        }
    }

    private FetchPortalNavigationItemUseCase.Output execute(PortalNavigationItem item) {
        return useCase.execute(new FetchPortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, item.getId().toString(), item.getReference()));
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId) {
        return aFolder(title, parentId, null);
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId, PortalNavigationItemSource source) {
        var folder = PortalNavigationFolder.builder()
            .reference(PORTAL_REFERENCE)
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
            .reference(PORTAL_REFERENCE)
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
