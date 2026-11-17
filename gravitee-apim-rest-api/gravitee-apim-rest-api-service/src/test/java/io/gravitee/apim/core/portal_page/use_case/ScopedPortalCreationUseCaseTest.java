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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalNavigationManifestParserInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationBulkImportDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemRenderingDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopedPortalCreationUseCaseTest {

    private static final String ORGANIZATION = "org-id";
    private static final String ENVIRONMENT = "env-id";
    private static final String API_ID = "api-id";

    private final List<PortalNavigationItem> navigationStorage = new ArrayList<>();
    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory(navigationStorage);
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final NavigationPortalDomainServiceInMemory portals = new NavigationPortalDomainServiceInMemory();
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalPageContentCrudServiceInMemory contentCrudService;
    private PortalNavigationItemSourceDomainServiceInMemory sourceService;
    private CreatePortalNavigationItemUseCase create;
    private BulkCreatePortalNavigationItemUseCase bulk;
    private ImportPortalNavigationUseCase importNavigation;
    private SeedDefaultPagesForPortalNavigationItemsUseCase seed;
    private PortalId defaultPortalId;
    private PortalId selectedPortalId;
    private NavigationItemReference.PortalReference selectedScope;

    @BeforeEach
    void initialize() {
        defaultPortalId = portals.givenDefaultPortal(ORGANIZATION, ENVIRONMENT);
        selectedPortalId = portals.givenAutomationPortal(ORGANIZATION, ENVIRONMENT);
        selectedScope = new NavigationItemReference.PortalReference(selectedPortalId);
        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("API").environmentId(ENVIRONMENT).build()));
        crudService = spy(new PortalNavigationItemsCrudServiceInMemory(navigationStorage));
        contentCrudService = spy(new PortalPageContentCrudServiceInMemory());
        sourceService = spy(new PortalNavigationItemSourceDomainServiceInMemory());
        var contentQuery = PortalPageContentQueryServiceInMemory.sharing(contentCrudService.storage());
        var apiOwned = new ApiOwnedNavigationDomainService(queryService, crudService);
        var access = new PortalNavigationAccessDomainService(
            portals.portalCrudService(),
            apiCrudService,
            queryService,
            contentQuery,
            apiOwned,
            new ApiQueryServiceInMemory(apiCrudService)
        );
        var apiProducts = new ApiProductQueryServiceInMemory();
        var domain = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            contentCrudService,
            contentQuery,
            apiCrudService,
            sourceService,
            apiOwned,
            portals
        );
        var validator = new PortalNavigationItemValidatorService(queryService, contentQuery, apiProducts, sourceService, apiCrudService);
        var expansion = new PortalNavigationItemCreationExpansionDomainService(apiProducts, apiCrudService, queryService);
        var rendering = new PortalNavigationItemRenderingDomainService(queryService);
        var defaults = new PortalNavigationDefaultPageDomainService(
            queryService,
            domain,
            contentCrudService,
            apiCrudService,
            expansion,
            validator,
            apiOwned
        );
        var importer = new PortalNavigationBulkImportDomainService(
            sourceService,
            new PortalNavigationManifestParserInMemory(),
            domain,
            queryService,
            crudService,
            contentCrudService,
            contentQuery,
            portals.portalCrudService()
        );
        create = new CreatePortalNavigationItemUseCase(access, apiOwned, domain, validator, expansion, defaults, sourceService, rendering);
        bulk = new BulkCreatePortalNavigationItemUseCase(access, domain, validator, expansion, defaults, sourceService, rendering);
        importNavigation = new ImportPortalNavigationUseCase(
            access,
            validator,
            domain,
            sourceService,
            importer,
            queryService,
            expansion,
            rendering
        );
        seed = new SeedDefaultPagesForPortalNavigationItemsUseCase(access, defaults);
        clearInvocations(crudService, contentCrudService, sourceService);
    }

    @Test
    void should_refuse_create_under_a_foreign_parent_before_any_write_or_fetch() {
        var foreign = folder(defaultPortalId, "Foreign");
        navigationStorage.add(foreign);

        assertThatThrownBy(() ->
            create.execute(
                new CreatePortalNavigationItemUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    candidate(PortalNavigationItemType.PAGE, foreign.getId()),
                    selectedScope
                )
            )
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);

        verifyNoInteractions(crudService, contentCrudService, sourceService);
        assertThat(navigationStorage).containsExactly(foreign);
    }

    @Test
    void should_refuse_a_mixed_bulk_before_creating_its_first_valid_item() {
        var foreign = folder(defaultPortalId, "Foreign");
        navigationStorage.add(foreign);
        var first = candidate(PortalNavigationItemType.FOLDER, null);
        var second = candidate(PortalNavigationItemType.PAGE, foreign.getId());

        assertThatThrownBy(() ->
            bulk.execute(
                new BulkCreatePortalNavigationItemUseCase.Input(ORGANIZATION, ENVIRONMENT, List.of(first, second), selectedPortalId)
            )
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);

        verifyNoInteractions(crudService, contentCrudService, sourceService);
        assertThat(navigationStorage).containsExactly(foreign);
    }

    @Test
    void should_refuse_import_under_a_foreign_parent_before_fetching_or_creating_content() {
        var foreign = folder(defaultPortalId, "Foreign");
        navigationStorage.add(foreign);
        var source = PortalNavigationItemSource.builder().sourceType("git-fetcher").sourceConfiguration("{}").build();
        var input = ImportPortalNavigationUseCase.Input.builder()
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title("Imported")
            .parentId(foreign.getId())
            .source(source)
            .scope(selectedScope)
            .build();

        assertThatThrownBy(() -> importNavigation.execute(input)).isInstanceOf(PortalNavigationItemNotFoundException.class);

        verifyNoInteractions(crudService, contentCrudService, sourceService);
        assertThat(source.isSubtreeImport()).isFalse();
    }

    @Test
    void should_validate_all_seed_ids_before_creating_a_page_for_the_first_item() {
        var section = folder(selectedPortalId, "Section");
        var apiListing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "API", section.getId(), API_ID);
        apiListing.setOrganizationId(ORGANIZATION);
        apiListing.setEnvironmentId(ENVIRONMENT);
        apiListing.changeOwner(selectedScope);
        apiListing.updateParent(section);
        var foreign = folder(defaultPortalId, "Foreign");
        navigationStorage.addAll(List.of(section, apiListing, foreign));

        assertThatThrownBy(() ->
            seed.execute(
                new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    List.of(apiListing.getId(), foreign.getId()),
                    selectedPortalId
                )
            )
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);

        verifyNoInteractions(crudService, contentCrudService, sourceService);
        assertThat(navigationStorage).hasSize(3);
    }

    @Test
    void should_create_a_root_for_the_selected_portal_without_resolving_the_default() {
        var result = create.execute(
            new CreatePortalNavigationItemUseCase.Input(
                ORGANIZATION,
                ENVIRONMENT,
                candidate(PortalNavigationItemType.FOLDER, null),
                selectedScope
            )
        );

        assertThat(result.item().getReference()).isEqualTo(selectedScope);
        assertThat(navigationStorage).hasSize(1);
        assertThat(navigationStorage.getFirst().getReference()).isEqualTo(selectedScope);
        assertThat(portals.resolveCalls()).isZero();
    }

    @Test
    void should_create_a_bulk_parent_and_page_for_the_selected_portal() {
        var parent = candidate(PortalNavigationItemType.FOLDER, null);
        var page = candidate(PortalNavigationItemType.PAGE, parent.getId());

        var result = bulk.execute(
            new BulkCreatePortalNavigationItemUseCase.Input(ORGANIZATION, ENVIRONMENT, List.of(parent, page), selectedPortalId)
        );

        assertThat(result.items())
            .hasSize(2)
            .allSatisfy(item -> assertThat(item.getReference()).isEqualTo(selectedScope));
        assertThat(navigationStorage)
            .hasSize(2)
            .allSatisfy(item -> assertThat(item.getReference()).isEqualTo(selectedScope));
        assertThat(contentCrudService.storage()).hasSize(1);
        assertThat(portals.resolveCalls()).isZero();
    }

    @Test
    void should_normalize_api_documentation_created_through_the_selected_portal() {
        var section = folder(selectedPortalId, "Section");
        var listing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "API", section.getId(), API_ID);
        listing.setOrganizationId(ORGANIZATION);
        listing.setEnvironmentId(ENVIRONMENT);
        listing.changeOwner(selectedScope);
        listing.updateParent(section);
        navigationStorage.addAll(List.of(section, listing));

        var result = create.execute(
            new CreatePortalNavigationItemUseCase.Input(
                ORGANIZATION,
                ENVIRONMENT,
                candidate(PortalNavigationItemType.PAGE, listing.getId()),
                selectedScope
            )
        );

        var saved = queryService.findByIdAndEnvironmentId(ENVIRONMENT, result.item().getId());
        assertThat(saved.getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
        assertThat(saved.getParentId()).isNull();
        assertThat(result.item().getParentId()).isEqualTo(listing.getId());
        assertThat(contentCrudService.storage()).hasSize(1);
        assertThat(portals.resolveCalls()).isZero();
    }

    private PortalNavigationFolder folder(PortalId portalId, String title) {
        var folder = PortalNavigationItemFixtures.aFolder(title)
            .toBuilder()
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .build();
        folder.markAsRoot();
        return folder;
    }

    private CreatePortalNavigationItem candidate(PortalNavigationItemType type, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(type)
            .title("New item")
            .area(PortalArea.TOP_NAVBAR)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
    }
}
