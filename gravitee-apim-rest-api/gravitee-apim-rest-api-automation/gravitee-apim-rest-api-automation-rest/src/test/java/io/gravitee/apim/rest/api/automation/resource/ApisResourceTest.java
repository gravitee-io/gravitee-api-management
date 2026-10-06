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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.IDExportStrategy;
import io.gravitee.apim.core.api.use_case.ExportApiCRDUseCase;
import io.gravitee.apim.core.api.use_case.ExportEnvironmentApiCRDsUseCase;
import io.gravitee.apim.core.api.use_case.ImportApiCRDUseCase;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.helpers.SharedPolicyGroupIdHelper;
import io.gravitee.apim.rest.api.automation.model.ApiV4State;
import io.gravitee.apim.rest.api.automation.model.StepV4;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.definition.model.v4.analytics.tracing.MaskingType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class ApisResourceTest extends AbstractResourceTest {

    @Inject
    private ImportApiCRDUseCase importApiCRDUseCase;

    @Inject
    private ValidateApiCRDDomainService validateApiCRDDomainService;

    @Inject
    private ExportEnvironmentApiCRDsUseCase exportEnvironmentApiCRDsUseCase;

    @Inject
    private ExportApiCRDUseCase exportApiCRDUseCase;

    @AfterEach
    void tearDown() {
        reset(importApiCRDUseCase);
        reset(validateApiCRDDomainService);
        reset(exportEnvironmentApiCRDsUseCase);
        reset(exportApiCRDUseCase);
        groupQueryServiceInMemory.reset();
    }

    @Nested
    class ListAll {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private void listing(ApiCRDSpec... specs) {
            when(exportEnvironmentApiCRDsUseCase.execute(any(ExportEnvironmentApiCRDsUseCase.Input.class))).thenReturn(
                new ExportEnvironmentApiCRDsUseCase.Output(List.of(specs))
            );
        }

        private List<ApiV4State> list() {
            try (var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                return response.readEntity(new GenericType<List<ApiV4State>>() {});
            }
        }

        @Test
        void should_return_one_state_per_crd_with_its_ids_and_where_it_lives() {
            listing(
                ApiCRDSpec.builder().id("api-1").crossId("cross-1").hrid("api-one").name("API one").build(),
                ApiCRDSpec.builder().id("api-2").crossId("cross-2").hrid("api-two").name("API two").build()
            );

            var states = list();

            Assertions.assertThat(states)
                .extracting(ApiV4State::getId, ApiV4State::getCrossId, ApiV4State::getHrid, ApiV4State::getName)
                .containsExactly(
                    Assertions.tuple("api-1", "cross-1", "api-one", "API one"),
                    Assertions.tuple("api-2", "cross-2", "api-two", "API two")
                );
            SoftAssertions.assertSoftly(soft -> {
                for (var state : states) {
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getErrors()).isNull();
                }
            });
        }

        @Test
        void should_serialize_a_missing_hrid_as_null() throws Exception {
            listing(ApiCRDSpec.builder().id("console-api").crossId("cross").name("Console API").build());

            try (var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                var states = MAPPER.readTree(response.readEntity(String.class));
                Assertions.assertThat(states).hasSize(1);
                Assertions.assertThat(states.get(0).has("hrid")).isTrue();
                Assertions.assertThat(states.get(0).get("hrid").isNull()).isTrue();
            }
        }

        @Test
        void should_replace_group_names_with_their_hrids() {
            groupQueryServiceInMemory.initWith(
                List.of(
                    Group.builder().id("group-1").environmentId(ENVIRONMENT).name("Group One").hrid("group-one").build(),
                    Group.builder().id("group-2").environmentId(ENVIRONMENT).name("Group Two").hrid("group-two").build()
                )
            );
            listing(
                ApiCRDSpec.builder()
                    .id("api-1")
                    .crossId("cross-1")
                    .hrid("api-one")
                    .name("API one")
                    .groups(Set.of("Group One", "Group Two"))
                    .consoleNotificationConfiguration(PortalNotificationConfigEntity.builder().groups(List.of("Group Two")).build())
                    .build()
            );

            var state = list().getFirst();

            Assertions.assertThat(state.getGroups()).containsExactlyInAnyOrder("group-one", "group-two");
            Assertions.assertThat(state.getConsoleNotification().getGroups()).containsExactly("group-two");
        }

        @Test
        void should_strip_shared_policy_group_ids_from_flows() {
            var flow = Flow.builder()
                .request(
                    List.of(
                        Step.builder()
                            .name("No Op")
                            .policy(SharedPolicyGroupPolicyPlugin.SHARED_POLICY_GROUP_POLICY_ID)
                            .configuration(
                                """
                                { "sharedPolicyGroupId": "666", "hrid": "no-op" }
                                """
                            )
                            .build()
                    )
                )
                .build();
            listing(ApiCRDSpec.builder().id("api-1").crossId("cross-1").hrid("api-one").name("API one").flows(List.of(flow)).build());

            var state = list().getFirst();

            Assertions.assertThat(state.getFlows().getFirst().getRequest().getFirst())
                .extracting(StepV4::getConfiguration)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("hrid", "no-op")
                .doesNotContainKey("sharedPolicyGroupId");
        }

        @Test
        void should_export_with_every_id_and_notifications_for_the_environment() {
            listing();
            var input = ArgumentCaptor.forClass(ExportEnvironmentApiCRDsUseCase.Input.class);

            list();

            verify(exportEnvironmentApiCRDsUseCase).execute(input.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(input.getValue().idExportStrategy()).isEqualTo(IDExportStrategy.ALL);
                soft.assertThat(input.getValue().exportNotifications()).isTrue();
                soft.assertThat(input.getValue().auditInfo().environmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(input.getValue().auditInfo().organizationId()).isEqualTo(ORGANIZATION);
            });
        }

        @Test
        void should_return_an_empty_array_when_the_environment_has_no_api() throws Exception {
            listing();

            try (var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                var states = MAPPER.readTree(response.readEntity(String.class));
                Assertions.assertThat(states.isArray()).isTrue();
                Assertions.assertThat(states).isEmpty();
            }
        }

        @Test
        void should_return_403_and_never_export_when_environment_api_read_is_denied() {
            when(
                permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_API), any(), eq(RolePermissionAction.READ))
            ).thenReturn(false);

            try (var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                Assertions.assertThat(response.getStatus()).isEqualTo(403);
            }
            verifyNoInteractions(exportEnvironmentApiCRDsUseCase);
        }

        @Test
        void should_leave_the_item_read_to_the_api_resource() {
            when(exportApiCRDUseCase.execute(any(ExportApiCRDUseCase.Input.class))).thenReturn(
                new ExportApiCRDUseCase.Output(ApiCRDSpec.builder().id("api-1").crossId("cross-1").hrid("api-one").build())
            );

            try (var response = rootTarget("api-one").request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
                Assertions.assertThat(response.readEntity(ApiV4State.class).getHrid()).isEqualTo("api-one");
            }
            verifyNoInteractions(exportEnvironmentApiCRDsUseCase);
        }
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(importApiCRDUseCase.execute(any(ImportApiCRDUseCase.Input.class))).thenReturn(
                new ImportApiCRDUseCase.Output(
                    ApiCRDStatus.builder()
                        .id("api-id")
                        .crossId("api-cross-id")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("api-with-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_dotted_hrid() {
            var state = expectEntity("api-with-dotted-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid.dotted");
            });
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        void should_accept_lowercase_enum_values(boolean dryRun) {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class))).thenAnswer(call ->
                Validator.Result.ofValue(call.getArgument(0))
            );

            var state = expectEntity("api-with-lowercase-flow-mode.json", dryRun, false);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_guid() {
            when(importApiCRDUseCase.execute(any(ImportApiCRDUseCase.Input.class))).thenAnswer(call ->
                new ImportApiCRDUseCase.Output(
                    ApiCRDStatus.builder()
                        .id("api-hrid")
                        .crossId("api-cross-id")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );

            var state = expectEntity("api-with-hrid.json", false, true);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-hrid");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_state_from_cross_id_hrid() {
            var state = expectEntity("api-with-cross-id-and-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_cross_id_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", false)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }

        @Test
        void should_reject_edge_api_type() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", false)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-edge-type.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }

        @Test
        void should_reject_authz_api_type() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", false)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-authz-type.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }

        @Test
        void should_import_listener_tracing_and_failover_settings() {
            expectEntity("api-with-listener-tracing-and-failover-settings.json");

            var input = ArgumentCaptor.forClass(ImportApiCRDUseCase.Input.class);
            verify(importApiCRDUseCase).execute(input.capture());
            var spec = input.getValue().spec();
            var listener = (HttpListener) spec.getListeners().getFirst();
            var redaction = spec.getAnalytics().getTracing().getRedaction();
            var rule = redaction.getRules().getFirst();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(listener.getPathMappings()).containsExactly("/products/:productId");
                soft.assertThat(listener.getCors().isAllowPrivateNetwork()).isTrue();
                soft.assertThat(listener.getRequestValidation().isRejectNullByte()).isTrue();
                soft.assertThat(redaction.getDefaultReplacement()).isEqualTo("[MASKED]");
                soft.assertThat(rule.getAttributeNamePattern()).isEqualTo("http.request.header.authorization");
                soft.assertThat(rule.getValuePattern()).isEqualTo("^Bearer ");
                soft.assertThat(rule.getMaskingStrategy().getType()).isEqualTo(MaskingType.PARTIAL);
                soft.assertThat(rule.getMaskingStrategy().getReplacement()).isEqualTo("#");
                soft.assertThat(rule.getMaskingStrategy().getPrefixLength()).isEqualTo(7);
                soft.assertThat(rule.getMaskingStrategy().getSuffixLength()).isEqualTo(2);
                soft.assertThat(spec.getFailover().getFailureCondition()).isEqualTo("{#response.status >= 500}");
                soft.assertThat(spec.getFailover().isForceNextEndpointOnFailure()).isTrue();
            });
        }

        @Test
        void should_return_400_and_no_fqcn_when_use_case_throws_validation_domain_exception() {
            when(importApiCRDUseCase.execute(any(ImportApiCRDUseCase.Input.class))).thenThrow(
                new io.gravitee.apim.core.exception.ValidationDomainException("bad payload")
            );
            try (
                var response = rootTarget()
                    .queryParam("dryRun", false)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-hrid.json")))
            ) {
                var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Error.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(response.getStatus()).isEqualTo(400);
                    soft.assertThat(body.getMessage()).doesNotContain("io.gravitee.apim.core");
                    soft.assertThat(body.getMessage()).contains("bad payload");
                });
            }
        }

        @Test
        void should_return_state_from_hrid_and_have_spg_hrid_replaced() {
            var state = expectEntity("api-with-hrid-spg-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getId()).isEqualTo("api-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });

            verify(importApiCRDUseCase, atMostOnce()).execute(
                argThat(
                    input ->
                        input
                            .spec()
                            .getPlans()
                            .values()
                            .stream()
                            .flatMap(p -> p.getFlows().stream())
                            .map(f -> (Flow) f)
                            .flatMap(f ->
                                Stream.concat(
                                    Stream.concat(f.getRequest().stream(), f.getResponse().stream()),
                                    Stream.concat(f.getSubscribe().stream(), f.getPublish().stream())
                                )
                            )
                            .map(Step::getConfiguration)
                            .allMatch(assertion()) &&
                        input
                            .spec()
                            .getFlows()
                            .stream()
                            .map(f -> (Flow) f)
                            .flatMap(f ->
                                Stream.concat(
                                    Stream.concat(f.getRequest().stream(), f.getResponse().stream()),
                                    Stream.concat(f.getSubscribe().stream(), f.getPublish().stream())
                                )
                            )
                            .map(Step::getConfiguration)
                            .allMatch(assertion())
                )
            );
        }

        @Nonnull
        private static Predicate<String> assertion() {
            return s ->
                s.contains(SharedPolicyGroupIdHelper.SHARED_POLICY_GROUP_ID_FIELD) &&
                !s.contains(ApisResource.HRID_FIELD) &&
                !s.contains("test-spg-hrid");
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_hrid() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class))).thenAnswer(call ->
                Validator.Result.ofValue(call.getArgument(0))
            );

            var state = expectEntity("api-with-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(state.getCrossId())
                    .isEqualTo(HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid("api-hrid").crossId());
                soft
                    .assertThat(state.getId())
                    .isEqualTo(HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid("api-hrid").id());
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
            });
        }

        @Test
        void should_return_state_from_cross_id() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class))).thenAnswer(call ->
                Validator.Result.ofValue(call.getArgument(0))
            );

            var state = expectEntity("api-with-cross-id-and-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(state.getId())
                    .isEqualTo(HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid("api-hrid").id());
                soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                soft.assertThat(state.getHrid()).isEqualTo("api-hrid");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_state_with_guid() {
            when(validateApiCRDDomainService.validateAndSanitize(any(ValidateApiCRDDomainService.Input.class))).thenAnswer(call ->
                Validator.Result.ofValue(call.getArgument(0))
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .queryParam("hridContainsUUID", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-uuid.json")));
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(ApiV4State.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo("api-id");
                    soft.assertThat(state.getCrossId()).isEqualTo("api-cross-id");
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_return_state_from_cross_id_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }

        @Test
        void should_reject_edge_api_type() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-edge-type.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }

        @Test
        void should_reject_authz_api_type() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-with-authz-type.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis";
    }

    private ApiV4State expectEntity(String spec) {
        return expectEntity(spec, false, false);
    }

    private ApiV4State expectEntity(String spec, boolean dryRun) {
        return expectEntity(spec, dryRun, false);
    }

    private ApiV4State expectEntity(String spec, boolean dryRun, boolean hridContainsUUID) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("hridContainsUUID", hridContainsUUID)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(ApiV4State.class);
        }
    }
}
