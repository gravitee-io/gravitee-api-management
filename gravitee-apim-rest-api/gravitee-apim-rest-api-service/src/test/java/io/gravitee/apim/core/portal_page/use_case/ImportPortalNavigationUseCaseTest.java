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
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalNavigationManifestParserInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationBulkImportDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationManifestParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportPortalNavigationUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    private ImportPortalNavigationUseCase useCase;
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private PortalNavigationItemSourceDomainServiceInMemory sourceDomainService;
    private PortalNavigationManifestParserInMemory manifestParser;

    @BeforeEach
    void setUp() {
        var storage = new ArrayList<PortalNavigationItem>();
        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();
        manifestParser = new PortalNavigationManifestParserInMemory();

        var pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage());
        var domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            pageContentQueryService,
            new ApiCrudServiceInMemory(),
            sourceDomainService
        );
        var validatorService = new PortalNavigationItemValidatorService(
            queryService,
            pageContentQueryService,
            new ApiProductQueryServiceInMemory(),
            sourceDomainService
        );
        var bulkImportDomainService = new PortalNavigationBulkImportDomainService(
            sourceDomainService,
            manifestParser,
            domainService,
            queryService,
            crudService,
            pageContentCrudService,
            pageContentQueryService
        );
        useCase = new ImportPortalNavigationUseCase(
            validatorService,
            domainService,
            sourceDomainService,
            bulkImportDomainService,
            queryService
        );
    }

    private ImportPortalNavigationUseCase.Output execute(String title) {
        return execute(title, null);
    }

    private ImportPortalNavigationUseCase.Output execute(String title, PortalNavigationItemId parentId) {
        return useCase.execute(
            ImportPortalNavigationUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .title(title)
                .parentId(parentId)
                .visibility(PortalVisibility.PRIVATE)
                .source(aSource())
                .build()
        );
    }

    private PortalNavigationItemSource aSource() {
        return PortalNavigationItemSource.builder().sourceType("github-fetcher").sourceConfiguration("{\"repository\":\"docs\"}").build();
    }

    @Nested
    class WithoutManifest {

        @Test
        void should_mirror_the_remote_tree_and_ignore_unsupported_files() {
            sourceDomainService.givenRemoteFile("/docs/getting-started.md", "# Getting started");
            sourceDomainService.givenRemoteFile("/docs/advanced/tuning.md", "# Tuning");
            sourceDomainService.givenRemoteFile("/specs/petstore.yaml", "openapi: 3.0.3");
            sourceDomainService.givenRemoteFile("/logo.png", "binary");
            // No AsciiDoc content type in the NG portal: rendering .adoc as markdown leaks raw syntax
            sourceDomainService.givenRemoteFile("/docs/notes.adoc", "= AsciiDoc");

            var output = execute("Imported Docs");

            var root = output.rootFolder();
            assertThat(root.getTitle()).isEqualTo("Imported Docs");
            assertThat(root.getSource()).isNotNull();
            // The marker is what routes later fetches of this folder to a re-import
            assertThat(root.getSource().isSubtreeImport()).isTrue();
            assertThat(root.getSource().getLastFetchedAt()).isNotNull();
            assertThat(root.getSource().getLastFetchError()).isNull();

            var docsFolder = childFolder(root.getId(), "docs");
            var advancedFolder = childFolder(docsFolder.getId(), "advanced");
            var specsFolder = childFolder(root.getId(), "specs");
            assertThat(childPage(docsFolder.getId(), "getting-started").getSource()).isNull();
            assertThat(contentOf(childPage(docsFolder.getId(), "getting-started"))).isInstanceOf(GraviteeMarkdownPageContent.class);
            assertThat(contentOf(childPage(advancedFolder.getId(), "tuning"))).isInstanceOf(GraviteeMarkdownPageContent.class);
            assertThat(contentOf(childPage(specsFolder.getId(), "petstore"))).isInstanceOf(OpenApiPageContent.class);

            assertThat(output.result().files())
                .extracting(
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::title,
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::success
                )
                .containsExactlyInAnyOrder(tuple("getting-started", true), tuple("tuning", true), tuple("petstore", true));
        }

        @Test
        void should_fail_the_second_entry_colliding_on_the_same_destination_and_title() {
            // api.md and api.yaml both land as "api" under /docs: importing both would silently
            // overwrite one with the other while reporting two successes on the same item
            sourceDomainService.givenRemoteFile("/docs/api.md", "# Api");
            sourceDomainService.givenRemoteFile("/docs/api.yaml", "openapi: 3.0.3");

            var output = execute("Imported Docs");

            assertThat(output.result().files())
                .extracting(
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::title,
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::success
                )
                .containsExactlyInAnyOrder(tuple("api", true), tuple("api", false));
            assertThat(output.result().files())
                .filteredOn(file -> !file.success())
                .singleElement()
                .satisfies(file -> assertThat(file.error()).contains("Duplicate title"));
        }

        @Test
        void should_leave_out_mirrored_files_that_are_not_documents() {
            // Sharing an extension with a spec is not enough: these used to be imported as broken
            // OpenAPI pages, and reporting them as failures on every run would be just as wrong
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            sourceDomainService.givenRemoteFile("/package.json", "{\"name\": \"docs\", \"version\": \"1.0.0\"}");
            sourceDomainService.givenRemoteFile("/.github/workflows/ci.yml", "name: build\non:\n  push:\n    branches: [master]");

            var output = execute("Imported Docs");

            assertThat(output.result().files())
                .extracting(
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::title,
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::success
                )
                .containsExactly(tuple("intro", true));
            assertThat(findChild(output.rootFolder().getId(), "package")).isNull();
            // Nor the folders that only existed to hold them
            assertThat(findChild(output.rootFolder().getId(), ".github")).isNull();
            // A clean import, not a partial one: nothing was left to report
            assertThat(output.rootFolder().getSource().getLastFetchError()).isNull();
            assertThat(output.rootFolder().getSource().getLastFetchedAt()).isNotNull();
        }

        @Test
        void should_record_a_listing_whose_files_hold_no_document_as_a_failure() {
            // Every listed file has a spec extension but none is a spec: importing "successfully
            // nothing" would empty the subtree on a re-import
            sourceDomainService.givenRemoteFile("/package.json", "{\"name\": \"docs\"}");
            sourceDomainService.givenRemoteFile("/ci.yml", "name: build");

            var output = execute("Imported Docs");

            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchError()).contains("no importable files");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }

        @Test
        void should_detect_asyncapi_specs_from_their_content() {
            sourceDomainService.givenRemoteFile("/specs/events.yaml", "asyncapi: 3.0.0");

            var output = execute("Imported Docs");

            var specsFolder = childFolder(output.rootFolder().getId(), "specs");
            assertThat(contentOf(childPage(specsFolder.getId(), "events"))).isInstanceOf(AsyncApiPageContent.class);
        }

        @Test
        void should_report_per_file_failures_without_aborting_the_import() {
            sourceDomainService.givenRemoteFile("/docs/ok.md", "# Ok");
            sourceDomainService.givenRemoteFile("/docs/broken.md", "# Broken");
            sourceDomainService.failFileFetch("/docs/broken.md");

            var output = execute("Imported Docs");

            assertThat(output.result().files())
                .extracting(
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::title,
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::success
                )
                .containsExactlyInAnyOrder(tuple("ok", true), tuple("broken", false));
            assertThat(output.rootFolder().getSource().getLastFetchError()).contains("1 of 2");
        }

        @Test
        void should_record_listing_failure_on_the_root_folder_instead_of_failing_the_creation() {
            sourceDomainService.givenRemoteFile("/docs/ok.md", "# Ok");
            sourceDomainService.failNextListFilesWith(new RuntimeException("cannot reach repository"));

            var output = execute("Imported Docs");

            assertThat(output.rootFolder().getSource().getLastFetchError()).contains("Unable to list files");
            assertThat(output.rootFolder().getSource().getLastFetchError()).doesNotContain("cannot reach repository");
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }

        @Test
        void should_record_an_empty_listing_as_a_failure_instead_of_importing_nothing() {
            // Files-capable source whose listing comes back empty — as a silently failing fetcher answers
            sourceDomainService.givenRemoteFile("/docs/gone.md", "# Gone");
            sourceDomainService.removeRemoteFile("/docs/gone.md");

            var output = execute("Imported Docs");

            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchError()).contains("listed no files");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }

        @Test
        void should_record_a_listing_without_importable_files_as_a_failure() {
            // Only unsupported types listed: importing "successfully nothing" would hide the problem
            sourceDomainService.givenRemoteFile("/docs/notes.adoc", "= AsciiDoc");
            sourceDomainService.givenRemoteFile("/logo.png", "binary");

            var output = execute("Imported Docs");

            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchError()).contains("no importable files");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }
    }

    @Nested
    class WithManifest {

        @Test
        void should_follow_the_manifest_hierarchy() {
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/content/intro.md", "# Intro");
            sourceDomainService.givenRemoteFile("/content/api.yaml", "openapi: 3.0.3");
            manifestParser.willParse(
                List.of(
                    new PortalNavigationManifestParser.ManifestPage("/content/intro.md", "Introduction", "/guides"),
                    new PortalNavigationManifestParser.ManifestPage("/content/api.yaml", null, null)
                )
            );

            var output = execute("Imported Docs");

            var root = output.rootFolder();
            var guidesFolder = childFolder(root.getId(), "guides");
            assertThat(childPage(guidesFolder.getId(), "Introduction")).isNotNull();
            // No dest in the manifest: the file's parent path is mirrored
            var contentFolder = childFolder(root.getId(), "content");
            assertThat(contentOf(childPage(contentFolder.getId(), "api"))).isInstanceOf(OpenApiPageContent.class);
        }

        @Test
        void should_follow_a_manifest_below_the_repository_root() {
            // A fetcher rooted in a subdirectory lists its files with the prefix, manifest included
            sourceDomainService.givenRemoteFile("/docs/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            manifestParser.willParse(List.of(new PortalNavigationManifestParser.ManifestPage("/docs/intro.md", "Introduction", "/guides")));

            var output = execute("Imported Docs");

            var guidesFolder = childFolder(output.rootFolder().getId(), "guides");
            assertThat(childPage(guidesFolder.getId(), "Introduction")).isNotNull();
            assertThat(findChild(output.rootFolder().getId(), "docs")).isNull();
        }

        @Test
        void should_read_the_least_deep_manifest_when_several_directories_carry_one() {
            // The deeper manifest is listed first: taking the first match would read it
            sourceDomainService.givenRemoteFile("/docs/.gravitee.json", "{nested manifest}");
            sourceDomainService.failFileFetch("/docs/.gravitee.json");
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            manifestParser.willParse(List.of(new PortalNavigationManifestParser.ManifestPage("/docs/intro.md", "Introduction", "/guides")));

            var output = execute("Imported Docs");

            var guidesFolder = childFolder(output.rootFolder().getId(), "guides");
            assertThat(childPage(guidesFolder.getId(), "Introduction")).isNotNull();
        }

        @Test
        void should_not_treat_a_file_merely_suffixed_like_the_manifest_as_the_manifest() {
            sourceDomainService.givenRemoteFile("/docs/my.gravitee.json", "{\"not\":\"a manifest\"}");
            sourceDomainService.givenRemoteFile("/docs/guide.md", "# Guide");
            manifestParser.willParse(
                List.of(new PortalNavigationManifestParser.ManifestPage("/docs/guide.md", "From Manifest", "/from-manifest"))
            );

            var output = execute("Imported Docs");

            // The manifest was not detected: the remote tree is mirrored instead of following willParse
            var docsFolder = childFolder(output.rootFolder().getId(), "docs");
            assertThat(childPage(docsFolder.getId(), "guide")).isNotNull();
            assertThat(findChild(output.rootFolder().getId(), "from-manifest")).isNull();
        }

        @Test
        void should_fail_a_manifest_page_pointing_at_an_unsupported_file() {
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/docs/legacy.adoc", "= Legacy");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            manifestParser.willParse(
                List.of(
                    new PortalNavigationManifestParser.ManifestPage("/docs/legacy.adoc", "Legacy", "/guides"),
                    new PortalNavigationManifestParser.ManifestPage("/docs/intro.md", "Introduction", "/guides")
                )
            );

            var output = execute("Imported Docs");

            assertThat(output.result().files())
                .extracting(
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::title,
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::success
                )
                .containsExactlyInAnyOrder(tuple("Legacy", false), tuple("Introduction", true));

            // A partial import is still a fetch: the imported pages are current, the error lists the rest
            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchedAt()).isNotNull();
            assertThat(source.getLastFetchError()).contains("Failed to import 1 of 2");
        }

        @Test
        void should_fail_a_manifest_page_pointing_at_a_file_that_is_not_a_document() {
            // Mirror mode leaves such a file out silently; a manifest named it, so the author must see it
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/config.json", "{\"name\": \"docs\"}");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            manifestParser.willParse(
                List.of(
                    new PortalNavigationManifestParser.ManifestPage("/config.json", "Config", "/guides"),
                    new PortalNavigationManifestParser.ManifestPage("/docs/intro.md", "Introduction", "/guides")
                )
            );

            var output = execute("Imported Docs");

            assertThat(output.result().files())
                .extracting(
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::title,
                    PortalNavigationBulkImportDomainService.BulkImportResult.FileImportResult::success
                )
                .containsExactlyInAnyOrder(tuple("Config", false), tuple("Introduction", true));
            assertThat(output.result().files())
                .filteredOn(file -> !file.success())
                .singleElement()
                .satisfies(file -> assertThat(file.error()).contains("Cannot determine the type of /config.json"));
        }

        @Test
        void should_record_a_manifest_without_pages_as_a_failure() {
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            manifestParser.willParse(List.of());

            var output = execute("Imported Docs");

            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchError()).contains(".gravitee.json manifest").contains("no importable pages");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }

        @Test
        void should_record_a_manifest_fetch_failure_on_the_source_instead_of_failing_the_import() {
            // The network incident most likely in practice: the listing works, the manifest fetch does not
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            sourceDomainService.failFileFetch("/.gravitee.json");

            var output = execute("Imported Docs");

            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchError()).contains(".gravitee.json manifest");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(source.getLastFetchAttemptAt()).isNotNull();
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }

        @Test
        void should_record_a_manifest_parse_failure_on_the_source_instead_of_failing_the_import() {
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{not a manifest}");
            sourceDomainService.givenRemoteFile("/docs/intro.md", "# Intro");
            manifestParser.failOnParse();

            var output = execute("Imported Docs");

            var source = output.rootFolder().getSource();
            assertThat(source.getLastFetchError()).contains(".gravitee.json manifest");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(output.result().files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
        }
    }

    @Nested
    class Validation {

        @Test
        void should_reject_a_source_that_cannot_list_files() {
            // No remote file registered: the in-memory source is not files-capable
            assertThatThrownBy(() -> execute("Imported Docs"))
                .isInstanceOf(InvalidPortalNavigationItemSourceException.class)
                .hasMessageContaining("cannot list files");
            assertThat(queryService.storage()).isEmpty();
        }

        @Test
        void should_derive_the_parent_area_and_reject_an_import_below_the_homepage() {
            // The caller never picks an area: deriving it from the parent means a homepage parent is
            // rejected by the homepage rules — the area can only hold one item — instead of an
            // area-mismatch error about a choice the caller never made
            sourceDomainService.givenRemoteFile("/docs/ok.md", "# Ok");
            var parent = PortalNavigationFolder.builder()
                .id(PortalNavigationItemId.random())
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .title("Homepage Folder")
                .segment("homepage-folder")
                .area(PortalArea.HOMEPAGE)
                .order(0)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .build();
            crudService.create(parent);

            assertThatThrownBy(() -> execute("Imported Docs", parent.getId())).isInstanceOf(HomepageAlreadyExistsException.class);
            // Nothing created besides the pre-existing parent
            assertThat(queryService.storage()).hasSize(1);
        }

        @Test
        void should_reject_an_import_below_an_unknown_parent() {
            // The area derivation looks the parent up before validation runs: an unknown id must
            // still surface as the validation error, not as an NPE
            sourceDomainService.givenRemoteFile("/docs/ok.md", "# Ok");

            assertThatThrownBy(() -> execute("Imported Docs", PortalNavigationItemId.random())).isInstanceOf(ParentNotFoundException.class);
            assertThat(queryService.storage()).isEmpty();
        }

        @Test
        void should_reject_an_import_below_a_sourced_folder() {
            sourceDomainService.givenRemoteFile("/docs/ok.md", "# Ok");
            var sourcedFolder = PortalNavigationFolder.builder()
                .id(PortalNavigationItemId.random())
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .title("Managed Folder")
                .segment("managed-folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .published(true)
                .visibility(PortalVisibility.PUBLIC)
                .source(aSource())
                .build();
            crudService.create(sourcedFolder);

            assertThatThrownBy(() -> execute("Imported Docs", sourcedFolder.getId())).isInstanceOf(
                InvalidPortalNavigationItemDataException.class
            );
        }
    }

    @Nested
    class Reimport {

        @Test
        void should_update_create_and_delete_pages_to_mirror_the_remote_listing() {
            sourceDomainService.givenRemoteFile("/docs/kept.md", "# Kept v1");
            sourceDomainService.givenRemoteFile("/docs/removed.md", "# Removed");
            var root = execute("Imported Docs").rootFolder();
            var docsFolder = childFolder(root.getId(), "docs");
            var keptPageId = childPage(docsFolder.getId(), "kept").getId();

            sourceDomainService.givenRemoteFile("/docs/kept.md", "# Kept v2");
            sourceDomainService.givenRemoteFile("/docs/added.md", "# Added");
            sourceDomainService.removeRemoteFile("/docs/removed.md");

            var result = reimportSubtree(root.getId());

            assertThat(result.files()).allMatch(file -> file.success());
            var refreshedDocsFolder = childFolder(root.getId(), "docs");
            assertThat(refreshedDocsFolder.getId()).isEqualTo(docsFolder.getId());
            var keptPage = childPage(refreshedDocsFolder.getId(), "kept");
            assertThat(keptPage.getId()).isEqualTo(keptPageId);
            assertThat(((GraviteeMarkdownPageContent) contentOf(keptPage)).getContent().value()).isEqualTo("# Kept v2");
            assertThat(childPage(refreshedDocsFolder.getId(), "added")).isNotNull();
            assertThat(findChild(refreshedDocsFolder.getId(), "removed")).isNull();
        }

        @Test
        void should_keep_the_last_good_page_when_a_renamed_entry_fails_to_fetch() {
            sourceDomainService.givenRemoteFile("/.gravitee.json", "{manifest}");
            sourceDomainService.givenRemoteFile("/docs/a.md", "# A");
            manifestParser.willParse(List.of(new PortalNavigationManifestParser.ManifestPage("/docs/a.md", "Old", "/guides")));
            var root = execute("Imported Docs").rootFolder();
            var guidesFolder = childFolder(root.getId(), "guides");
            var lastGoodPageId = childPage(guidesFolder.getId(), "Old").getId();

            // The manifest renames the page and fetching the file transiently fails on that same run:
            // nothing is touched under either title, and pruning would delete the last good page
            manifestParser.willParse(List.of(new PortalNavigationManifestParser.ManifestPage("/docs/a.md", "New", "/guides")));
            sourceDomainService.failFileFetch("/docs/a.md");

            var result = reimportSubtree(root.getId());

            assertThat(result.files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
            assertThat(childPage(guidesFolder.getId(), "Old").getId()).isEqualTo(lastGoodPageId);
        }

        @Test
        void should_defer_pruning_until_every_entry_imported_successfully() {
            sourceDomainService.givenRemoteFile("/docs/kept.md", "# Kept");
            sourceDomainService.givenRemoteFile("/docs/flaky.md", "# Flaky");
            sourceDomainService.givenRemoteFile("/docs/removed.md", "# Removed");
            var root = execute("Imported Docs").rootFolder();
            var docsFolder = childFolder(root.getId(), "docs");

            sourceDomainService.removeRemoteFile("/docs/removed.md");
            sourceDomainService.failFileFetch("/docs/flaky.md");

            var result = reimportSubtree(root.getId());

            assertThat(result.files())
                .filteredOn(file -> !file.success())
                .hasSize(1);
            assertThat(childPage(docsFolder.getId(), "removed")).isNotNull();

            // The next clean run reconciles the subtree and prunes what the remote no longer holds
            var cleanResult = reimportSubtree(root.getId());

            assertThat(cleanResult.files()).allMatch(file -> file.success());
            assertThat(findChild(docsFolder.getId(), "removed")).isNull();
        }

        @Test
        void should_keep_the_imported_pages_when_a_re_import_lists_no_files() {
            sourceDomainService.givenRemoteFile("/docs/kept.md", "# Kept");
            var root = execute("Imported Docs").rootFolder();
            var docsFolder = childFolder(root.getId(), "docs");

            // A flaky fetcher can answer an empty listing without failing; it must not wipe the subtree
            sourceDomainService.removeRemoteFile("/docs/kept.md");

            var result = reimportSubtree(root.getId());

            assertThat(result.files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
            assertThat(childPage(docsFolder.getId(), "kept")).isNotNull();
            var refreshedRoot = (PortalNavigationFolder) queryService.findByIdAndEnvironmentId(ENV_ID, root.getId());
            assertThat(refreshedRoot.getSource().getLastFetchError()).contains("listed no files");
        }

        @Test
        void should_keep_the_imported_pages_when_a_re_import_lists_only_unsupported_files() {
            sourceDomainService.givenRemoteFile("/docs/kept.md", "# Kept");
            var root = execute("Imported Docs").rootFolder();
            var docsFolder = childFolder(root.getId(), "docs");

            // The remote tree now only carries unsupported types (e.g. every page moved to .adoc)
            sourceDomainService.removeRemoteFile("/docs/kept.md");
            sourceDomainService.givenRemoteFile("/docs/kept.adoc", "= Kept");

            var result = reimportSubtree(root.getId());

            assertThat(result.files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
            assertThat(childPage(docsFolder.getId(), "kept")).isNotNull();
            var refreshedRoot = (PortalNavigationFolder) queryService.findByIdAndEnvironmentId(ENV_ID, root.getId());
            assertThat(refreshedRoot.getSource().getLastFetchError()).contains("no importable files");
        }

        @Test
        void should_keep_the_imported_pages_when_a_re_import_holds_no_document() {
            sourceDomainService.givenRemoteFile("/docs/kept.md", "# Kept");
            var root = execute("Imported Docs").rootFolder();
            var docsFolder = childFolder(root.getId(), "docs");

            // The listing is no longer empty, but nothing in it is a document
            sourceDomainService.removeRemoteFile("/docs/kept.md");
            sourceDomainService.givenRemoteFile("/package.json", "{\"name\": \"docs\"}");

            var result = reimportSubtree(root.getId());

            assertThat(result.files())
                .singleElement()
                .satisfies(file -> assertThat(file.success()).isFalse());
            assertThat(childPage(docsFolder.getId(), "kept")).isNotNull();
            var refreshedRoot = (PortalNavigationFolder) queryService.findByIdAndEnvironmentId(ENV_ID, root.getId());
            assertThat(refreshedRoot.getSource().getLastFetchError()).contains("no importable files");
        }

        private PortalNavigationBulkImportDomainService.BulkImportResult reimportSubtree(PortalNavigationItemId rootId) {
            var bulkImportDomainService = new PortalNavigationBulkImportDomainService(
                sourceDomainService,
                manifestParser,
                new PortalNavigationItemDomainService(
                    crudService,
                    queryService,
                    pageContentCrudService,
                    PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
                    new ApiCrudServiceInMemory(),
                    sourceDomainService
                ),
                queryService,
                crudService,
                pageContentCrudService,
                PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage())
            );
            return bulkImportDomainService.importSubtree((PortalNavigationFolder) queryService.findByIdAndEnvironmentId(ENV_ID, rootId));
        }
    }

    private PortalNavigationFolder childFolder(PortalNavigationItemId parentId, String title) {
        var found = findChild(parentId, title);
        assertThat(found).as("folder '%s' below %s", title, parentId).isInstanceOf(PortalNavigationFolder.class);
        return (PortalNavigationFolder) found;
    }

    private PortalNavigationPage childPage(PortalNavigationItemId parentId, String title) {
        var found = findChild(parentId, title);
        assertThat(found).as("page '%s' below %s", title, parentId).isInstanceOf(PortalNavigationPage.class);
        return (PortalNavigationPage) found;
    }

    private PortalNavigationItem findChild(PortalNavigationItemId parentId, String title) {
        return queryService
            .findByParentIdAndEnvironmentId(ENV_ID, parentId)
            .stream()
            .filter(item -> title.equals(item.getTitle()))
            .findFirst()
            .orElse(null);
    }

    private Object contentOf(PortalNavigationPage page) {
        return pageContentCrudService
            .storage()
            .stream()
            .filter(content -> content.getId().equals(page.getPortalPageContentId()))
            .findFirst()
            .orElseThrow();
    }
}
