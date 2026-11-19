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
import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal.exception.DefaultPortalNotFoundException;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.CheckTypoToleranceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
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
    private PortalCrudServiceInMemory portalCrudService;

    @BeforeEach
    void setUp() {
        navQueryService = new PortalNavigationItemsQueryServiceInMemory();
        portalCrudService = new PortalCrudServiceInMemory();
        givenDefaultPortal();
        membershipQueryService = new MembershipQueryServiceInMemory();
        var subscriptionQueryService = new SubscriptionQueryServiceInMemory();
        apiSearchQueryService = new ApiPortalSearchQueryServiceInMemory();
        var apiQueryService = new ApiQueryServiceInMemory();
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            subscriptionQueryService,
            apiQueryService
        );
        var visibilityDomainService = new PortalNavigationApiVisibilityDomainService(navQueryService, apiMembershipDomainService);
        useCase = new SearchApisForPortalUseCase(
            new PortalNavigationAccessDomainService(
                portalCrudService,
                new ApiCrudServiceInMemory(),
                navQueryService,
                new PortalPageContentQueryServiceInMemory(),
                new ApiOwnedNavigationDomainService(navQueryService, new PortalNavigationItemsCrudServiceInMemory()),
                apiQueryService
            ),
            visibilityDomainService,
            apiSearchQueryService,
            new CheckTypoToleranceDomainService(new ParametersQueryServiceInMemory())
        );
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

        var result = useCase.execute(
            new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 10), null, Optional.empty())
        );

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

        var result = useCase.execute(
            new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, USER_ID, null, new PageableImpl(1, 10), null, Optional.empty())
        );

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

        var page1 = useCase.execute(
            new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 2), null, Optional.empty())
        );
        var page2 = useCase.execute(
            new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(2, 2), null, Optional.empty())
        );

        assertThat(page1.apis().getContent()).hasSize(2);
        assertThat(page1.apis().getTotalElements()).isEqualTo(3);
        assertThat(page2.apis().getContent()).hasSize(1);
        assertThat(page2.apis().getTotalElements()).isEqualTo(3);
    }

    @Test
    void searches_only_the_apis_of_the_given_portal() {
        var portal = givenPortal(ENV_ID, ORG_ID);
        var otherPortal = givenPortal(ENV_ID, ORG_ID);
        navQueryService.initWith(
            List.of(
                publishedApiNavItem("api-own", PortalVisibility.PUBLIC, portal),
                publishedApiNavItem("api-other", PortalVisibility.PUBLIC, otherPortal)
            )
        );
        apiSearchQueryService.initWith(List.of(anApi("api-own"), anApi("api-other")));

        var result = useCase.execute(scopedInput(null, portal));

        assertThat(result.apis().getContent()).extracting(Api::getId).containsExactly("api-own");
        assertThat(result.apis().getTotalElements()).isEqualTo(1);
    }

    @Test
    void keeps_visibility_rules_within_the_given_portal() {
        var portal = givenPortal(ENV_ID, ORG_ID);
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, portal),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE, portal)
            )
        );
        membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));
        apiSearchQueryService.initWith(List.of(anApi(PUBLIC_API_ID), anApi(PRIVATE_API_ID)));

        var anonymous = useCase.execute(scopedInput(null, portal));
        var member = useCase.execute(scopedInput(USER_ID, portal));

        assertThat(anonymous.apis().getContent()).extracting(Api::getId).containsExactly(PUBLIC_API_ID);
        assertThat(member.apis().getContent()).extracting(Api::getId).containsExactlyInAnyOrder(PUBLIC_API_ID, PRIVATE_API_ID);
    }

    @Test
    void resolves_api_visibility_within_the_given_portal() {
        var portal = givenPortal(ENV_ID, ORG_ID);
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE, portal),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PUBLIC)
            )
        );
        apiSearchQueryService.initWith(List.of(anApi(PRIVATE_API_ID)));

        assertThat(useCase.execute(scopedInput(null, portal)).apis().getContent()).isEmpty();
        assertThat(useCase.execute(scopedInput(USER_ID, portal)).apis().getContent()).isEmpty();

        membershipQueryService.initWith(List.of(apiMembership(USER_ID, PRIVATE_API_ID)));

        assertThat(useCase.execute(scopedInput(USER_ID, portal)).apis().getContent())
            .extracting(Api::getId)
            .containsExactly(PRIVATE_API_ID);
    }

    @Test
    void resolves_api_visibility_within_the_default_portal_when_no_portal_is_given() {
        var otherPortal = givenPortal(ENV_ID, ORG_ID);
        navQueryService.initWith(
            List.of(
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PRIVATE),
                publishedApiNavItem(PRIVATE_API_ID, PortalVisibility.PUBLIC, otherPortal)
            )
        );
        apiSearchQueryService.initWith(List.of(anApi(PRIVATE_API_ID)));
        var nonMember = new SearchApisForPortalUseCase.Input(
            ENV_ID,
            ORG_ID,
            USER_ID,
            null,
            new PageableImpl(1, 10),
            null,
            Optional.empty()
        );

        assertThat(useCase.execute(nonMember).apis().getContent()).isEmpty();
        assertThat(useCase.execute(nonMember).apis().getTotalElements()).isZero();
    }

    @Test
    void searches_the_default_portals_apis_when_no_portal_is_given() {
        var otherPortal = givenPortal(ENV_ID, ORG_ID);
        navQueryService.initWith(
            List.of(
                publishedApiNavItem("api-default", PortalVisibility.PUBLIC),
                publishedApiNavItem("api-other", PortalVisibility.PUBLIC, otherPortal)
            )
        );
        apiSearchQueryService.initWith(List.of(anApi("api-default"), anApi("api-other")));

        var result = useCase.execute(
            new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 10), null, Optional.empty())
        );

        assertThat(result.apis().getContent()).extracting(Api::getId).containsExactly("api-default");
        assertThat(result.apis().getTotalElements()).isEqualTo(1);
    }

    @Test
    void throws_default_portal_not_found_when_no_portal_is_given_and_the_environment_has_no_default_one() {
        portalCrudService.reset();
        givenPortal(ENV_ID, ORG_ID);
        givenPortal(ENV_ID, ORG_ID);
        var anonymous = new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 10), null, Optional.empty());
        var member = new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, USER_ID, null, new PageableImpl(1, 10), null, Optional.empty());

        assertThatThrownBy(() -> useCase.execute(anonymous)).isInstanceOf(DefaultPortalNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(member)).isInstanceOf(DefaultPortalNotFoundException.class);
    }

    @Test
    void throws_portal_not_found_for_an_unknown_portal() {
        var unknown = PortalId.random();

        assertThatThrownBy(() -> useCase.execute(scopedInput(null, unknown))).isInstanceOf(PortalNotFoundException.class);
        assertThatThrownBy(() -> useCase.execute(scopedInput(USER_ID, unknown))).isInstanceOf(PortalNotFoundException.class);
    }

    @Test
    void throws_portal_not_found_for_a_portal_of_another_environment() {
        var elsewhere = givenPortal("another-env", ORG_ID);
        navQueryService.initWith(List.of(publishedApiNavItem(PUBLIC_API_ID, PortalVisibility.PUBLIC, elsewhere)));

        assertThatThrownBy(() -> useCase.execute(scopedInput(null, elsewhere))).isInstanceOf(PortalNotFoundException.class);
    }

    // --- helpers ---

    private SearchApisForPortalUseCase.Input scopedInput(String userId, PortalId portalId) {
        return new SearchApisForPortalUseCase.Input(ENV_ID, ORG_ID, userId, null, new PageableImpl(1, 10), null, Optional.of(portalId));
    }

    @SneakyThrows
    private void givenDefaultPortal() {
        portalCrudService.create(
            Portal.of(
                ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId(),
                ENV_ID,
                ORG_ID,
                "Developer Portal",
                PortalNavigationStructure.empty(),
                null,
                true,
                URI.create("https://portal.example.com").toURL()
            )
        );
    }

    @SneakyThrows
    private PortalId givenPortal(String environmentId, String organizationId) {
        return portalCrudService
            .create(Portal.create(environmentId, organizationId, "Portal", URI.create("https://portal.example.com").toURL()))
            .getId();
    }

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility, PortalId portalId) {
        return publishedApiNavItem(apiId, visibility, new NavigationItemReference.PortalReference(portalId));
    }

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility) {
        return publishedApiNavItem(apiId, visibility, PORTAL_REFERENCE);
    }

    private PortalNavigationApi publishedApiNavItem(String apiId, PortalVisibility visibility, NavigationItemReference reference) {
        return PortalNavigationApi.builder()
            .reference(reference)
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
