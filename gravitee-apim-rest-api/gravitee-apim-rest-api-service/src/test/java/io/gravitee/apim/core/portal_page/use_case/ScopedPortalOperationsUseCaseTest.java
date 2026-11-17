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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static fixtures.core.model.PortalPageContentFixtures.aGraviteeMarkdownPageContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationBulkImportDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ScopedPortalOperationsUseCaseTest {

    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final NavigationItemReference.PortalReference PORTAL = new NavigationItemReference.PortalReference(PORTAL_ID);
    private static final NavigationItemReference.PortalReference FOREIGN_PORTAL = new NavigationItemReference.PortalReference(
        PortalId.of("22222222-2222-2222-2222-222222222222")
    );
    private static final NavigationItemReference.ApiReference API = new NavigationItemReference.ApiReference("api-a");

    private final PortalNavigationItemsQueryServiceInMemory navigation = new PortalNavigationItemsQueryServiceInMemory();
    private final PortalPageContentQueryServiceInMemory contents = new PortalPageContentQueryServiceInMemory();
    private final PortalCrudService portals = mock(PortalCrudService.class);
    private final ApiCrudService apis = mock(ApiCrudService.class);
    private final PortalNavigationItemDomainService writes = mock(PortalNavigationItemDomainService.class);
    private final PortalNavigationItemSourceDomainService source = mock(PortalNavigationItemSourceDomainService.class);
    private final PortalNavigationBulkImportDomainService imports = mock(PortalNavigationBulkImportDomainService.class);
    private final PortalNavigationAccessDomainService access = new PortalNavigationAccessDomainService(
        portals,
        apis,
        navigation,
        contents,
        new ApiOwnedNavigationDomainService(navigation, mock(PortalNavigationItemCrudService.class)),
        mock(ApiQueryService.class)
    );
    private final PortalNavigationSourcedItemsDomainService sourcedItems = new PortalNavigationSourcedItemsDomainService(navigation);
    private final DeletePortalNavigationItemUseCase delete = new DeletePortalNavigationItemUseCase(access, writes, sourcedItems);
    private final FetchPortalNavigationItemUseCase fetch = new FetchPortalNavigationItemUseCase(
        access,
        sourcedItems,
        writes,
        source,
        imports
    );

    @BeforeEach
    void set_up() throws Exception {
        when(portals.findByIdAndEnvironmentId(PORTAL_ID, ENV_ID)).thenReturn(
            Optional.of(Portal.create(PORTAL_ID, ENV_ID, ORG_ID, "Portal", URI.create("https://portal.example.test").toURL()))
        );
        when(apis.findById(API.apiId())).thenReturn(Optional.of(Api.builder().id(API.apiId()).environmentId(ENV_ID).build()));
    }

    @Test
    void should_not_delete_an_item_of_another_portal() {
        var page = aPage("Foreign", null).toBuilder().reference(FOREIGN_PORTAL).build();
        navigation.initWith(List.of(page));

        assertThatThrownBy(() -> delete(page, PORTAL)).isInstanceOf(PortalNavigationItemNotFoundException.class);
        verifyNoInteractions(writes);
    }

    @Test
    void should_not_delete_a_subtree_containing_another_portals_item() {
        var folder = aFolder("Root").toBuilder().reference(PORTAL).build();
        var foreign = aPage("Foreign", folder.getId()).toBuilder().reference(FOREIGN_PORTAL).build();
        navigation.initWith(List.of(folder, foreign));

        assertThatThrownBy(() -> delete(folder, PORTAL)).isInstanceOf(PortalNavigationItemNotFoundException.class);
        verifyNoInteractions(writes);
    }

    @Test
    void should_not_delete_content_referenced_by_another_portal() {
        var page = aPage("Own", null).toBuilder().reference(PORTAL).build();
        var foreign = aPage("Foreign", null)
            .toBuilder()
            .reference(FOREIGN_PORTAL)
            .portalPageContentId(page.getPortalPageContentId())
            .build();
        navigation.initWith(List.of(page, foreign));
        contents.initWith(List.of(aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, ENV_ID, "Shared")));

        assertThatThrownBy(() -> delete(page, PORTAL)).isInstanceOf(PageContentNotFoundException.class);
        verifyNoInteractions(writes);
    }

    @ParameterizedTest
    @MethodSource("scopes")
    void should_delete_an_accessible_page_with_its_content(NavigationItemReference scope) {
        var page = aPage("Own", null).toBuilder().reference(scope).build();
        navigation.initWith(List.of(page));
        contents.initWith(List.of(aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, ENV_ID, "Own")));

        delete(page, scope);

        verify(writes).deleteWithDescendants(page);
    }

    @Test
    void should_not_fetch_another_portals_page() {
        var page = aPage("Foreign", null).toBuilder().reference(FOREIGN_PORTAL).source(aSource()).build();
        navigation.initWith(List.of(page));

        assertThatThrownBy(() -> fetch(page, PORTAL)).isInstanceOf(PortalNavigationItemNotFoundException.class);
        verifyNoInteractions(writes, source, imports);
    }

    @Test
    void should_validate_the_fetch_callers_organization_before_any_effect() {
        var page = aPage("API guide", null).toBuilder().reference(API).source(aSource()).build();
        navigation.initWith(List.of(page));

        assertThatThrownBy(() ->
            fetch.execute(new FetchPortalNavigationItemUseCase.Input("other-org", ENV_ID, page.getId().json(), API))
        ).isInstanceOf(PortalNavigationItemNotFoundException.class);
        verifyNoInteractions(writes, source, imports);
    }

    @Test
    void should_validate_all_contents_before_fetching_the_first_page() {
        var folder = aFolder("Root").toBuilder().reference(PORTAL).build();
        var first = aPage("First", folder.getId()).toBuilder().reference(PORTAL).source(aSource()).build();
        var second = aPage("Second", folder.getId()).toBuilder().reference(PORTAL).source(aSource()).build();
        var foreign = aPage("Foreign", null)
            .toBuilder()
            .reference(FOREIGN_PORTAL)
            .portalPageContentId(second.getPortalPageContentId())
            .build();
        navigation.initWith(List.of(folder, first, second, foreign));
        contents.initWith(
            List.of(
                aGraviteeMarkdownPageContent(first.getPortalPageContentId(), ORG_ID, ENV_ID, "First"),
                aGraviteeMarkdownPageContent(second.getPortalPageContentId(), ORG_ID, ENV_ID, "Second")
            )
        );

        assertThatThrownBy(() -> fetch(folder, PORTAL)).isInstanceOf(PageContentNotFoundException.class);
        verifyNoInteractions(writes, source, imports);
    }

    @Test
    void should_validate_contents_before_reimporting_a_folder() {
        var folder = aFolder("Import").toBuilder().reference(PORTAL).source(aSource().toBuilder().subtreeImport(true).build()).build();
        var page = aPage("Imported", folder.getId()).toBuilder().reference(PORTAL).build();
        navigation.initWith(List.of(folder, page));
        contents.initWith(List.of(aGraviteeMarkdownPageContent(page.getPortalPageContentId(), "other-org", ENV_ID, "Foreign")));

        assertThatThrownBy(() -> fetch(folder, PORTAL)).isInstanceOf(PageContentNotFoundException.class);
        verifyNoInteractions(writes, source, imports);
    }

    @ParameterizedTest
    @MethodSource("scopes")
    void should_fetch_an_accessible_page(NavigationItemReference scope) {
        var page = aPage("Own", null).toBuilder().reference(scope).source(aSource()).build();
        navigation.initWith(List.of(page));
        contents.initWith(List.of(aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, ENV_ID, "Own")));
        when(writes.fetchPageContent(page)).thenReturn(page);

        assertThat(fetch(page, scope).item()).isSameAs(page);
        verify(writes).fetchPageContent(page);
    }

    @Test
    void should_fetch_shared_api_documentation_reachable_from_the_selected_portal() {
        var section = aFolder("APIs").toBuilder().reference(PORTAL).build();
        var listing = apiListing(section.getId(), PORTAL);
        var page = aPage("API guide", null).toBuilder().reference(API).source(aSource()).build();
        navigation.initWith(List.of(section, listing, page));
        contents.initWith(List.of(aGraviteeMarkdownPageContent(page.getPortalPageContentId(), ORG_ID, ENV_ID, "Own")));
        when(writes.fetchPageContent(page)).thenReturn(page);

        assertThat(fetch(page, PORTAL).item()).isSameAs(page);
        verify(writes).fetchPageContent(page);
    }

    private void delete(PortalNavigationItem item, NavigationItemReference scope) {
        delete.execute(new DeletePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, item.getId(), scope));
    }

    private FetchPortalNavigationItemUseCase.Output fetch(PortalNavigationItem item, NavigationItemReference scope) {
        return fetch.execute(new FetchPortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, item.getId().json(), scope));
    }

    private static Stream<NavigationItemReference> scopes() {
        return Stream.of(PORTAL, API);
    }

    private static PortalNavigationItemSource aSource() {
        return PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build();
    }

    private static PortalNavigationApi apiListing(PortalNavigationItemId parentId, NavigationItemReference reference) {
        var listing = anApi(PortalNavigationItemId.random().json(), "API", parentId, API.apiId());
        listing.changeOwner(reference);
        return listing;
    }
}
