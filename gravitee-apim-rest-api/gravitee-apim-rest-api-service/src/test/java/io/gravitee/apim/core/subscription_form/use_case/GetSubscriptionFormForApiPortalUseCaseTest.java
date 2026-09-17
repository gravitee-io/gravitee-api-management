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
package io.gravitee.apim.core.subscription_form.use_case;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionFormElResolverInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormResolutionDomainService;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetSubscriptionFormForApiPortalUseCaseTest {

    private static final String ENV_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final String ORG_ID = "org-id";
    private static final String API_ID = "api-1";
    private static final String USER_ID = "user-1";

    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final SubscriptionFormElResolverInMemory elResolver = new SubscriptionFormElResolverInMemory();
    private final SubscriptionFormSchemaGeneratorImpl schemaGenerator = new SubscriptionFormSchemaGeneratorImpl();
    private GetSubscriptionFormForApiPortalUseCase useCase;

    @BeforeEach
    void setUp() {
        navQueryService.reset();
        membershipQueryService.reset();
        subscriptionQueryService.reset();
        apiQueryService.reset();
        elResolver.reset();

        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            apiQueryService
        );
        var visibility = new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService);
        useCase = new GetSubscriptionFormForApiPortalUseCase(
            visibility,
            new SubscriptionFormResolutionDomainService(),
            schemaGenerator,
            elResolver
        );
    }

    @Test
    void should_throw_api_not_found_when_api_not_visible_in_portal() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PRIVATE)));

        var input = GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(USER_ID).build();

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_subscription_form_not_found_when_no_form_applies_to_the_api() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PUBLIC)));

        var input = GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(USER_ID).build();

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility) {
        return PortalNavigationApi.builder()
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
