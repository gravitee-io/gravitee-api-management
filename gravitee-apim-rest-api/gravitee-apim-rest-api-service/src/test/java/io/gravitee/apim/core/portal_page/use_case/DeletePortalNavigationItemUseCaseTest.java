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

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DeletePortalNavigationItemUseCaseTest {

    private final PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService =
        new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService =
        new PortalNavigationItemsQueryServiceInMemory(portalNavigationItemsCrudService.storage());
    private final PortalPageContentCrudServiceInMemory portalPageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private DeletePortalNavigationItemUseCase deletePortalNavigationItemUseCase;

    @BeforeEach
    void setUp() {
        apiCrudService.initWith(List.of(Api.builder().id("api-id").environmentId(PortalNavigationItemFixtures.ENV_ID).build()));
        var domainService = new PortalNavigationItemDomainService(
            portalNavigationItemsCrudService,
            portalNavigationItemsQueryService,
            portalPageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(portalPageContentCrudService.storage()),
            apiCrudService,
            new PortalNavigationItemSourceDomainServiceInMemory(),
            new ApiOwnedNavigationDomainService(portalNavigationItemsQueryService, portalNavigationItemsCrudService),
            new NavigationPortalDomainServiceInMemory()
        );

        deletePortalNavigationItemUseCase = new DeletePortalNavigationItemUseCase(
            PortalNavigationUseCaseTestFixtures.access(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                portalNavigationItemsQueryService,
                PortalPageContentQueryServiceInMemory.sharing(portalPageContentCrudService.storage()),
                apiCrudService
            ),
            domainService,
            new PortalNavigationSourcedItemsDomainService(portalNavigationItemsQueryService)
        );
    }

    @AfterEach
    void afterEach() {
        portalNavigationItemsCrudService.reset();
        portalNavigationItemsQueryService.reset();
        portalPageContentCrudService.reset();
        apiCrudService.reset();
    }

    @Test
    void should_delete_an_accessible_page_whose_content_is_already_missing() {
        var page = PortalNavigationItemFixtures.aPage("Broken page", null);
        page.markAsRoot();
        givenNavigation(List.of(page));
        portalPageContentCrudService.reset();

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(page.getOrganizationId(), page.getEnvironmentId(), page.getId(), PORTAL_REFERENCE)
        );

        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_a_folder_with_existing_and_missing_child_content() {
        var folder = PortalNavigationItemFixtures.aFolder("Broken documentation");
        folder.markAsRoot();
        var missing = PortalNavigationItemFixtures.aPage("Missing", folder.getId());
        var existing = PortalNavigationItemFixtures.aPage("Existing", folder.getId());
        missing.updateParent(folder);
        existing.updateParent(folder);
        givenNavigation(List.of(folder, missing, existing));
        portalPageContentCrudService.delete(missing.getPortalPageContentId());

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                folder.getOrganizationId(),
                folder.getEnvironmentId(),
                folder.getId(),
                PORTAL_REFERENCE
            )
        );

        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_foreign_associations_even_when_content_is_missing() {
        var own = PortalNavigationItemFixtures.aPage("Own", null);
        var foreign = PortalNavigationItemFixtures.aPage("Foreign", null)
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")))
            .portalPageContentId(own.getPortalPageContentId())
            .build();
        givenNavigation(List.of(own, foreign));
        portalPageContentCrudService.reset();

        assertThatThrownBy(() ->
            deletePortalNavigationItemUseCase.execute(
                new DeletePortalNavigationItemUseCase.Input(own.getOrganizationId(), own.getEnvironmentId(), own.getId(), PORTAL_REFERENCE)
            )
        ).isInstanceOf(PageContentNotFoundException.class);

        assertThat(portalNavigationItemsCrudService.storage()).containsExactly(own, foreign);
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_a_foreign_portal_item_in_the_same_organization_and_environment() {
        var foreign = PortalNavigationItemFixtures.aPage("Foreign", null)
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")))
            .build();
        givenNavigation(List.of(foreign));

        assertThatThrownBy(() ->
            deletePortalNavigationItemUseCase.execute(
                new DeletePortalNavigationItemUseCase.Input(
                    foreign.getOrganizationId(),
                    foreign.getEnvironmentId(),
                    foreign.getId(),
                    PORTAL_REFERENCE
                )
            )
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);

        assertThat(portalNavigationItemsCrudService.storage()).containsExactly(foreign);
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_delete() {
        // Given
        var toDelete = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null);
        toDelete.markAsRoot();
        givenNavigation(List.of(toDelete));

        // When
        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                toDelete.getId(),
                toDelete.getReference()
            )
        );

        // Then
        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_an_api_owned_folder_with_its_contents_and_nothing_else_the_api_owns() {
        var apiReference = new NavigationItemReference.ApiReference("api-id");
        var folder = PortalNavigationItemFixtures.aFolder("Auth").toBuilder().reference(apiReference).order(0).build();
        folder.markAsRoot();
        var nestedPage = PortalNavigationItemFixtures.aPage("Setup", folder.getId()).toBuilder().reference(apiReference).build();
        nestedPage.updateParent(folder);
        var otherPage = PortalNavigationItemFixtures.aPage("Overview", null).toBuilder().reference(apiReference).order(1).build();
        otherPage.markAsRoot();
        var portalSection = PortalNavigationItemFixtures.aFolder("APIs").toBuilder().order(0).build();
        portalSection.markAsRoot();
        givenNavigation(List.of(folder, nestedPage, otherPage, portalSection));

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                folder.getId(),
                folder.getReference()
            )
        );

        assertThat(portalNavigationItemsCrudService.storage())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(otherPage.getId(), portalSection.getId());
        // Only the API's own siblings close the gap: the portal section keeps its position
        assertThat(otherPage.getOrder()).isZero();
        assertThat(portalSection.getOrder()).isZero();
    }

    @Test
    void should_reorder_when_delete() {
        // Given
        PortalNavigationPage page1 = PortalNavigationItemFixtures.aPage("p1", null).toBuilder().order(1).build();
        page1.markAsRoot();
        PortalNavigationPage page2 = PortalNavigationItemFixtures.aPage("p2", null).toBuilder().order(2).build();
        page2.markAsRoot();
        PortalNavigationPage page3 = PortalNavigationItemFixtures.aPage("p3", null).toBuilder().order(3).build();
        page3.markAsRoot();
        PortalNavigationPage page4 = PortalNavigationItemFixtures.aPage("p4", null).toBuilder().order(4).build();
        page4.markAsRoot();
        PortalNavigationPage page5 = PortalNavigationItemFixtures.aPage("p5", null).toBuilder().order(5).build();
        page5.markAsRoot();
        givenNavigation(List.of(page1, page2, page3, page4, page5));

        // When
        // perform delete
        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                page2.getId(),
                page2.getReference()
            )
        );

        // Then
        assertThat(portalNavigationItemsCrudService.storage()).hasSize(4);
        var expected = Map.of(page1.getId(), 1, page3.getId(), 2, page4.getId(), 3, page5.getId(), 4);
        assertThat(
            portalNavigationItemsCrudService
                .storage()
                .stream()
                .collect(Collectors.toMap(PortalNavigationItem::getId, PortalNavigationItem::getOrder))
        )
            .containsOnlyKeys(expected.keySet())
            .containsAllEntriesOf(expected);
    }

    @Test
    void should_throw_not_found_exception_when_item_to_delete_does_not_exist() {
        // Given
        var toKeep = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null);
        givenNavigation(List.of(toKeep));

        // When
        var throwable = Assertions.catchThrowable(() ->
            deletePortalNavigationItemUseCase.execute(
                new DeletePortalNavigationItemUseCase.Input(
                    PortalNavigationItemFixtures.ORG_ID,
                    PortalNavigationItemFixtures.ENV_ID,
                    PortalNavigationItemId.of(PortalNavigationItemFixtures.PAGE12_ID),
                    PORTAL_REFERENCE
                )
            )
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found")
            .extracting(ex -> ((PortalNavigationItemNotFoundException) ex).getId())
            .isEqualTo(PortalNavigationItemFixtures.PAGE12_ID);
    }

    @Test
    void should_delete_page_with_content() {
        // Given
        var pageContent = portalPageContentCrudService.createDefault(
            PortalNavigationItemFixtures.ORG_ID,
            PortalNavigationItemFixtures.ENV_ID,
            PortalPageContentType.GRAVITEE_MARKDOWN
        );
        var toDelete = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null)
            .toBuilder()
            .portalPageContentId(pageContent.getId())
            .build();
        toDelete.markAsRoot();
        givenNavigation(List.of(toDelete));
        portalPageContentCrudService.initWith(List.of(pageContent));

        // When
        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                toDelete.getId(),
                toDelete.getReference()
            )
        );

        // Then
        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_folder_and_its_children() {
        var folder = PortalNavigationItemFixtures.aFolder("50000000-0000-4000-8000-000000000001", "Folder");
        folder.markAsRoot();
        var child = PortalNavigationItemFixtures.aPage("50000000-0000-4000-8000-000000000002", "Child", folder.getId());
        child.updateParent(folder);
        givenNavigation(List.of(folder, child));

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                folder.getId(),
                folder.getReference()
            )
        );

        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_a_sourced_page_without_removing_its_source_first() {
        var sourcedPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null)
            .toBuilder()
            .source(aSource())
            .build();
        sourcedPage.markAsRoot();
        givenNavigation(List.of(sourcedPage));

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                sourcedPage.getId(),
                sourcedPage.getReference()
            )
        );

        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_an_imported_folder_with_everything_it_holds() {
        var importedFolder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemFixtures.FOLDER_ID, "Imported Folder")
            .toBuilder()
            .source(aSource().toBuilder().subtreeImport(true).build())
            .build();
        importedFolder.markAsRoot();
        var child = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "Child Page", importedFolder.getId());
        child.updateParent(importedFolder);
        givenNavigation(List.of(importedFolder, child));

        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                importedFolder.getId(),
                importedFolder.getReference()
            )
        );

        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_delete_of_child_of_sourced_folder() {
        var sourcedFolder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemFixtures.FOLDER_ID, "Sourced Folder")
            .toBuilder()
            .source(aSource())
            .build();
        sourcedFolder.markAsRoot();
        var child = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "Child Page", sourcedFolder.getId());
        givenNavigation(List.of(sourcedFolder, child));

        var error = org.junit.jupiter.api.Assertions.assertThrows(
            io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException.class,
            () ->
                deletePortalNavigationItemUseCase.execute(
                    new DeletePortalNavigationItemUseCase.Input(
                        PortalNavigationItemFixtures.ORG_ID,
                        PortalNavigationItemFixtures.ENV_ID,
                        child.getId(),
                        child.getReference()
                    )
                )
        );

        assertThat(error).hasMessageContaining("read-only");
        assertThat(portalNavigationItemsCrudService.storage()).hasSize(2);
    }

    private void givenNavigation(List<PortalNavigationItem> items) {
        portalNavigationItemsCrudService.initWith(items);
        portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));
        items
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .filter(page ->
                portalPageContentCrudService
                    .storage()
                    .stream()
                    .noneMatch(content -> content.getId().equals(page.getPortalPageContentId()))
            )
            .forEach(page ->
                portalPageContentCrudService.create(
                    new GraviteeMarkdownPageContent(
                        page.getPortalPageContentId(),
                        page.getOrganizationId(),
                        page.getEnvironmentId(),
                        GraviteeMarkdown.of("# Documentation")
                    )
                )
            );
    }

    private static io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource aSource() {
        return io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}")
            .build();
    }
}
