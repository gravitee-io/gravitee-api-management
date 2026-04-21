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

import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListPortalNavigationItemsUseCaseTest {

    private static final String ENV_ID = "env-id";

    private ListPortalNavigationItemsUseCase useCase;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private SubscriptionQueryServiceInMemory subscriptionQueryService;
    private ApiQueryServiceInMemory apiQueryService;

    @BeforeEach
    void setUp() {
        queryService = new PortalNavigationItemsQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        subscriptionQueryService = new SubscriptionQueryServiceInMemory();
        apiQueryService = new ApiQueryServiceInMemory();
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            apiQueryService
        );
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(queryService, apiMembershipDomainService);
        useCase = new ListPortalNavigationItemsUseCase(queryService, apiVisibilityDomainService);

        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_return_top_level_navigation_items_when_parent_id_is_null_and_load_children_is_true() {
        // Given
        // Setup data for APIs, Guides, Support

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        // Then
        assertThat(result.items())
            .hasSize(13)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly(
                "APIs",
                "Example Link",
                "Guides",
                "Support",
                "Overview",
                "Getting Started",
                "Category1",
                "API 1",
                "page11",
                "API 2 Folder",
                "page12",
                "API 1 Folder",
                "API 2"
            );
    }

    @Test
    void should_return_direct_children_when_parent_id_is_apis_and_load_children_is_false() {
        // Given
        // Setup data

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(PortalNavigationItemId.of(APIS_ID)),
                false,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        // Then
        assertThat(result.items())
            .hasSize(4)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("Overview", "Getting Started", "Category1", "API 1");
    }

    @Test
    void should_return_direct_children_and_grandchildren_when_parent_id_is_apis_and_load_children_is_true() {
        // Given
        // Setup data

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(PortalNavigationItemId.of(APIS_ID)),
                true,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        // Then
        assertThat(result.items())
            .hasSize(9)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly(
                "Overview",
                "Getting Started",
                "Category1",
                "API 1",
                "page11",
                "API 2 Folder",
                "page12",
                "API 1 Folder",
                "API 2"
            );
    }

    @Test
    void should_only_return_valid_published_items_if_only_visible_in_portal_is_true() {
        // Given
        // Setup data

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forPortal(true)
            )
        );

        // Then
        assertThat(result.items())
            .hasSize(12)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly(
                "APIs",
                "Example Link",
                "Guides",
                "Support",
                "Overview",
                "Getting Started",
                "Category1",
                "API 1",
                "API 2 Folder",
                "page12",
                "API 1 Folder",
                "API 2"
            );
    }

    @Test
    void should_not_return_published_item_if_parent_is_unpublished_when_parent_id_null() {
        // Given
        var unpublishedFolder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "Unpublished Folder");
        unpublishedFolder.setPublished(false);

        var publishedChildPage = PortalNavigationItemFixtures.aPage(
            PortalNavigationItemId.random().toString(),
            "Published Child Page",
            unpublishedFolder.getId()
        );
        publishedChildPage.setPublished(true);

        queryService.initWith(List.of(unpublishedFolder, publishedChildPage));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forPortal(true)
            )
        );

        // Then
        assertThat(result.items()).hasSize(0);
    }

    @Test
    void should_not_return_published_child_when_parent_is_unpublished_and_parent_id_is_defined() {
        // Given
        var unpublishedFolder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "Unpublished Folder");
        unpublishedFolder.setPublished(false);

        var publishedChildPage = PortalNavigationItemFixtures.aPage(
            PortalNavigationItemId.random().toString(),
            "Published Child Page",
            unpublishedFolder.getId()
        );
        publishedChildPage.setPublished(true);

        queryService.initWith(List.of(unpublishedFolder, publishedChildPage));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(unpublishedFolder.getId()),
                true,
                PortalNavigationItemViewerContext.forPortal(true)
            )
        );

        // Then
        assertThat(result.items()).hasSize(0);
    }

    @Test
    void should_return_published_children_when_parent_is_published_and_parent_id_is_defined() {
        // Given
        var publishedFolder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "Published Folder");
        publishedFolder.setPublished(true);

        var publishedChildPage = PortalNavigationItemFixtures.aPage(
            PortalNavigationItemId.random().toString(),
            "Published Child Page",
            publishedFolder.getId()
        );
        publishedChildPage.setPublished(true);

        queryService.initWith(List.of(publishedFolder, publishedChildPage));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(publishedFolder.getId()),
                true,
                PortalNavigationItemViewerContext.forPortal(true)
            )
        );

        // Then
        assertThat(result.items()).hasSize(1).extracting(PortalNavigationItem::getTitle).containsExactly("Published Child Page");
    }

    @Test
    void should_empty_list_when_parent_id_defined_but_not_found() {
        // Given
        // Setup data

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(PortalNavigationItemId.random()),
                true,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        // Then
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_return_empty_list_when_parent_is_found_and_not_visible_in_portal() {
        // Given
        var unpublishedFolder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().toString(), "Unpublished Folder");
        unpublishedFolder.setPublished(false);

        var publishedChildPage = PortalNavigationItemFixtures.aPage(
            PortalNavigationItemId.random().toString(),
            "Published Child Page",
            unpublishedFolder.getId()
        );

        queryService.initWith(List.of(unpublishedFolder, publishedChildPage));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(unpublishedFolder.getId()),
                true,
                PortalNavigationItemViewerContext.forPortal(true)
            )
        );

        // Then
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_return_only_published_items_when_load_children_false_and_only_visible_in_portal_true() {
        // Given
        // Setup data for APIs, Guides, Support
        var unpublishedPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "Unpublished Page", null);
        unpublishedPage.setPublished(false);

        var publishedPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "Published Page", null);
        publishedPage.setPublished(true);

        queryService.initWith(List.of(unpublishedPage, publishedPage));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                false,
                PortalNavigationItemViewerContext.forPortal(true)
            )
        );

        // Then
        assertThat(result.items()).hasSize(1).extracting(PortalNavigationItem::getTitle).containsExactly("Published Page");
    }

    @Test
    void should_return_only_public_items_when_only_portal_visibility_and_not_authenticated() {
        // Given
        // Setup data for APIs, Guides, Support
        var privatePage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "Private Page", null);
        privatePage.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);

        var publicPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "Public Page", null);
        publicPage.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);

        queryService.initWith(List.of(privatePage, publicPage));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                false,
                PortalNavigationItemViewerContext.forPortal(false)
            )
        );

        // Then
        assertThat(result.items()).hasSize(1).extracting(PortalNavigationItem::getTitle).containsExactly("Public Page");
    }

    @Test
    void should_hide_private_api_nav_item_from_authenticated_non_member() {
        // Given a private API nav item and an authenticated user that is neither a member nor a subscriber.
        var apiNavItem = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            "Restricted API",
            null,
            "private-api-id"
        );
        apiNavItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
        apiNavItem.setPublished(true);

        queryService.initWith(List.of(apiNavItem));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forPortal("user-without-access")
            )
        );

        // Then the API nav item is not exposed through the documentation endpoint either,
        // keeping it consistent with the catalog visibility rules.
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_show_private_api_nav_item_to_authenticated_member() {
        // Given a private API and a user that is a member of it.
        var apiNavItem = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            "Restricted API",
            null,
            "private-api-id"
        );
        apiNavItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
        apiNavItem.setPublished(true);

        queryService.initWith(List.of(apiNavItem));
        membershipQueryService.initWith(
            List.of(
                io.gravitee.apim.core.membership.model.Membership.builder()
                    .id("m-1")
                    .memberId("user-member")
                    .memberType(io.gravitee.apim.core.membership.model.Membership.Type.USER)
                    .referenceType(io.gravitee.apim.core.membership.model.Membership.ReferenceType.API)
                    .referenceId("private-api-id")
                    .build()
            )
        );

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forPortal("user-member")
            )
        );

        // Then
        assertThat(result.items()).extracting(PortalNavigationItem::getTitle).containsExactly("Restricted API");
    }

    @Test
    void should_hide_children_of_private_api_nav_item_for_non_member() {
        // Given a private API nav item with a public child folder containing a public page.
        var apiNavItem = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            "Restricted API",
            null,
            "private-api-id"
        );
        apiNavItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
        apiNavItem.setPublished(true);

        var childFolder = PortalNavigationItemFixtures.aFolder(
            PortalNavigationItemId.random().toString(),
            "API Resources",
            apiNavItem.getId()
        );
        childFolder.updateParent(apiNavItem);

        var childPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "API Overview", childFolder.getId());
        childPage.updateParent(childFolder);

        queryService.initWith(List.of(apiNavItem, childFolder, childPage));

        // When — non-member tries to load the whole tree
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forPortal("user-without-access")
            )
        );

        // Then none of the restricted API subtree is returned.
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_return_empty_when_parent_is_private_api_and_user_is_not_member() {
        // Given a private API nav item with a public child.
        var apiNavItem = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            "Restricted API",
            null,
            "private-api-id"
        );
        apiNavItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
        apiNavItem.setPublished(true);

        var childPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "API Overview", apiNavItem.getId());
        childPage.updateParent(apiNavItem);

        queryService.initWith(List.of(apiNavItem, childPage));

        // When — user queries with the API nav item as parent directly (bypass attempt)
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(apiNavItem.getId()),
                true,
                PortalNavigationItemViewerContext.forPortal("user-without-access")
            )
        );

        // Then the parent is rejected and no children are exposed.
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_return_empty_when_parent_is_descendant_of_private_api_and_user_is_not_member() {
        // Given a private API nav item with a folder child containing a page.
        var apiNavItem = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            "Restricted API",
            null,
            "private-api-id"
        );
        apiNavItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
        apiNavItem.setPublished(true);

        var childFolder = PortalNavigationItemFixtures.aFolder(
            PortalNavigationItemId.random().toString(),
            "API Resources",
            apiNavItem.getId()
        );
        childFolder.updateParent(apiNavItem);

        var childPage = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "API Overview", childFolder.getId());
        childPage.updateParent(childFolder);

        queryService.initWith(List.of(apiNavItem, childFolder, childPage));

        // When — user queries with the folder as parent (descendant of hidden API)
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(childFolder.getId()),
                true,
                PortalNavigationItemViewerContext.forPortal("user-without-access")
            )
        );

        // Then the ancestor check still prevents exposure.
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_only_return_nav_bar_items_when_area_is_top_navbar() {
        // Given
        // Setup data for APIs, Guides, Support
        var homePageItem = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "Home Page Item", null);
        homePageItem.setArea(PortalArea.HOMEPAGE);
        homePageItem.setPublished(true);

        var navBarItem = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().toString(), "Nav Bar Item", null);
        navBarItem.setArea(PortalArea.TOP_NAVBAR);

        queryService.initWith(List.of(homePageItem, navBarItem));

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.empty(),
                true,
                PortalNavigationItemViewerContext.forConsole()
            )
        );

        // Then
        assertThat(result.items()).hasSize(1).extracting(PortalNavigationItem::getTitle).containsExactly("Nav Bar Item");
    }
}
