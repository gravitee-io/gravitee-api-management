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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.application.domain_service.UserApplicationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationApiVisibilityDomainServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-1";

    private static final String PUBLIC_API_ID = "public-api-1";
    private static final String PRIVATE_API_ID = "private-api-1";

    private PortalNavigationApiVisibilityDomainService domainService;
    private PortalNavigationItemsQueryServiceInMemory navQueryService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private SubscriptionQueryServiceInMemory subscriptionQueryService;

    @BeforeEach
    void setUp() {
        navQueryService = new PortalNavigationItemsQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        subscriptionQueryService = new SubscriptionQueryServiceInMemory();
        var userApplicationDomainService = new UserApplicationDomainService(membershipQueryService);
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            userApplicationDomainService
        );
        domainService = new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService);
    }

    // --- resolveVisibleItems ---

    @Test
    void anonymous_user_sees_only_public_apis() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );

        var result = domainService.resolveVisibleItems(ENV_ID, null);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactly(PUBLIC_API_ID);
    }

    @Test
    void authenticated_user_who_is_member_sees_private_api() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactlyInAnyOrder(PUBLIC_API_ID, PRIVATE_API_ID);
    }

    @Test
    void authenticated_user_who_is_not_member_cannot_see_private_api() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactly(PUBLIC_API_ID);
    }

    @Test
    void returns_empty_list_when_no_allowed_ids() {
        navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));

        var result = domainService.resolveVisibleItems(ENV_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    void authenticated_user_who_is_member_via_group_sees_private_api() {
        var groupId = "group-1";
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        membershipQueryService.initWith(List.of(userGroupMembership(USER_ID, groupId), groupApiMembership(groupId, PRIVATE_API_ID)));

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactlyInAnyOrder(PUBLIC_API_ID, PRIVATE_API_ID);
    }

    @Test
    void authenticated_user_whose_group_has_no_api_membership_cannot_see_private_api() {
        var groupId = "group-1";
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        membershipQueryService.initWith(List.of(userGroupMembership(USER_ID, groupId)));

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactly(PUBLIC_API_ID);
    }

    @Test
    void authenticated_user_with_application_subscription_sees_private_api() {
        var appId = "app-1";
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        membershipQueryService.initWith(List.of(applicationMembership(USER_ID, appId)));
        subscriptionQueryService.initWith(List.of(aSubscription(appId, PRIVATE_API_ID, SubscriptionEntity.Status.ACCEPTED)));

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactlyInAnyOrder(PUBLIC_API_ID, PRIVATE_API_ID);
    }

    @Test
    void authenticated_user_with_closed_subscription_cannot_see_private_api() {
        var appId = "app-1";
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        membershipQueryService.initWith(List.of(applicationMembership(USER_ID, appId)));
        subscriptionQueryService.initWith(List.of(aSubscription(appId, PRIVATE_API_ID, SubscriptionEntity.Status.CLOSED)));

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactly(PUBLIC_API_ID);
    }

    @Test
    void authenticated_user_with_no_application_cannot_see_private_api_via_subscription() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );

        var result = domainService.resolveVisibleItems(ENV_ID, USER_ID);

        assertThat(result).extracting(PortalNavigationApi::getApiId).containsExactly(PUBLIC_API_ID);
    }

    // --- isVisibleToUser ---

    @Test
    void public_item_is_visible_to_anonymous_user() {
        var item = publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC);
        assertThat(domainService.isVisibleToUser(item, null)).isTrue();
    }

    @Test
    void public_item_is_visible_to_authenticated_user() {
        var item = publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC);
        assertThat(domainService.isVisibleToUser(item, USER_ID)).isTrue();
    }

    @Test
    void private_item_is_not_visible_to_anonymous_user() {
        var item = publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE);
        assertThat(domainService.isVisibleToUser(item, null)).isFalse();
    }

    @Test
    void private_item_is_visible_to_member_user() {
        membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));
        var item = publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE);
        assertThat(domainService.isVisibleToUser(item, USER_ID)).isTrue();
    }

    @Test
    void private_item_is_visible_to_subscriber_user() {
        var appId = "app-1";
        membershipQueryService.initWith(List.of(applicationMembership(USER_ID, appId)));
        subscriptionQueryService.initWith(List.of(aSubscription(appId, PRIVATE_API_ID, SubscriptionEntity.Status.ACCEPTED)));
        var item = publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE);
        assertThat(domainService.isVisibleToUser(item, USER_ID)).isTrue();
    }

    @Test
    void private_item_is_not_visible_to_non_member_non_subscriber() {
        var item = publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE);
        assertThat(domainService.isVisibleToUser(item, USER_ID)).isFalse();
    }

    @Nested
    class IsApiVisibleToUser {

        @Test
        void public_api_is_visible_to_anonymous_user_by_id() {
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));
            assertThat(domainService.isApiVisibleToUser(ENV_ID, PUBLIC_API_ID, null)).isTrue();
        }

        @Test
        void private_api_is_not_visible_to_anonymous_user_by_id() {
            navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));
            assertThat(domainService.isApiVisibleToUser(ENV_ID, PRIVATE_API_ID, null)).isFalse();
        }

        @Test
        void private_api_is_visible_to_member_by_id() {
            navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));
            membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));
            assertThat(domainService.isApiVisibleToUser(ENV_ID, PRIVATE_API_ID, USER_ID)).isTrue();
        }

        @Test
        void private_api_is_visible_to_subscriber_by_id() {
            var appId = "app-1";
            navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));
            membershipQueryService.initWith(List.of(applicationMembership(USER_ID, appId)));
            subscriptionQueryService.initWith(List.of(aSubscription(appId, PRIVATE_API_ID, SubscriptionEntity.Status.ACCEPTED)));
            assertThat(domainService.isApiVisibleToUser(ENV_ID, PRIVATE_API_ID, USER_ID)).isTrue();
        }

        @Test
        void private_api_is_not_visible_to_non_member_non_subscriber_by_id() {
            navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));
            assertThat(domainService.isApiVisibleToUser(ENV_ID, PRIVATE_API_ID, USER_ID)).isFalse();
        }

        @Test
        void unknown_api_is_not_visible_by_id() {
            navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));
            assertThat(domainService.isApiVisibleToUser(ENV_ID, "non-existent-api", USER_ID)).isFalse();
        }
    }

    // --- helpers ---

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Nav for " + apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(visibility)
            .build();
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

    private Membership userGroupMembership(String userId, String groupId) {
        return Membership.builder()
            .id("membership-" + userId + "-" + groupId)
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.GROUP)
            .referenceId(groupId)
            .build();
    }

    private Membership groupApiMembership(String groupId, String apiId) {
        return Membership.builder()
            .id("membership-" + groupId + "-" + apiId)
            .memberId(groupId)
            .memberType(Membership.Type.GROUP)
            .referenceType(Membership.ReferenceType.API)
            .referenceId(apiId)
            .build();
    }

    private Membership applicationMembership(String userId, String appId) {
        return Membership.builder()
            .id("membership-" + userId + "-" + appId)
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.APPLICATION)
            .referenceId(appId)
            .build();
    }

    private SubscriptionEntity aSubscription(String appId, String apiId, SubscriptionEntity.Status status) {
        return SubscriptionEntity.builder().id("sub-" + appId + "-" + apiId).applicationId(appId).apiId(apiId).status(status).build();
    }
}
