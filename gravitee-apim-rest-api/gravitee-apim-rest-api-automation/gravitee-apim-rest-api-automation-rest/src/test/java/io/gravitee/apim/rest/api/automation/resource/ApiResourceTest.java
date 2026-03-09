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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.model.PlanSecurityType;
import io.gravitee.apim.rest.api.automation.model.PlanV4;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.v4.ApiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

        @ParameterizedTest
        @MethodSource("securityTypes")
        void should_get_api_with_security_plan_type(String securityType, PlanSecurityType expectedType) {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));

                when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenReturn(
                    new ExportApiCRDUseCase.Output(
                        ApiCRDSpec.builder()
                            .id(API_ID)
                            .crossId(API_CROSS_ID)
                            .hrid(HRID)
                            .plans(Map.of("plan", PlanCRD.builder().security(PlanSecurity.builder().type(securityType).build()).build()))
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
                        .isEqualTo(expectedType);
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

        private static Stream<Arguments> securityTypes() {
            return Stream.of(
                Arguments.of("API_KEY", PlanSecurityType.API_KEY),
                Arguments.of("KEY_LESS", PlanSecurityType.KEY_LESS),
                Arguments.of("OAUTH2", PlanSecurityType.OAUTH2),
                Arguments.of("JWT", PlanSecurityType.JWT),
                Arguments.of("MTLS", PlanSecurityType.MTLS)
            );
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
