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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.ImportedFileContentType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationManifestParser;
import io.gravitee.common.utils.TimeProvider;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Imports a remote documentation tree behind a {@code FilesFetcher} source into the subtree of the
 * folder carrying that source. The folder owns the sync: imported children carry no source of their
 * own and are read-only by inheritance. Re-running the import re-lists the files, updates matched
 * pages, creates new ones and removes the previously imported items that no longer exist remotely.
 */
@DomainService
@CustomLog
@RequiredArgsConstructor
public class PortalNavigationBulkImportDomainService {

    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final PortalNavigationManifestParser manifestParser;
    private final PortalNavigationItemDomainService itemDomainService;
    private final PortalNavigationItemsQueryService queryService;
    private final PortalNavigationItemCrudService crudService;
    private final PortalPageContentCrudService pageContentCrudService;
    private final PortalPageContentQueryService pageContentQueryService;

    public BulkImportResult importSubtree(PortalNavigationFolder rootFolder) {
        var source = importManagedSourceOf(rootFolder);
        source.registerFetchAttempt();

        final List<String> files;
        try {
            files = sourceDomainService.listFiles(source);
        } catch (Exception e) {
            log.warn(
                "Failed to list files of portal navigation folder [id={}, sourceType={}]",
                rootFolder.getId().json(),
                source.getSourceType(),
                e
            );
            // Built here rather than taken from the exception: the stored message is returned by the
            // API, and an arbitrary one may quote the configuration and its secrets.
            return failListing(rootFolder, "Unable to list files from source type %s.".formatted(source.getSourceType()));
        }

        if (files.isEmpty()) {
            // A silently failing fetcher answers an empty listing too; mirroring it would wipe the subtree
            log.warn(
                "Portal navigation folder [id={}, sourceType={}] listed no files: leaving its subtree untouched",
                rootFolder.getId().json(),
                source.getSourceType()
            );
            return failListing(rootFolder, "The source of type %s listed no files.".formatted(source.getSourceType()));
        }

        final ResolvedEntries resolved;
        try {
            resolved = resolveEntries(source, files);
        } catch (Exception e) {
            // The manifest lookup refetches from the source and parses: a network incident or a broken
            // manifest here must leave the same trail as a listing failure, not surface a raw error
            // while an import-managed folder sits in the tree with nothing stamped on its source.
            log.warn(
                "Failed to resolve the entries of portal navigation folder [id={}, sourceType={}]",
                rootFolder.getId().json(),
                source.getSourceType(),
                e
            );
            return failListing(
                rootFolder,
                "Unable to read the %s manifest from source type %s.".formatted(manifestParser.manifestFileName(), source.getSourceType())
            );
        }

        var entries = resolved.entries();
        if (entries.isEmpty()) {
            // Same wipe risk one level down: files were listed but none is importable (unsupported
            // types only, or a manifest with no pages) — deleteOrphans would then empty the subtree.
            // The two causes call for different fixes on the repository side, so name the right one.
            log.warn(
                "Portal navigation folder [id={}, sourceType={}] listed no importable files: leaving its subtree untouched",
                rootFolder.getId().json(),
                source.getSourceType()
            );
            return failListing(
                rootFolder,
                resolved.fromManifest()
                    ? "The %s manifest of the source lists no importable pages.".formatted(manifestParser.manifestFileName())
                    : "The source of type %s listed no importable files.".formatted(source.getSourceType())
            );
        }

        var touchedItemIds = new HashSet<PortalNavigationItemId>();
        var results = importEntries(rootFolder, entries, touchedItemIds, resolved.fromManifest());
        if (results.isEmpty()) {
            // Every mirrored entry turned out not to be a document once fetched — a repository whose
            // only JSON and YAML are package.json, lock files and CI workflows. Same wipe risk as an
            // empty listing: deleteOrphans would empty the subtree.
            log.warn(
                "Portal navigation folder [id={}, sourceType={}] holds no importable document: leaving its subtree untouched",
                rootFolder.getId().json(),
                source.getSourceType()
            );
            return failListing(rootFolder, "The source of type %s listed no importable files.".formatted(source.getSourceType()));
        }
        deleteOrphans(rootFolder, touchedItemIds);

        stampFetchOutcome(source, results);
        crudService.update(rootFolder);

        return new BulkImportResult(List.copyOf(results));
    }

    private PortalNavigationItemSource importManagedSourceOf(PortalNavigationFolder rootFolder) {
        var source = rootFolder.getSource();
        if (source == null) {
            throw InvalidPortalNavigationItemDataException.noSourceConfigured(rootFolder.getId().json());
        }
        if (!source.isSubtreeImport()) {
            // Deleting the orphans of a folder the import does not own would destroy hand-made content
            throw new IllegalStateException(
                "Folder %s is not managed by the navigation import: refusing to re-import its subtree.".formatted(rootFolder.getId().json())
            );
        }
        return source;
    }

    private BulkImportResult failListing(PortalNavigationFolder rootFolder, String error) {
        rootFolder.getSource().setLastFetchError(error);
        crudService.update(rootFolder);
        return BulkImportResult.listingFailure(rootFolder.getTitle(), error);
    }

    private List<BulkImportResult.FileImportResult> importEntries(
        PortalNavigationFolder rootFolder,
        List<ImportEntry> entries,
        Set<PortalNavigationItemId> touchedItemIds,
        boolean fromManifest
    ) {
        var folderIdsByPath = new HashMap<String, PortalNavigationItemId>();
        var results = new ArrayList<BulkImportResult.FileImportResult>();
        var seenTitles = new HashSet<String>();
        for (var entry : entries) {
            // Two entries landing on the same (destination, title) — api.md next to api.yaml, or two
            // manifest pages sharing name and dest — would silently overwrite each other's content
            // while both report success; failing the second one keeps every failure visible
            if (!seenTitles.add(entry.destinationPath() + "/" + entry.title())) {
                results.add(
                    BulkImportResult.FileImportResult.failure(
                        entry.title(),
                        "Duplicate title %s under %s: %s collides with an earlier entry.".formatted(
                            entry.title(),
                            entry.destinationPath().isEmpty() ? "the root folder" : entry.destinationPath(),
                            entry.sourcePath()
                        )
                    )
                );
                continue;
            }
            var result = importEntry(rootFolder, entry, folderIdsByPath, touchedItemIds, fromManifest);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    private void stampFetchOutcome(PortalNavigationItemSource source, List<BulkImportResult.FileImportResult> results) {
        var failed = results
            .stream()
            .filter(result -> !result.success())
            .count();
        if (failed == 0) {
            source.setLastFetchedAt(TimeProvider.instantNow());
            source.setLastFetchError(null);
            return;
        }
        // A partial import is still a fetch: the imported pages are current, the error lists the rest
        if (failed < results.size()) {
            source.setLastFetchedAt(TimeProvider.instantNow());
        }
        source.setLastFetchError(
            "Failed to import %d of %d files from source type %s.".formatted(failed, results.size(), source.getSourceType())
        );
    }

    private ResolvedEntries resolveEntries(PortalNavigationItemSource source, List<String> files) {
        var manifestFileName = manifestParser.manifestFileName();
        // The manifest can live below the fetcher's configured root ("/docs/.gravitee.json"), as in
        // the legacy documentation import — but only under its exact file name, so "my.gravitee.json"
        // cannot hijack the import. The least deep match wins: the outcome must not depend on the
        // listing order of the fetcher.
        var manifestPath = files
            .stream()
            .filter(file -> manifestFileName.equals(file) || file.endsWith("/" + manifestFileName))
            .min(
                Comparator.comparingLong((String file) ->
                    file
                        .chars()
                        .filter(c -> c == '/')
                        .count()
                ).thenComparing(Comparator.naturalOrder())
            );
        if (manifestPath.isPresent()) {
            var manifestContent = sourceDomainService.fetchFileContent(source, manifestPath.get());
            return new ResolvedEntries(
                manifestParser
                    .parse(manifestContent)
                    .stream()
                    .map(page ->
                        new ImportEntry(
                            page.src(),
                            isBlank(page.name()) ? baseNameOf(page.src()) : page.name(),
                            isBlank(page.dest()) ? parentPathOf(page.src()) : page.dest()
                        )
                    )
                    .toList(),
                true
            );
        }
        // Without a manifest the remote tree is mirrored as-is; files of unsupported types are ignored.
        // Paths are taken as listed by the fetcher — legacy-import parity: the configured root directory
        // belongs to the plugin's opaque configuration, so it cannot be stripped and becomes folder levels.
        return new ResolvedEntries(
            files
                .stream()
                .filter(ImportedFileContentType::isImportable)
                .map(file -> new ImportEntry(file, baseNameOf(file), parentPathOf(file)))
                .toList(),
            false
        );
    }

    /**
     * @return the outcome to report, or {@code null} when the entry holds no document and is left
     *         out of the import altogether.
     */
    @Nullable
    private BulkImportResult.FileImportResult importEntry(
        PortalNavigationFolder rootFolder,
        ImportEntry entry,
        Map<String, PortalNavigationItemId> folderIdsByPath,
        Set<PortalNavigationItemId> touchedItemIds,
        boolean fromManifest
    ) {
        try {
            // Nothing is looked up or created before the file is known to be a document: resolving the
            // destination creates the folders along the way, and a repository's .github/workflows has
            // no business showing up in the navigation tree.
            var content = sourceDomainService.fetchFileContent(rootFolder.getSource(), entry.sourcePath());
            var contentType = ImportedFileContentType.from(entry.sourcePath(), content).orElse(null);
            if (contentType == null) {
                if (fromManifest) {
                    // The manifest named this file: not importing it is a failure the author must see
                    return BulkImportResult.FileImportResult.failure(entry.title(), unsupportedDocumentError(entry.sourcePath()));
                }
                // A mirrored file that merely shares an extension with a spec — package.json, a CI
                // workflow. It is not a document, so it is left out of the import rather than reported
                // as a failure on every run.
                log.debug(
                    "Skipping non-document file [{}] below portal navigation folder [id={}]",
                    entry.sourcePath(),
                    rootFolder.getId().json()
                );
                return null;
            }

            var parentId = resolveTargetFolder(rootFolder, entry.destinationPath(), folderIdsByPath, touchedItemIds);
            var existingPage = findChildPageByTitle(rootFolder.getEnvironmentId(), parentId, entry.title());
            if (existingPage != null) {
                touchedItemIds.add(existingPage.getId());
                updatePageContent(existingPage, contentType, content);
                return BulkImportResult.FileImportResult.success(existingPage.getId().id(), entry.title());
            }

            var createdPage = createPage(rootFolder, parentId, entry.title(), contentType, content);
            touchedItemIds.add(createdPage.getId());
            return BulkImportResult.FileImportResult.success(createdPage.getId().id(), entry.title());
        } catch (Exception e) {
            log.warn("Failed to import file [{}] below portal navigation folder [id={}]", entry.sourcePath(), rootFolder.getId().json(), e);
            return BulkImportResult.FileImportResult.failure(entry.title(), "Unable to import file %s.".formatted(entry.sourcePath()));
        }
    }

    private PortalNavigationItemId resolveTargetFolder(
        PortalNavigationFolder rootFolder,
        String destinationPath,
        Map<String, PortalNavigationItemId> folderIdsByPath,
        Set<PortalNavigationItemId> touchedItemIds
    ) {
        touchedItemIds.add(rootFolder.getId());
        var currentParentId = rootFolder.getId();
        var currentPath = "";
        for (var pathElement : destinationPath.split("/")) {
            if (pathElement.isBlank()) {
                continue;
            }
            currentPath = currentPath + "/" + pathElement;
            var knownFolderId = folderIdsByPath.get(currentPath);
            if (knownFolderId != null) {
                currentParentId = knownFolderId;
                continue;
            }
            var folderId = findOrCreateChildFolder(rootFolder, currentParentId, pathElement);
            folderIdsByPath.put(currentPath, folderId);
            touchedItemIds.add(folderId);
            currentParentId = folderId;
        }
        return currentParentId;
    }

    private PortalNavigationItemId findOrCreateChildFolder(
        PortalNavigationFolder rootFolder,
        PortalNavigationItemId parentId,
        String title
    ) {
        var existing = queryService
            .findByParentIdAndEnvironmentId(rootFolder.getEnvironmentId(), parentId)
            .stream()
            .filter(PortalNavigationFolder.class::isInstance)
            .filter(item -> title.equals(item.getTitle()))
            .findFirst();
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        var created = itemDomainService.create(
            rootFolder.getOrganizationId(),
            rootFolder.getEnvironmentId(),
            CreatePortalNavigationItem.builder()
                .title(title)
                .type(PortalNavigationItemType.FOLDER)
                .area(rootFolder.getArea())
                .parentId(parentId)
                .visibility(rootFolder.getVisibility())
                .published(rootFolder.getPublished())
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build()
        );
        return created.getId();
    }

    @Nullable
    private PortalNavigationPage findChildPageByTitle(String environmentId, PortalNavigationItemId parentId, String title) {
        return queryService
            .findByParentIdAndEnvironmentId(environmentId, parentId)
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .filter(page -> title.equals(page.getTitle()))
            .findFirst()
            .orElse(null);
    }

    private void updatePageContent(PortalNavigationPage page, PortalPageContentType contentType, String content) {
        var existingContent = pageContentQueryService
            .findById(page.getPortalPageContentId())
            .orElseThrow(() -> new IllegalStateException("Page content not found for imported page " + page.getId().json()));
        var updatedContent = PortalPageContent.of(
            contentType,
            existingContent.getId(),
            existingContent.getOrganizationId(),
            existingContent.getEnvironmentId(),
            content,
            existingContent.getAutomationMetadata()
        );
        pageContentCrudService.update(updatedContent);
    }

    private PortalNavigationItem createPage(
        PortalNavigationFolder rootFolder,
        PortalNavigationItemId parentId,
        String title,
        PortalPageContentType contentType,
        String content
    ) {
        var pageContent = PortalPageContent.of(
            contentType,
            PortalPageContentId.random(),
            rootFolder.getOrganizationId(),
            rootFolder.getEnvironmentId(),
            content,
            null
        );
        var createdContent = pageContentCrudService.create(pageContent);
        return itemDomainService.create(
            rootFolder.getOrganizationId(),
            rootFolder.getEnvironmentId(),
            CreatePortalNavigationItem.builder()
                .title(title)
                .type(PortalNavigationItemType.PAGE)
                .area(rootFolder.getArea())
                .parentId(parentId)
                .visibility(rootFolder.getVisibility())
                .published(rootFolder.getPublished())
                .contentType(contentType)
                .portalPageContentId(createdContent.getId())
                .build()
        );
    }

    /**
     * The subtree mirrors the remote listing: items imported by a previous run and no longer backed
     * by a remote file are removed. An untouched item's descendants are all untouched (a touched one
     * would have marked its whole ancestor chain), so removing the top-most untouched items suffices.
     */
    private void deleteOrphans(PortalNavigationFolder rootFolder, Set<PortalNavigationItemId> touchedItemIds) {
        var itemsToVisit = new ArrayList<>(queryService.findByParentIdAndEnvironmentId(rootFolder.getEnvironmentId(), rootFolder.getId()));
        while (!itemsToVisit.isEmpty()) {
            var item = itemsToVisit.removeFirst();
            if (!touchedItemIds.contains(item.getId())) {
                itemDomainService.deleteWithDescendants(item);
            } else if (item instanceof PortalNavigationFolder) {
                itemsToVisit.addAll(queryService.findByParentIdAndEnvironmentId(rootFolder.getEnvironmentId(), item.getId()));
            }
        }
    }

    private static String unsupportedDocumentError(String sourcePath) {
        return "Cannot determine the type of %s: expected a .md file, or a document declaring a root \"openapi\", \"swagger\" or \"asyncapi\" property.".formatted(
            sourcePath
        );
    }

    private static String baseNameOf(String filePath) {
        var fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        var extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex <= 0 ? fileName : fileName.substring(0, extensionIndex);
    }

    private static String parentPathOf(String filePath) {
        var separatorIndex = filePath.lastIndexOf('/');
        return separatorIndex <= 0 ? "" : filePath.substring(0, separatorIndex);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ImportEntry(String sourcePath, String title, String destinationPath) {}

    /** The import entries plus where they come from: a manifest parsing to zero pages is its own failure mode */
    private record ResolvedEntries(List<ImportEntry> entries, boolean fromManifest) {}

    public record BulkImportResult(List<FileImportResult> files) {
        public static BulkImportResult listingFailure(String rootTitle, String error) {
            return new BulkImportResult(List.of(FileImportResult.failure(rootTitle, error)));
        }

        public record FileImportResult(@Nullable UUID navigationItemId, String title, boolean success, @Nullable String error) {
            static FileImportResult success(UUID navigationItemId, String title) {
                return new FileImportResult(navigationItemId, title, true, null);
            }

            static FileImportResult failure(String title, String error) {
                return new FileImportResult(null, title, false, error);
            }
        }
    }
}
