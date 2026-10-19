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
package io.gravitee.gamma.rest.resource.observability.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.filter.exception.ObservabilityFilterNotFoundException;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.GetObservabilityFilterValuesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.use_case.ResolveObservabilityFilterLabelsUseCase;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.filters.ObservabilityFiltersResourceTest.FiltersTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, FiltersTestConfiguration.class })
class ObservabilityFiltersResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";

    @Inject
    private GetObservabilityFilterDefinitionsUseCase getFilterDefinitionsUseCase;

    @Inject
    private GetObservabilityFilterValuesUseCase getFilterValuesUseCase;

    @Inject
    private ResolveObservabilityFilterLabelsUseCase resolveFilterLabelsUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/filters";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetUseCases() {
        reset(getFilterDefinitionsUseCase, getFilterValuesUseCase, resolveFilterLabelsUseCase);
    }

    @Nested
    class GetFilterDefinitions {

        @Test
        void should_return_data_envelope_with_lowercase_type_uppercase_operators_labelled_enum_values_and_both_axes() {
            FilterSpec spec = new FilterSpec(
                "API_TYPE",
                "API Type",
                FilterType.ENUM,
                List.of(FilterOperator.EQ, FilterOperator.IN),
                List.of(new FilterSpec.EnumValue("NATIVE", "Kafka (native)")),
                null,
                Set.of(Signal.LOGS, Signal.ANALYTICS),
                Set.of(ApiType.HTTP_PROXY, ApiType.LLM)
            );
            when(getFilterDefinitionsUseCase.execute(any())).thenReturn(new GetObservabilityFilterDefinitionsUseCase.Output(List.of(spec)));

            Response response = rootTarget("definition").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            JsonNode first = body.get("data").get(0);
            assertThat(first.get("name").asText()).isEqualTo("API_TYPE");
            assertThat(first.get("type").asText()).isEqualTo("enum");
            assertThat(first.get("operators")).extracting(JsonNode::asText).containsExactly("EQ", "IN");
            JsonNode enumValue = first.get("enumValues").get(0);
            assertThat(enumValue.get("value").asText()).isEqualTo("NATIVE");
            assertThat(enumValue.get("label").asText()).isEqualTo("Kafka (native)");
            assertThat(first.get("signals")).extracting(JsonNode::asText).containsExactlyInAnyOrder("LOGS", "ANALYTICS");
            assertThat(first.get("apiTypes")).extracting(JsonNode::asText).containsExactlyInAnyOrder("HTTP_PROXY", "LLM");
            assertThat(first.has("range")).isFalse();
        }

        @Test
        void should_forward_multi_valued_signal_and_api_type_axes_to_the_use_case() {
            when(getFilterDefinitionsUseCase.execute(any())).thenReturn(new GetObservabilityFilterDefinitionsUseCase.Output(List.of()));

            Response response = rootTarget("definition")
                .queryParam("signal", "LOGS")
                .queryParam("signal", "analytics")
                .queryParam("apiType", "LLM")
                .request()
                .get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<GetObservabilityFilterDefinitionsUseCase.Input> captor = ArgumentCaptor.forClass(
                GetObservabilityFilterDefinitionsUseCase.Input.class
            );
            Mockito.verify(getFilterDefinitionsUseCase).execute(captor.capture());
            assertThat(captor.getValue().signals()).containsExactlyInAnyOrder(Signal.LOGS, Signal.ANALYTICS);
            assertThat(captor.getValue().apiTypes()).containsExactly(ApiType.LLM);
        }

        @Test
        void should_return_full_catalog_when_no_axis_is_provided() {
            when(getFilterDefinitionsUseCase.execute(any())).thenReturn(new GetObservabilityFilterDefinitionsUseCase.Output(List.of()));

            Response response = rootTarget("definition").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<GetObservabilityFilterDefinitionsUseCase.Input> captor = ArgumentCaptor.forClass(
                GetObservabilityFilterDefinitionsUseCase.Input.class
            );
            Mockito.verify(getFilterDefinitionsUseCase).execute(captor.capture());
            assertThat(captor.getValue().signals()).isEmpty();
            assertThat(captor.getValue().apiTypes()).isEmpty();
        }

        @Test
        void should_return_400_for_an_unknown_signal_value() {
            Response response = rootTarget("definition").queryParam("signal", "NOPE").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            Mockito.verifyNoInteractions(getFilterDefinitionsUseCase);
        }

        /** The catalog holds no traffic data. */
        @Test
        void should_return_the_catalog_to_a_caller_without_any_permission() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            when(getFilterDefinitionsUseCase.execute(any())).thenReturn(new GetObservabilityFilterDefinitionsUseCase.Output(List.of()));

            Response response = rootTarget("definition").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }
    }

    @Nested
    class GetFilterValues {

        @Test
        void should_return_200_with_paginated_envelope_for_enum_filter() {
            FilterValuesPage page = new FilterValuesPage(
                List.of(new FilterValue("NATIVE", "Kafka (native)"), new FilterValue("GET", "GET")),
                2L
            );
            when(getFilterValuesUseCase.execute(any())).thenReturn(new GetObservabilityFilterValuesUseCase.Output(page, 1, 10));

            Response response = rootTarget("API_TYPE/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(2);
            assertThat(body.get("data").get(0).get("value").asText()).isEqualTo("NATIVE");
            assertThat(body.get("data").get(0).get("label").asText()).isEqualTo("Kafka (native)");
            // label collapsed to absent when identical to value.
            assertThat(body.get("data").get(1).has("label")).isFalse();
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(2L);
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(1);
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_build_envelope_from_the_resolved_pagination_not_the_raw_query_params() {
            // Caller asks for perPage=500 but the use case clamps it to 100; the envelope must reflect the clamped value.
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 250L), 1, 100)
            );

            Response response = rootTarget("API_TYPE/values").queryParam("perPage", 500).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("pagination").get("perPage").asInt()).isEqualTo(100);
            // pageCount derives from the clamped perPage: ceil(250 / 100) = 3, not ceil(250 / 500) = 1.
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(3);
        }

        @Test
        void should_forward_query_page_perPage_to_the_use_case() {
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 2, 5)
            );

            rootTarget("API_TYPE/values").queryParam("query", "ka").queryParam("page", 2).queryParam("perPage", 5).request().get();

            ArgumentCaptor<GetObservabilityFilterValuesUseCase.Input> captor = ArgumentCaptor.forClass(
                GetObservabilityFilterValuesUseCase.Input.class
            );
            Mockito.verify(getFilterValuesUseCase).execute(captor.capture());
            assertThat(captor.getValue().filterName()).isEqualTo("API_TYPE");
            assertThat(captor.getValue().query()).isEqualTo("ka");
            assertThat(captor.getValue().page()).isEqualTo(2);
            assertThat(captor.getValue().perPage()).isEqualTo(5);
        }

        @Test
        void should_return_404_when_filter_is_unknown() {
            when(getFilterValuesUseCase.execute(any())).thenThrow(new ObservabilityFilterNotFoundException("UNKNOWN"));

            Response response = rootTarget("UNKNOWN/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_400_for_unsupported_value_listing() {
            when(getFilterValuesUseCase.execute(any())).thenThrow(
                UnsupportedObservabilityFilterException.valueListingNotSupported("HTTP_STATUS", "NUMBER")
            );

            Response response = rootTarget("HTTP_STATUS/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_forward_apiType_query_params_to_the_use_case() {
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            rootTarget("API_TYPE/values").queryParam("apiType", "MCP").queryParam("apiType", "LLM").request().get();

            ArgumentCaptor<GetObservabilityFilterValuesUseCase.Input> captor = ArgumentCaptor.forClass(
                GetObservabilityFilterValuesUseCase.Input.class
            );
            Mockito.verify(getFilterValuesUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiTypes()).containsExactlyInAnyOrder(ApiType.MCP, ApiType.LLM);
        }

        @Test
        void should_forward_the_api_product_the_request_is_scoped_to() {
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            rootTarget("APPLICATION/values").queryParam("apiProduct", "workspace-1").request().get();

            ArgumentCaptor<GetObservabilityFilterValuesUseCase.Input> captor = ArgumentCaptor.forClass(
                GetObservabilityFilterValuesUseCase.Input.class
            );
            Mockito.verify(getFilterValuesUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiProduct()).isEqualTo("workspace-1");
        }

        @Test
        void should_read_a_blank_api_product_as_absent() {
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            rootTarget("APPLICATION/values").queryParam("apiProduct", " ").request().get();

            ArgumentCaptor<GetObservabilityFilterValuesUseCase.Input> captor = ArgumentCaptor.forClass(
                GetObservabilityFilterValuesUseCase.Input.class
            );
            Mockito.verify(getFilterValuesUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiProduct()).isNull();
        }

        @Test
        void should_return_400_for_unknown_apiType_value() {
            Response response = rootTarget("API_TYPE/values").queryParam("apiType", "NOPE").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            Mockito.verifyNoInteractions(getFilterValuesUseCase);
        }

        /** The values come from the rows the caller reads: no environment permission is required. */
        @Test
        void should_list_values_without_any_environment_permission() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            Response response = rootTarget("APPLICATION/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            Mockito.verify(getFilterValuesUseCase).execute(any());
        }

        @Test
        void should_answer_from_the_rows_when_caller_only_holds_environment_api_read() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_API, ENVIRONMENT);
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            Response response = rootTarget("API_TYPE/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            Mockito.verify(getFilterValuesUseCase).execute(any());
        }

        @Test
        void should_answer_from_the_rows_when_environment_platform_read_is_held_on_another_environment() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, OTHER_ENVIRONMENT);
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            Response response = rootTarget("API_TYPE/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            Mockito.verify(getFilterValuesUseCase).execute(any());
        }

        @Test
        void should_return_200_when_caller_holds_environment_platform_read() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, ENVIRONMENT);
            when(getFilterValuesUseCase.execute(any())).thenReturn(
                new GetObservabilityFilterValuesUseCase.Output(new FilterValuesPage(List.of(), 0L), 1, 10)
            );

            Response response = rootTarget("API_TYPE/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }
    }

    @Nested
    class ResolveFilterLabels {

        @Test
        void should_return_entries_with_labels_grouped_by_filter() {
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(
                new ResolveObservabilityFilterLabelsUseCase.Output(
                    List.of(new ResolveObservabilityFilterLabelsUseCase.ResolvedEntry("API", Map.of("api-1", "Petstore")))
                )
            );

            Response response = rootTarget("resolve")
                .request()
                .post(
                    Entity.entity(
                        Map.of("entries", List.of(Map.of("filterName", "API", "ids", List.of("api-1")))),
                        MediaType.APPLICATION_JSON_TYPE
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("entries")).hasSize(1);
            assertThat(body.get("entries").get(0).get("filterName").asText()).isEqualTo("API");
            assertThat(body.get("entries").get(0).get("labels").get("api-1").asText()).isEqualTo("Petstore");
        }

        @Test
        void should_forward_entries_to_the_use_case() {
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            rootTarget("resolve")
                .request()
                .post(
                    Entity.entity(
                        Map.of("entries", List.of(Map.of("filterName", "PLAN", "ids", List.of("plan-1", "plan-2")))),
                        MediaType.APPLICATION_JSON_TYPE
                    )
                );

            ArgumentCaptor<ResolveObservabilityFilterLabelsUseCase.Input> captor = ArgumentCaptor.forClass(
                ResolveObservabilityFilterLabelsUseCase.Input.class
            );
            Mockito.verify(resolveFilterLabelsUseCase).execute(captor.capture());
            assertThat(captor.getValue().entries())
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.filterName()).isEqualTo("PLAN");
                    assertThat(e.ids()).containsExactly("plan-1", "plan-2");
                });
        }

        @Test
        void should_forward_the_api_product_the_labelled_request_is_scoped_to() {
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            Response response = rootTarget("resolve")
                .queryParam("apiProduct", "workspace-1")
                .request()
                .post(
                    Entity.entity(
                        Map.of("entries", List.of(Map.of("filterName", "APPLICATION", "ids", List.of("app-1")))),
                        MediaType.APPLICATION_JSON_TYPE
                    )
                );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<ResolveObservabilityFilterLabelsUseCase.Input> captor = ArgumentCaptor.forClass(
                ResolveObservabilityFilterLabelsUseCase.Input.class
            );
            Mockito.verify(resolveFilterLabelsUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiProduct()).isEqualTo("workspace-1");
        }

        @Test
        void should_read_a_blank_api_product_as_absent() {
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            rootTarget("resolve")
                .queryParam("apiProduct", "")
                .request()
                .post(
                    Entity.entity(
                        Map.of("entries", List.of(Map.of("filterName", "APPLICATION", "ids", List.of("app-1")))),
                        MediaType.APPLICATION_JSON_TYPE
                    )
                );

            ArgumentCaptor<ResolveObservabilityFilterLabelsUseCase.Input> captor = ArgumentCaptor.forClass(
                ResolveObservabilityFilterLabelsUseCase.Input.class
            );
            Mockito.verify(resolveFilterLabelsUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiProduct()).isNull();
        }

        @Test
        void should_return_200_to_the_library_resolve_payload() {
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));
            var body = Map.of(
                "entries",
                List.of(
                    Map.of("filterName", "API", "ids", List.of("api-1", "api-2")),
                    Map.of("filterName", "APPLICATION", "ids", List.of("app-1"))
                )
            );

            Response response = rootTarget("resolve").request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @ParameterizedTest
        @CsvSource({ "root, apiId", "entry, filtername" })
        void should_return_400_naming_an_unknown_key(String location, String unknownKey) {
            var entry = new HashMap<String, Object>(Map.of("filterName", "API", "ids", List.of("api-1")));
            var body = new HashMap<String, Object>(Map.of("entries", List.of(entry)));
            (location.equals("root") ? body : entry).put(unknownKey, "x");

            Response response = rootTarget("resolve").request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).isEqualTo(
                "Property [" + unknownKey + "] is not recognized as a valid property"
            );
            Mockito.verifyNoInteractions(resolveFilterLabelsUseCase);
        }

        /** Only ids found in the rows the caller reads are labelled: no environment permission is required. */
        @Test
        void should_resolve_labels_without_any_environment_permission() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            Response response = rootTarget("resolve")
                .request()
                .post(Entity.entity(Map.of("entries", List.of()), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            Mockito.verify(resolveFilterLabelsUseCase).execute(any());
        }

        @Test
        void should_answer_from_the_rows_when_caller_only_holds_environment_api_read() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_API, ENVIRONMENT);
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            Response response = rootTarget("resolve")
                .request()
                .post(Entity.entity(Map.of("entries", List.of()), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            Mockito.verify(resolveFilterLabelsUseCase).execute(any());
        }

        @Test
        void should_answer_from_the_rows_when_environment_platform_read_is_held_on_another_environment() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, OTHER_ENVIRONMENT);
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            Response response = rootTarget("resolve")
                .request()
                .post(Entity.entity(Map.of("entries", List.of()), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            Mockito.verify(resolveFilterLabelsUseCase).execute(any());
        }

        @Test
        void should_return_200_when_caller_holds_environment_platform_read() {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, ENVIRONMENT);
            when(resolveFilterLabelsUseCase.execute(any())).thenReturn(new ResolveObservabilityFilterLabelsUseCase.Output(List.of()));

            Response response = rootTarget("resolve")
                .request()
                .post(Entity.entity(Map.of("entries", List.of()), MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }
    }

    @Configuration
    static class FiltersTestConfiguration {

        @Bean
        GetObservabilityFilterDefinitionsUseCase getObservabilityFilterDefinitionsUseCase() {
            return mock(GetObservabilityFilterDefinitionsUseCase.class);
        }

        @Bean
        GetObservabilityFilterValuesUseCase getObservabilityFilterValuesUseCase() {
            return mock(GetObservabilityFilterValuesUseCase.class);
        }

        @Bean
        ResolveObservabilityFilterLabelsUseCase resolveObservabilityFilterLabelsUseCase() {
            return mock(ResolveObservabilityFilterLabelsUseCase.class);
        }
    }
}
