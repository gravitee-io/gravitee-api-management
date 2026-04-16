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
package io.gravitee.rest.api.management.v2.rest.resource.observability;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterValueItem;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterValuesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterValuesResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    @Inject
    FilterValuesQueryService filterValuesQueryService;

    @Inject
    FilterValueNameResolver filterValueNameResolver;

    @Inject
    AnalyticsQueryContextLoader analyticsQueryContextLoader;

    @Inject
    PlanQueryServiceInMemory planQueryServiceInMemory;

    @BeforeEach
    void setup() {
        var environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        when(analyticsQueryContextLoader.load(any())).thenReturn(
            new AnalyticsQueryContext(null, new ExecutionContext(ORGANIZATION, ENVIRONMENT), Set.of(), Map.of(), Map.of(), Map.of())
        );

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/observability/filters";
    }

    @Test
    void should_return_enum_values_for_http_method() {
        var response = rootTarget("HTTP_METHOD/values").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).isNotEmpty();
                assertThat(data).extracting(FilterValueItem::getValue).contains("GET", "POST", "PUT");
            });
    }

    @Test
    void should_return_400_for_number_filter() {
        var response = rootTarget("HTTP_STATUS/values").request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_return_400_for_string_filter() {
        var response = rootTarget("HTTP_PATH/values").request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_return_keyword_filter_values() {
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.GATEWAY),
                any(),
                any(),
                anyInt(),
                eq(10),
                any(),
                any(),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-1"), new FilterValue("gw-2")), null));

        var response = rootTarget("GATEWAY/values").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(2);
                assertThat(data).extracting(FilterValueItem::getValue).containsExactly("gw-1", "gw-2");
            });
    }

    @Test
    void should_pass_authorized_api_ids_from_analytics_context_to_keyword_filter_query() {
        when(analyticsQueryContextLoader.load(any())).thenReturn(
            new AnalyticsQueryContext(
                null,
                new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                Set.of("auth-api-1", "auth-api-2"),
                Map.of(),
                Map.of(),
                Map.of()
            )
        );
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.GATEWAY),
                any(),
                any(),
                anyInt(),
                eq(10),
                any(),
                any(),
                eq(Set.of("auth-api-1", "auth-api-2"))
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-1")), null));

        var response = rootTarget("GATEWAY/values").request().get();

        assertThat(response).hasStatus(200);
        verify(filterValuesQueryService).searchFilterValues(
            eq(ORGANIZATION),
            eq(ENVIRONMENT),
            eq(FilterSpec.Name.GATEWAY),
            any(),
            any(),
            eq(1),
            eq(10),
            any(),
            any(),
            eq(Set.of("auth-api-1", "auth-api-2"))
        );
    }

    @Test
    void should_pass_time_range_to_keyword_filter() {
        when(
            filterValuesQueryService.searchFilterValues(any(), any(), any(), any(), any(), anyInt(), eq(10), any(), any(), any())
        ).thenReturn(new FilterValuesPage(Collections.emptyList(), null));

        var response = rootTarget("API/values").queryParam("from", 1704067200000L).queryParam("to", 1735689599000L).request().get();

        assertThat(response).hasStatus(200);
    }

    @Test
    void should_return_400_for_invalid_filter_name() {
        var response = rootTarget("INVALID_FILTER/values").request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_pass_query_param_for_keyword_filter() {
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.GATEWAY),
                any(),
                any(),
                anyInt(),
                eq(10),
                any(),
                eq("gw-prod-1"),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-prod-1")), null));

        var response = rootTarget("GATEWAY/values").queryParam("query", "gw-prod-1").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data.get(0).getValue()).isEqualTo("gw-prod-1");
            });
    }

    @Test
    void should_search_by_name_for_id_based_filter_with_query() {
        when(analyticsQueryContextLoader.load(any())).thenReturn(
            new AnalyticsQueryContext(
                null,
                new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                Set.of(),
                Map.of("api-id-1", "My API 1", "api-id-2", "Other API"),
                Map.of(),
                Map.of()
            )
        );

        var response = rootTarget("API/values").queryParam("query", "My").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data.get(0).getValue()).isEqualTo("My API 1");
                assertThat(data.get(0).getId()).isEqualTo("api-id-1");
            });
    }

    @Test
    void should_filter_enum_values_with_query() {
        var response = rootTarget("HTTP_METHOD/values").queryParam("query", "PO").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data.get(0).getValue()).isEqualTo("POST");
            });
    }

    @Test
    void should_resolve_id_based_filter_names_without_query() {
        when(analyticsQueryContextLoader.load(any())).thenReturn(
            new AnalyticsQueryContext(
                null,
                new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                Set.of(),
                Map.of("api-id-1", "My API 1", "api-id-2", "My API 2"),
                Map.of(),
                Map.of()
            )
        );
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.API),
                any(),
                any(),
                anyInt(),
                eq(10),
                any(),
                any(),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("api-id-1"), new FilterValue("api-id-2")), null));

        var response = rootTarget("API/values").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(2);
                assertThat(data).extracting(FilterValueItem::getValue).containsExactly("My API 1", "My API 2");
                assertThat(data).extracting(FilterValueItem::getId).containsExactly("api-id-1", "api-id-2");
            });
    }

    @Test
    void should_paginate_enum_values_with_query() {
        var response = rootTarget("HTTP_METHOD/values")
            .queryParam("query", "PO")
            .queryParam("page", 1)
            .queryParam("perPage", 1)
            .request()
            .get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data.get(0).getValue()).isEqualTo("POST");
            });
    }

    @Test
    void should_pass_query_and_time_range_for_direct_value_keyword() {
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.GATEWAY),
                any(),
                any(),
                anyInt(),
                eq(10),
                any(),
                eq("gw-prod"),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-prod")), null));

        var response = rootTarget("GATEWAY/values")
            .queryParam("query", "gw-prod")
            .queryParam("from", 1704067200000L)
            .queryParam("to", 1735689599000L)
            .request()
            .get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data.get(0).getValue()).isEqualTo("gw-prod");
                assertThat(data.get(0).getId()).isNull();
            });
    }

    @Test
    void should_resolve_application_names_for_id_based_filter() {
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.APPLICATION),
                any(),
                any(),
                anyInt(),
                eq(10),
                any(),
                any(),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("app-id-1")), null));
        when(filterValueNameResolver.resolveNames(any(), eq(FilterSpec.Name.APPLICATION), any())).thenReturn(
            java.util.Map.of("app-id-1", "My Application")
        );

        var response = rootTarget("APPLICATION/values").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .extracting(FilterValuesResponse::getData)
            .satisfies(data -> {
                assertThat(data).hasSize(1);
                assertThat(data.get(0).getValue()).isEqualTo("My Application");
                assertThat(data.get(0).getId()).isEqualTo("app-id-1");
            });
    }

    @Test
    void should_search_plan_by_name_with_query() {
        planQueryServiceInMemory.reset();
        when(analyticsQueryContextLoader.load(any())).thenReturn(
            new AnalyticsQueryContext(null, new ExecutionContext(ORGANIZATION, ENVIRONMENT), Set.of("api-1"), Map.of(), Map.of(), Map.of())
        );
        // initWith uses Plan#copy(); definitionVersion must be set or copy() fails on switch (null).
        planQueryServiceInMemory.initWith(
            List.of(
                Plan.builder()
                    .id("plan-id-1")
                    .name("Gold Plan")
                    .definitionVersion(DefinitionVersion.V4)
                    .referenceId("api-1")
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .environmentId(ENVIRONMENT)
                    .build()
            )
        );

        try {
            var response = rootTarget("PLAN/values").queryParam("query", "Gold").request().get();

            assertThat(response)
                .hasStatus(200)
                .asEntity(FilterValuesResponse.class)
                .extracting(FilterValuesResponse::getData)
                .satisfies(data -> {
                    assertThat(data).hasSize(1);
                    assertThat(data.get(0).getValue()).isEqualTo("Gold Plan");
                    assertThat(data.get(0).getId()).isEqualTo("plan-id-1");
                });
        } finally {
            planQueryServiceInMemory.reset();
        }
    }

    @Test
    void should_return_pagination_for_keyword_filter_page_2() {
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.GATEWAY),
                any(),
                any(),
                eq(2),
                eq(5),
                any(),
                any(),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("gw-6"), new FilterValue("gw-7")), null, 12L));

        var response = rootTarget("GATEWAY/values").queryParam("page", 2).queryParam("perPage", 5).request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .satisfies(body -> {
                assertThat(body.getData()).hasSize(2);
                assertThat(body.getData()).extracting(FilterValueItem::getValue).containsExactly("gw-6", "gw-7");
                assertThat(body.getPagination().getPage()).isEqualTo(2);
                assertThat(body.getPagination().getPerPage()).isEqualTo(5);
                assertThat(body.getPagination().getPageItemsCount()).isEqualTo(2);
                assertThat(body.getPagination().getTotalCount()).isEqualTo(12L);
            });
    }

    @Test
    void should_return_pagination_for_enum_values() {
        var response = rootTarget("HTTP_METHOD/values").queryParam("page", 1).queryParam("perPage", 3).request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .satisfies(body -> {
                assertThat(body.getData()).hasSize(3);
                assertThat(body.getPagination().getPage()).isEqualTo(1);
                assertThat(body.getPagination().getPerPage()).isEqualTo(3);
                assertThat(body.getPagination().getPageItemsCount()).isEqualTo(3);
                assertThat(body.getPagination().getTotalCount()).isEqualTo(10L);
            });
    }

    @Test
    void should_return_400_for_per_page_out_of_range() {
        var response = rootTarget("HTTP_METHOD/values").queryParam("perPage", 101).request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_return_400_for_per_page_zero() {
        var response = rootTarget("HTTP_METHOD/values").queryParam("perPage", 0).request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_return_empty_page_for_keyword_filter_beyond_data() {
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.GATEWAY),
                any(),
                any(),
                eq(5),
                eq(10),
                any(),
                any(),
                any()
            )
        ).thenReturn(new FilterValuesPage(Collections.emptyList(), null, 3L));

        var response = rootTarget("GATEWAY/values").queryParam("page", 5).request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .satisfies(body -> {
                assertThat(body.getData()).isEmpty();
                assertThat(body.getPagination().getPage()).isEqualTo(5);
                assertThat(body.getPagination().getPageItemsCount()).isEqualTo(0);
                assertThat(body.getPagination().getTotalCount()).isEqualTo(3L);
            });
    }

    @Test
    void should_return_id_based_filter_values_on_page_2() {
        when(analyticsQueryContextLoader.load(any())).thenReturn(
            new AnalyticsQueryContext(
                null,
                new ExecutionContext(ORGANIZATION, ENVIRONMENT),
                Set.of(),
                Map.of("api-id-6", "My API 6"),
                Map.of(),
                Map.of()
            )
        );
        when(
            filterValuesQueryService.searchFilterValues(
                any(),
                any(),
                eq(FilterSpec.Name.API),
                any(),
                any(),
                eq(2),
                eq(5),
                any(),
                any(),
                any()
            )
        ).thenReturn(new FilterValuesPage(List.of(new FilterValue("api-id-6")), null, 11L));

        var response = rootTarget("API/values").queryParam("page", 2).queryParam("perPage", 5).request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterValuesResponse.class)
            .satisfies(body -> {
                assertThat(body.getData()).hasSize(1);
                assertThat(body.getData().get(0).getValue()).isEqualTo("My API 6");
                assertThat(body.getData().get(0).getId()).isEqualTo("api-id-6");
                assertThat(body.getPagination().getPage()).isEqualTo(2);
                assertThat(body.getPagination().getPerPage()).isEqualTo(5);
            });
    }
}
