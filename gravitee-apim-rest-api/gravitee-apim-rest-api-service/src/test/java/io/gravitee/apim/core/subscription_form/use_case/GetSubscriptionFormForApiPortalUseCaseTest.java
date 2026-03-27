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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionFormElResolverInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import java.util.Map;
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
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final SubscriptionFormElResolverInMemory elResolver = new SubscriptionFormElResolverInMemory();
    private final SubscriptionFormSchemaGeneratorImpl schemaGenerator = new SubscriptionFormSchemaGeneratorImpl();
    private GetSubscriptionFormForApiPortalUseCase useCase;

    @BeforeEach
    void setUp() {
        navQueryService.reset();
        membershipQueryService.reset();
        subscriptionQueryService.reset();
        queryService.reset();
        elResolver.reset();

        var apiMembershipDomainService = new ApiPortalMembershipDomainService(membershipQueryService, subscriptionQueryService);
        var visibility = new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService);
        useCase = new GetSubscriptionFormForApiPortalUseCase(visibility, queryService, schemaGenerator, elResolver);
    }

    @Test
    void should_throw_api_not_found_when_api_not_visible_in_portal() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PRIVATE)));

        var input = GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(USER_ID).build();

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_return_resolved_options_when_api_is_public() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PUBLIC)));
        var form = enabledFormWithDynamicSelect();
        queryService.initWith(List.of(form));
        elResolver.withResolved(Map.of("{#api.metadata['envs']}", List.of("Dev", "Staging", "Prod")));

        var result = useCase.execute(
            GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(USER_ID).build()
        );

        assertThat(result.subscriptionForm()).isEqualTo(form);
        assertThat(result.resolvedOptions()).containsEntry("env", List.of("Dev", "Staging", "Prod"));
    }

    @Test
    void should_return_resolved_options_when_api_is_private_and_user_is_member() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PRIVATE)));
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-" + USER_ID + "-" + API_ID)
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .build()
            )
        );
        var form = enabledFormWithDynamicSelect();
        queryService.initWith(List.of(form));
        elResolver.withResolved(Map.of("{#api.metadata['envs']}", List.of("A", "B")));

        var result = useCase.execute(
            GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(USER_ID).build()
        );

        assertThat(result.resolvedOptions()).containsEntry("env", List.of("A", "B"));
    }

    @Test
    void should_throw_subscription_form_not_found_after_visibility_when_no_form() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PUBLIC)));
        queryService.initWith(List.of());

        var input = GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(null).build();

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }

    @Test
    void should_throw_when_form_disabled() {
        navQueryService.initWith(List.of(publishedApiNavItem(API_ID, PortalVisibility.PUBLIC)));
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENV_ID).enabled(false).build();
        queryService.initWith(List.of(disabledForm));

        var input = GetSubscriptionFormForApiPortalUseCase.Input.builder().environmentId(ENV_ID).apiId(API_ID).userId(null).build();

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }

    private SubscriptionForm enabledFormWithDynamicSelect() {
        return SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .environmentId(ENV_ID)
            .enabled(true)
            .gmdContent(
                io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown.of(
                    "<gmd-select fieldKey=\"env\" options=\"{#api.metadata['envs']}:Prod,Test\"/>"
                )
            )
            .build();
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
