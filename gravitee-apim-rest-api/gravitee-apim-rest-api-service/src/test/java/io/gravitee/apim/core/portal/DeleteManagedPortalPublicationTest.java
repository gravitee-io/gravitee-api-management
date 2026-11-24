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
package io.gravitee.apim.core.portal;

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE12_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static fixtures.core.model.PortalNavigationItemFixtures.anApi;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalFixtures;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalAddressDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal.use_case.DeleteManagedPortalUseCase;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.ListApiDocumentationUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeleteManagedPortalPublicationTest {

    private static final PortalId TARGET = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId OTHER = PortalId.of("22222222-2222-2222-2222-222222222222");
    private final PortalCrudServiceInMemory portals = new PortalCrudServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navigation = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory query = new PortalNavigationItemsQueryServiceInMemory(navigation.storage());
    private final PortalPageContentCrudServiceInMemory contents = new PortalPageContentCrudServiceInMemory();
    private final PortalListingCrudServiceInMemory listings = new PortalListingCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory contentQuery = PortalPageContentQueryServiceInMemory.sharing(contents.storage());
    private final ApiOwnedNavigationDomainService apiNavigation = new ApiOwnedNavigationDomainService(query, navigation);
    private final DeleteManagedPortalUseCase useCase = new DeleteManagedPortalUseCase(
        portals,
        query,
        navigation,
        contents,
        contentQuery,
        apiNavigation,
        listings,
        new PortalAddressDomainServiceInMemory(portals, new InstallationAccessQueryServiceInMemory())
    );

    @Test
    void should_preserve_api_documentation_and_other_portal_listing() {
        portals.initWith(List.of(PortalFixtures.aPortal(TARGET, ENV_ID, ORG_ID, "Target")));
        var targetListing = anApi(PAGE_ID, "API", null, "api");
        var section = aFolder("other section").toBuilder().reference(new NavigationItemReference.PortalReference(OTHER)).build();
        var otherListing = PortalNavigationApi.builder()
            .id(PortalNavigationItemId.of(PAGE11_ID))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(OTHER))
            .title("API")
            .segment("api")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId("api")
            .parentId(section.getId())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        var apiPage = aPage(PAGE12_ID, "docs", null).toBuilder().reference(new NavigationItemReference.ApiReference("api")).build();
        navigation.initWith(List.of(targetListing, section, otherListing, apiPage));

        useCase.execute(new DeleteManagedPortalUseCase.Input(ENV_ID, TARGET));

        assertThat(navigation.storage()).containsExactly(section, otherListing, apiPage);
        assertThat(apiPage.getPublished()).isTrue();
        var publications = new ListApiDocumentationUseCase(apiNavigation, query, new PortalNavigationItemSourceDomainServiceInMemory())
            .execute(new ListApiDocumentationUseCase.Input(ENV_ID, "api"))
            .publications();
        assertThat(publications).extracting(ListApiDocumentationUseCase.Publication::portalId).containsExactly(OTHER);
    }
}
