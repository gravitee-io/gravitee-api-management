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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.rest.api.automation.helpers.HRIDHelper;
import io.gravitee.apim.rest.api.automation.helpers.SharedPolicyGroupIdHelper;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.model.PlanV4;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.v4.ApiService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiResourceTest extends AbstractResourceTest {

    @Inject
    private ExportApiCRDUseCase exportApiCRDUseCase;

    @Inject
    private ApiService apiService;

    static final String API_ID = "api-id";
    static final String API_CROSS_ID = "api-cross-id";
    static final String HRID = "api-hrid";
    static final AuditInfo auditInfo = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();

    @AfterEach
    void tearDown() {
        reset(exportApiCRDUseCase);
    }

    @Nested
    class GET {

        @Test
        void should_get_api_from_known_hrid() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));
                when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenReturn(
                    new ExportApiCRDUseCase.Output(ApiCRDSpec.builder().id(API_ID).crossId(API_CROSS_ID).hrid(HRID).build())
                );
                var state = expectEntity(HRID, false);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(API_ID);
                    assertThat(state.getHrid()).isEqualTo(HRID);
                    assertThat(state.getCrossId()).isEqualTo(API_CROSS_ID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_get_api_from_known_legacy_id() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));
                when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenReturn(
                    new ExportApiCRDUseCase.Output(ApiCRDSpec.builder().id(API_ID).crossId(API_CROSS_ID).hrid(API_ID).build())
                );
                var state = expectEntity(API_ID, true);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(API_ID);
                    assertThat(state.getHrid()).isEqualTo(API_ID);
                    assertThat(state.getCrossId()).isEqualTo(API_CROSS_ID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_get_api_from_known_id_and_populate_hrid() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));
                Map<String, @Valid PageCRD> pages = new LinkedHashMap<>();
                pages.put("Home", PageCRD.builder().id("pages-abc").name("Home").build());
                pages.put("General Conditions", PageCRD.builder().id("pages-xyz").parentId("pages-abc").name("General Conditions").build());
                var flow = List.of(
                    Flow.builder()
                        .request(
                            List.of(
                                Step.builder()
                                    .name("No Op")
                                    .policy(SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID)
                                    .configuration(
                                        """
                                        {
                                            "sharedPolicyGroupId": "666"
                                        }
                                        """
                                    )
                                    .build()
                            )
                        )
                        .build()
                );
                var planFlow = List.copyOf(flow);
                when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenReturn(
                    new ExportApiCRDUseCase.Output(
                        ApiCRDSpec.builder()
                            .id(API_ID)
                            .crossId(API_CROSS_ID)
                            .name("Test HRID")
                            .plans(
                                Map.of(
                                    "Keyless plan",
                                    PlanCRD.builder().name("Keyless plan").generalConditions("pages-xyz").flows(planFlow).build()
                                )
                            )
                            .pages(pages)
                            .flows(flow)
                            .build()
                    )
                );
                var state = expectEntityFromUUIDWithPopulatedHRIDs(API_ID);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(API_ID);
                    assertThat(state.getHrid()).isEqualTo("test-hrid");
                    assertThat(state.getCrossId()).isEqualTo(API_CROSS_ID);
                    assertThat(state.getPlans()).isNotEmpty();
                    assertThat(state.getPlans())
                        .first()
                        .extracting("hrid", "generalConditionsHrid")
                        .containsExactly("keyless-plan", "general-conditions");
                    assertThat(state.getPages())
                        .extracting("hrid", "parentHrid")
                        .containsExactly(Tuple.tuple("home", null), Tuple.tuple("general-conditions", "home"));
                    assertThat(state.getFlows()).isNotEmpty();
                    assertThat(state.getFlows().getFirst().getRequest()).isNotEmpty();
                    assertThat(state.getFlows().getFirst().getRequest().getFirst())
                        .extracting(StepV4::getConfiguration)
                        .asInstanceOf(MAP)
                        .containsEntry("hrid", "no-op")
                        .doesNotContainKey("sharedPolicyGroupId");
                    assertThat(state.getPlans().getFirst().getFlows()).isNotEmpty();
                    assertThat(state.getPlans().getFirst().getFlows().getFirst().getRequest()).isNotEmpty();
                    assertThat(state.getPlans().getFirst().getFlows().getFirst().getRequest().getFirst())
                        .extracting(StepV4::getConfiguration)
                        .asInstanceOf(MAP)
                        .containsEntry("hrid", "no-op")
                        .doesNotContainKey("sharedPolicyGroupId");
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_get_api_with_null_plan_type() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));
                when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenReturn(
                    new ExportApiCRDUseCase.Output(
                        ApiCRDSpec.builder()
                            .id(API_ID)
                            .crossId(API_CROSS_ID)
                            .hrid(HRID)
                            .plans(Map.of("apikey", PlanCRD.builder().security(PlanSecurity.builder().type("API_KEY").build()).build()))
                            .build()
                    )
                );
                var state = expectEntity(HRID, false);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(API_ID);
                    assertThat(state.getHrid()).isEqualTo(HRID);
                    assertThat(state.getCrossId()).isEqualTo(API_CROSS_ID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    assertThat(state.getPlans()).hasSize(1);
                    assertThat(state.getPlans().stream().findFirst())
                        .get()
                        .extracting(PlanV4::getSecurity)
                        .extracting(io.gravitee.apim.rest.api.automation.model.PlanSecurity::getType)
                        .isNull();
                });
            }
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenThrow(
                new NotFoundException("No API found with hrid: unknown")
            );

            expectNotFound("unknown");
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        private ApiV4State expectEntity(String hrid, boolean legacy) {
            try (
                var response = rootTarget().queryParam("legacy", legacy).path(hrid).request().accept(MediaType.APPLICATION_JSON_TYPE).get()
            ) {
                return response.readEntity(ApiV4State.class);
            }
        }

        private ApiV4State expectEntityFromUUIDWithPopulatedHRIDs(String hrid) {
            try (
                var response = rootTarget()
                    .path(hrid)
                    .request()
                    .header(HRIDHelper.HEADER_X_GRAVITEE_SET_HRID, true)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get()
            ) {
                return response.readEntity(ApiV4State.class);
            }
        }
    }

    @Nested
    class DELETE {

        @Test
        void should_delete_api_and_return_no_content() {
            expectNoContent(HRID);

            verify(apiService, atLeastOnce()).delete(any(), eq(IdBuilder.builder(auditInfo, HRID).buildId()), eq(true));
        }

        @Test
        void should_delete_api_and_return_no_content_with_valid_legacy_id() {
            expectNoContent(API_ID, true);

            verify(apiService, atLeastOnce()).delete(any(), eq(API_ID), eq(true));
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            doThrow(new ApiNotFoundException("unknown")).when(apiService).delete(any(), any(), eq(true));

            expectNotFound("unknown");
        }

        private void expectNoContent(String hrid) {
            expectNoContent(hrid, false);
        }

        private void expectNoContent(String hrid, boolean legacy) {
            try (var response = rootTarget().queryParam("legacy", legacy).path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis";
    }
}
