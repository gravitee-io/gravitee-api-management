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

import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.application.domain_service.UserApplicationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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

    @BeforeEach
    void setUp() {
        var userApplicationDomainService = new UserApplicationDomainService(membershipQueryService);
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            userApplicationDomainService
        );
        useCase = new GetApiForPortalUseCase(new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService));
    }

    @Test
    void should_return_visible_when_api_is_public() {
        navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));

        var output = useCase.execute(new GetApiForPortalUseCase.Input(ENV_ID, PUBLIC_API_ID, null));

        assertThat(output.visible()).isTrue();
    }

    @Test
    void should_return_not_visible_when_api_is_private_and_anonymous() {
        navQueryService.initWith(List.of(publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE)));

        var output = useCase.execute(new GetApiForPortalUseCase.Input(ENV_ID, PRIVATE_API_ID, null));

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

        var output = useCase.execute(new GetApiForPortalUseCase.Input(ENV_ID, PRIVATE_API_ID, USER_ID));

        assertThat(output.visible()).isTrue();
    }

    @Test
    void should_return_not_visible_when_api_not_in_portal_navigation() {
        navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC)));

        var output = useCase.execute(new GetApiForPortalUseCase.Input(ENV_ID, "non-existent-api", USER_ID));

        assertThat(output.visible()).isFalse();
    }

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
}
