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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ApiProductSearchQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.domain_service.ApiProductDeploymentStateDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchApiProductsUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";

    private static final Instant DEPLOYED_AT = Instant.parse("2024-01-10T10:00:00Z");

    private ApiProductSearchQueryServiceInMemory apiProductSearchQueryService;
    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final ObjectMapper objectMapper = GraviteeJacksonMapper.getInstance();

    @Mock
    private ApiProductAccessibleIdsDomainService apiProductAccessibleIdsDomainService;

    private SearchApiProductsUseCase useCase;

    @BeforeEach
    void setUp() {
        apiProductSearchQueryService = new ApiProductSearchQueryServiceInMemory();
        useCase = new SearchApiProductsUseCase(
            apiProductSearchQueryService,
            apiProductAccessibleIdsDomainService,
            new ApiProductDeploymentStateDomainService(eventLatestQueryService, planQueryService, objectMapper)
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(apiProductAccessibleIdsDomainService);
        eventLatestQueryService.reset();
        planQueryService.reset();
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

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of("id-1", "id-3"));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id-1", "id-3");
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_intersect_requested_ids_with_owned_ids_for_non_admin() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Product Two").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2));

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of("id-1"));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, Set.of("id-1", "id-2"), pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactly("id-1");
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_return_empty_page_for_non_admin_with_no_memberships() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1));

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of());

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).isEmpty();
        assertThat(output.page().getTotalElements()).isZero();
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_include_api_products_inherited_via_group_membership_for_non_admin() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Product Two").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2));

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of("id-2"));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactly("id-2");
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_union_direct_and_group_inherited_api_products_for_non_admin() {
        var product1 = ApiProduct.builder().id("id-1").name("Direct").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Inherited").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2));

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of("id-1", "id-2"));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactlyInAnyOrder("id-1", "id-2");
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_intersect_requested_ids_with_group_inherited_ids_for_non_admin() {
        var product1 = ApiProduct.builder().id("id-1").name("Inherited One").environmentId(ENV_ID).build();
        var product2 = ApiProduct.builder().id("id-2").name("Other").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1, product2));

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of("id-1"));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, Set.of("id-1", "id-2"), pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).extracting(ApiProduct::getId).containsExactly("id-1");
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_return_empty_page_when_non_admin_requests_ids_they_do_not_own() {
        var product1 = ApiProduct.builder().id("id-1").name("Product One").environmentId(ENV_ID).build();
        apiProductSearchQueryService.initWith(List.of(product1));

        when(apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(ENV_ID, USER_ID)).thenReturn(Set.of("id-2"));

        var pageable = new PageableImpl(1, 10);
        var input = SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, Set.of("id-1"), pageable, null, USER_ID, false);

        var output = useCase.execute(input);

        assertThat(output.page().getContent()).isEmpty();
        verify(apiProductAccessibleIdsDomainService).findAccessibleApiProductIds(eq(ENV_ID), eq(USER_ID));
    }

    @Test
    void should_report_whether_each_matched_product_is_still_deployed() throws Exception {
        // A listing that never resolves this reports every product as "not deployed", whatever the gateway
        // is actually serving.
        var deployed = ApiProduct.builder().id("id-1").name("Deployed").environmentId(ENV_ID).apiIds(Set.of("api-1")).build();
        var drifted = ApiProduct.builder().id("id-2").name("Drifted").environmentId(ENV_ID).apiIds(Set.of("api-1", "api-2")).build();
        apiProductSearchQueryService.initWith(List.of(deployed, drifted));
        eventLatestQueryService.initWith(
            List.of(
                aDeployEvent("id-1", ApiProduct.builder().id("id-1").apiIds(Set.of("api-1")).build()),
                aDeployEvent("id-2", ApiProduct.builder().id("id-2").apiIds(Set.of("api-1")).build())
            )
        );

        var output = useCase.execute(
            SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 10), null, USER_ID, true)
        );

        assertThat(output.page().getContent())
            .extracting(ApiProduct::getId, ApiProduct::getDeploymentState)
            .containsExactlyInAnyOrder(
                tuple("id-1", ApiProduct.DeploymentState.DEPLOYED),
                tuple("id-2", ApiProduct.DeploymentState.NEED_REDEPLOY)
            );
    }

    @Test
    void should_report_a_product_that_was_never_deployed_as_needing_a_deploy() {
        apiProductSearchQueryService.initWith(List.of(ApiProduct.builder().id("id-1").name("Fresh").environmentId(ENV_ID).build()));

        var output = useCase.execute(
            SearchApiProductsUseCase.Input.of(ENV_ID, ORG_ID, null, null, new PageableImpl(1, 10), null, USER_ID, true)
        );

        assertThat(output.page().getContent())
            .extracting(ApiProduct::getDeploymentState)
            .containsExactly(ApiProduct.DeploymentState.NEED_REDEPLOY);
    }

    private Event aDeployEvent(String apiProductId, ApiProduct deployedPayload) throws Exception {
        return Event.builder()
            .id("event-" + apiProductId)
            .type(EventType.DEPLOY_API_PRODUCT)
            .properties(new EnumMap<>(Map.of(Event.EventProperties.API_PRODUCT_ID, apiProductId)))
            .payload(objectMapper.writeValueAsString(deployedPayload))
            .environments(Set.of(ENV_ID))
            .createdAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
            .updatedAt(DEPLOYED_AT.atZone(ZoneId.systemDefault()))
            .build();
    }
}
