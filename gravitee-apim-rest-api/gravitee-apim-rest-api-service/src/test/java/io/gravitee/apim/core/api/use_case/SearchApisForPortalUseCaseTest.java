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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.application.domain_service.UserApplicationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApisForPortalUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-1";

    private static final String PUBLIC_API_ID = "public-api-1";
    private static final String PRIVATE_API_ID = "private-api-1";

    private SearchApisForPortalUseCase useCase;
    private PortalNavigationItemsQueryServiceInMemory navQueryService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private ApiPortalSearchQueryServiceInMemory apiSearchQueryService;

    @BeforeEach
    void setUp() {
        navQueryService = new PortalNavigationItemsQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        var subscriptionQueryService = new SubscriptionQueryServiceInMemory();
        apiSearchQueryService = new ApiPortalSearchQueryServiceInMemory();
        var userApplicationDomainService = new UserApplicationDomainService(membershipQueryService);
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            userApplicationDomainService
        );
        var visibilityDomainService = new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService);
        useCase = new SearchApisForPortalUseCase(visibilityDomainService, apiSearchQueryService);
    }

    @Test
    void delegates_visibility_resolution_and_searches_allowed_apis() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        apiSearchQueryService.initWith(List.of(anApi(PUBLIC_API_ID), anApi(PRIVATE_API_ID)));

        var result = useCase.execute(new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 10), null));

        assertThat(result.apis().getContent()).extracting(Api::getId).containsExactly(PUBLIC_API_ID);
        assertThat(result.apis().getTotalElements()).isEqualTo(1);
    }

    @Test
    void passes_correct_allowed_ids_for_member_user() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)
            )
        );
        membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));
        apiSearchQueryService.initWith(List.of(anApi(PUBLIC_API_ID), anApi(PRIVATE_API_ID)));

        var result = useCase.execute(new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, USER_ID, null, new PageableImpl(1, 10), null));

        assertThat(result.apis().getContent()).extracting(Api::getId).containsExactlyInAnyOrder(PUBLIC_API_ID, PRIVATE_API_ID);
        assertThat(result.apis().getTotalElements()).isEqualTo(2);
    }

    @Test
    void returns_page_of_apis_from_search_service() {
        navQueryService.initWith(
            List.of(
                publishedApiNavItem("api-a", PortalVisibility.PUBLIC),
                publishedApiNavItem("api-b", PortalVisibility.PUBLIC),
                publishedApiNavItem("api-c", PortalVisibility.PUBLIC)
            )
        );
        apiSearchQueryService.initWith(List.of(anApi("api-a"), anApi("api-b"), anApi("api-c")));

        var page1 = useCase.execute(new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 2), null));
        var page2 = useCase.execute(new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(2, 2), null));

        assertThat(page1.apis().getContent()).hasSize(2);
        assertThat(page1.apis().getTotalElements()).isEqualTo(3);
        assertThat(page2.apis().getContent()).hasSize(1);
        assertThat(page2.apis().getTotalElements()).isEqualTo(3);
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

    private Api anApi(String id) {
        return Api.builder().id(id).name(id).environmentId(ENV_ID).build();
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
}
