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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiProductSearchQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchApiProductsUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";
    private static final String ROLE_ID = "role-id";

    private ApiProductSearchQueryServiceInMemory apiProductSearchQueryService;
    private MembershipCrudServiceInMemory membershipCrudService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private SearchApiProductsUseCase useCase;

    @BeforeEach
    void setUp() {
        apiProductSearchQueryService = new ApiProductSearchQueryServiceInMemory();
        membershipCrudService = new MembershipCrudServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        useCase = new SearchApiProductsUseCase(apiProductSearchQueryService, membershipQueryService);
    }

    @Test
    void should_delegate_to_query_service_with_correct_arguments_for_admin() {
        var pageable = new PageableImpl(1, 10);
        var sortable = new SortableImpl("name", true);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, "my query", Set.of("id-1"), pageable, sortable, USER_ID, true);

        var output = useCase.execute(input);

        assertThat(output.page()).isNotNull();
        assertThat(output.page().getContent()).isEmpty();
        assertThat(output.page().getPageNumber()).isOne();
        assertThat(output.page().getPageElements()).isZero();
    }

    @Test
    void should_trim_query_so_whitespace_only_becomes_null() {
        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, "   \t  ", null, pageable, null, USER_ID, true);

        var output = useCase.execute(input);

        assertThat(output.page()).isNotNull();
        assertThat(output.page().getContent()).isEmpty();
    }

    @Test
    void should_pass_environment_id_and_organization_id_in_correct_order() {
        var pageable = new PageableImpl(2, 5);
        var input = SearchApiProductsUseCase.Input.of("env-1", "org-1", null, null, pageable, null, USER_ID, true);

        useCase.execute(input);

        assertThat(apiProductSearchQueryService.getLastEnvironmentId()).isEqualTo("env-1");
        assertThat(apiProductSearchQueryService.getLastOrganizationId()).isEqualTo("org-1");
    }

    @Test
    void should_return_empty_page_when_no_criteria_for_admin() {
        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, true);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).isEmpty();
        assertThat(output.page().getTotalElements()).isZero();
    }

    @Test
    void should_return_product_matching_query_for_admin() {
        var product = ApiProduct.builder().id("product-1").name("My API Product").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, "My API", null, pageable, null, USER_ID, true);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).hasSize(1);
        assertThat(output.page().getContent().get(0).getId()).isEqualTo("product-1");
        assertThat(output.page().getContent().get(0).getName()).isEqualTo("My API Product");
        assertThat(output.page().getTotalElements()).isOne();
    }

    @Test
    void should_return_products_filtered_by_ids_for_admin() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Product Two").environmentId(ENV_ID).build();
        var product3 = ApiProduct.builder().id("id-3").name("Product Three").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2, product3));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, Set.of("id-1", "id-3"), pageable, null, USER_ID, true);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).hasSize(2);
        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id-1", "id-3");
        assertThat(output.page().getTotalElements()).isEqualTo(2);
    }

    @Test
    void should_return_only_owned_products_for_non_admin_user() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Product Two").environmentId(ENV_ID).build();
        var product3 = ApiProduct.builder().id("id-3").name("Product Three").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2, product3));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("m-1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id-1")
                    .roleId(ROLE_ID)
                    .build(),
                Membership.builder()
                    .id("m-2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id-3")
                    .roleId(ROLE_ID)
                    .build()
            )
        );

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id-1", "id-3");
    }

    @Test
    void should_intersect_requested_ids_with_owned_ids_for_non_admin() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Product Two").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("m-1")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id-1")
                    .roleId(ROLE_ID)
                    .build()
            )
        );

        var pageable = new PageableImpl(1, 10);
        // User requests id-1 and id-2 but only owns id-1
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, Set.of("id-1", "id-2"), pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactly("id-1");
    }

    @Test
    void should_return_empty_page_for_non_admin_with_no_memberships() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).isEmpty();
        assertThat(output.page().getTotalElements()).isZero();
    }

    @Test
    void should_return_empty_page_when_non_admin_requests_ids_they_do_not_own() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1));

        membershipCrudService.initWith(
            List.of(
                Membership.builder()
                    .id("m-2")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API_PRODUCT)
                    .referenceId("id-2")
                    .roleId(ROLE_ID)
                    .build()
            )
        );

        var pageable = new PageableImpl(1, 10);
        // User owns id-2 but requests id-1 (which they don't own)
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, Set.of("id-1"), pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).isEmpty();
    }
}
