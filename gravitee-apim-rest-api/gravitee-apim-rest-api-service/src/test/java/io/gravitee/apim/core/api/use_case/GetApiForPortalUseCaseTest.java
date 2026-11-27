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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetApiForPortalUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-1";
    private static final String PUBLIC_API_ID = "public-api-1";
    private static final String PRIVATE_API_ID = "private-api-1";

    private GetApiForPortalUseCase useCase;
    private PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory();
    private MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory();
    private ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            apiQueryService
        );
        useCase = new GetApiForPortalUseCase(
            new PortalNavigationAccessDomainService(
                portalCrudService,
                new ApiCrudServiceInMemory(),
                navQueryService,
                new PortalPageContentQueryServiceInMemory(),
                new ApiOwnedNavigationDomainService(navQueryService, new PortalNavigationItemsCrudServiceInMemory()),
                apiQueryService
            ),
            new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService)
        );
    }

    @Test
    void should_return_visible_when_api_is_public() {
        navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));

        var output = useCase.execute(unscopedInput(PUBLIC_API_ID, null));

        assertThat(output.visible()).isTrue();
    }

    @Test
    void should_return_not_visible_when_api_is_private_and_anonymous() {
        navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));

        var output = useCase.execute(unscopedInput(PRIVATE_API_ID, null));

        assertThat(output.visible()).isFalse();
    }

    @Test
    void should_return_visible_when_api_is_private_and_user_is_member() {
        navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-" + USER_ID + "-" + PRIVATE_API_ID)
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(PRIVATE_API_ID)
                    .build()
            )
        );

        var output = useCase.execute(unscopedInput(PRIVATE_API_ID, USER_ID));

        assertThat(output.visible()).isTrue();
    }

    @Test
    void should_return_visible_when_api_is_private_and_user_has_access_via_group() {
        var groupId = "group-1";
        navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-" + USER_ID + "-" + groupId)
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId(groupId)
                    .build()
            )
        );
        apiQueryService.initWith(
            List.of(Api.builder().id(PRIVATE_API_ID).environmentId(ENV_ID).name(PRIVATE_API_ID).groups(Set.of(groupId)).build())
        );

        var output = useCase.execute(unscopedInput(PRIVATE_API_ID, USER_ID));

        assertThat(output.visible()).isTrue();
    }

    @Test
    void should_return_not_visible_when_api_not_in_portal_navigation() {
        navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));

        var output = useCase.execute(unscopedInput("non-existent-api", USER_ID));

        assertThat(output.visible()).isFalse();
    }

    @Nested
    class Within_a_portal {

        private PortalId portalA;
        private PortalId portalB;

        @BeforeEach
        void two_portals() {
            portalA = givenPortal(ENV_ID, ORG_ID);
            portalB = givenPortal(ENV_ID, ORG_ID);
        }

        @Test
        void should_return_visible_when_the_portal_lists_the_api() {
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, portalB)));

            assertThat(useCase.execute(scopedInput(PUBLIC_API_ID, null, portalB)).visible()).isTrue();
        }

        @Test
        void should_return_not_visible_when_only_another_portal_lists_the_public_api() {
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, portalB)));

            assertThat(useCase.execute(scopedInput(PUBLIC_API_ID, null, portalA)).visible()).isFalse();
            assertThat(useCase.execute(scopedInput(PUBLIC_API_ID, USER_ID, portalA)).visible()).isFalse();
        }

        @Test
        void should_return_not_visible_on_the_default_portal_when_only_another_portal_lists_the_api() {
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, portalB)));
            var defaultPortal = ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId();
            givenDefaultPortal(defaultPortal);

            assertThat(useCase.execute(scopedInput(PUBLIC_API_ID, null, defaultPortal)).visible()).isFalse();
        }

        @Test
        void should_keep_visibility_rules_within_the_portal() {
            navQueryService.initWith(
                List.of(
                    publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE, portalA),
                    publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PUBLIC, portalB)
                )
            );

            assertThat(useCase.execute(scopedInput(PRIVATE_API_ID, null, portalA)).visible()).isFalse();
            assertThat(useCase.execute(scopedInput(PRIVATE_API_ID, USER_ID, portalA)).visible()).isFalse();

            membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));

            assertThat(useCase.execute(scopedInput(PRIVATE_API_ID, USER_ID, portalA)).visible()).isTrue();
        }

        @Test
        void should_throw_portal_not_found_for_an_unknown_portal() {
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));

            assertThatThrownBy(() -> useCase.execute(scopedInput(PUBLIC_API_ID, null, PortalId.random()))).isInstanceOf(
                PortalNotFoundException.class
            );
        }

        @Test
        void should_throw_portal_not_found_for_a_portal_of_another_environment() {
            var elsewhere = givenPortal("another-env", ORG_ID);
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, elsewhere)));

            assertThatThrownBy(() -> useCase.execute(scopedInput(PUBLIC_API_ID, null, elsewhere))).isInstanceOf(
                PortalNotFoundException.class
            );
        }
    }

    @Test
    void should_return_visible_on_the_unscoped_path_whichever_portal_lists_the_api() {
        var portalB = givenPortal(ENV_ID, ORG_ID);
        navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, portalB)));

        assertThat(useCase.execute(unscopedInput(PUBLIC_API_ID, null)).visible()).isTrue();
    }

    private GetApiForPortalUseCase.Input unscopedInput(String apiId, String userId) {
        return new GetApiForPortalUseCase.Input(ENV_ID, ORG_ID, apiId, userId, Optional.empty());
    }

    private GetApiForPortalUseCase.Input scopedInput(String apiId, String userId, PortalId portalId) {
        return new GetApiForPortalUseCase.Input(ENV_ID, ORG_ID, apiId, userId, Optional.of(portalId));
    }

    private PortalId givenPortal(String environmentId, String organizationId) {
        return portalCrudService.create(Portal.create(environmentId, organizationId, "Portal", "portal-access-point")).getId();
    }

    private void givenDefaultPortal(PortalId portalId) {
        portalCrudService.create(
            Portal.of(portalId, ENV_ID, ORG_ID, "Developer Portal", PortalNavigationStructure.empty(), null, true, "portal-access-point")
        );
    }

    private Membership apiMembership(String userId, String apiId) {
        return Membership.builder()
            .id("membership-" + userId + "-" + apiId)
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API)
            .referenceId(apiId)
            .build();
    }

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility, PortalId portalId) {
        var item = publishedApiNavItem(apiId, visibility);
        item.changeOwner(new NavigationItemReference.PortalReference(portalId));
        return item;
    }

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility) {
        return PortalNavigationApi.builder()
            .reference(PORTAL_REFERENCE)
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Nav for " + apiId)
            .segment(PortalNavigationItem.slugify("Nav for " + apiId).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(visibility)
            .build();
    }
}
