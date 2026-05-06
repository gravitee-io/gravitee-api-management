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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiFlowsTest extends ApiResourceTest {

    private static final String MERGE_PATCH_TYPE = "application/merge-patch+json";
    private static final String JSON_PATCH_TYPE = "application/json-patch+json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    UpdateApiDomainService updateApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void setUpApiAndPrimaryOwner() {
        givenApiWithFlows(List.of());

        var apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setUpdatedAt(Date.from(Instant.ofEpochMilli(1000)));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            apiEntity
        );

        roleQueryService.resetSystemRoles(ORGANIZATION);
        primaryOwnerDomainService.initWith(
            List.of(Map.entry(API, PrimaryOwnerEntity.builder().id(USER_NAME).type(PrimaryOwnerEntity.Type.USER).build()))
        );
        membershipQueryServiceInMemory.initWith(
            List.of(
                Membership.builder()
                    .memberId(USER_NAME)
                    .referenceId(API)
                    .roleId("api-po-id-" + ORGANIZATION)
                    .referenceType(Membership.ReferenceType.API)
                    .memberType(Membership.Type.USER)
                    .build()
            )
        );

        doAnswer(inv -> inv.getArgument(0))
            .when(updateApiDomainService)
            .updateV4(any(), any());
        doAnswer(inv -> inv.getArgument(0))
            .when(updateApiDomainService)
            .validateV4(any(), any());
    }

    @AfterEach
    public void tearDown() {
        Stream.of(apiCrudService, membershipQueryServiceInMemory, primaryOwnerDomainService).forEach(InMemoryAlternative::reset);
        reset(updateApiDomainService, apiSearchServiceV4);
    }

    private void givenApiWithFlows(List<Flow> flows) {
        var base = ApiFixtures.aProxyApiV4();
        var api = base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().flows(flows).build()).build();
        apiCrudService.initWith(List.of(api));
    }

    private static Step step(String name, String policy) {
        return Step.builder().name(name).policy(policy).enabled(true).build();
    }

    private static Flow flow(String name, List<Step> request) {
        return Flow.builder().name(name).enabled(true).request(request).build();
    }

    private static String mergePatchFlows(List<Map<String, Object>> flows) {
        return "{\"flows\":" + toJson(flows) + "}";
    }

    private static String jsonPatchReplaceFlows(List<Map<String, Object>> flows) {
        return "[{\"op\":\"replace\",\"path\":\"/flows\",\"value\":" + toJson(flows) + "}]";
    }

    private static Stream<Arguments> bothFlowsReplaceVariants(List<Map<String, Object>> flows) {
        return Stream.of(
            Arguments.of(mergePatchFlows(flows), MERGE_PATCH_TYPE),
            Arguments.of(jsonPatchReplaceFlows(flows), JSON_PATCH_TYPE)
        );
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> stepMap(String name, String policy) {
        return Map.of("name", name, "policy", policy, "enabled", true);
    }

    private static Map<String, Object> flowMap(String name, List<Map<String, Object>> request) {
        return Map.of("name", name, "enabled", true, "request", request);
    }

    private ApiV4 patchAndAssertOk(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));
        return assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
    }

    static Stream<Arguments> addFlowVariants() {
        return bothFlowsReplaceVariants(List.of(flowMap("added", List.of(stepMap("s1", "policy-a")))));
    }

    @ParameterizedTest
    @MethodSource("addFlowVariants")
    void patch_adding_a_flow_returns_200_and_response_reflects_the_new_flow(String body, String contentType) {
        givenApiWithFlows(List.of());

        var apiV4 = patchAndAssertOk(body, contentType);

        Assertions.assertThat(apiV4.getFlows()).hasSize(1);
        var flow = apiV4.getFlows().getFirst();
        Assertions.assertThat(flow.getName()).isEqualTo("added");
        Assertions.assertThat(flow.getRequest()).hasSize(1);
        Assertions.assertThat(flow.getRequest().getFirst().getPolicy()).isEqualTo("policy-a");
    }

    @ParameterizedTest
    @MethodSource("addFlowVariants")
    void caller_without_api_definition_update_permission_receives_403_on_flows_patch(String body, String contentType) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_GATEWAY_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Nested
    class Validation {

        static Stream<Arguments> unknownSelectorTypeVariants() {
            var flows = List.of(
                Map.of(
                    "name",
                    "f1",
                    "enabled",
                    true,
                    "selectors",
                    List.of(Map.of("type", "unknown-type", "path", "/api")),
                    "request",
                    List.of(stepMap("s1", "p1"))
                )
            );
            return bothFlowsReplaceVariants(flows);
        }

        static Stream<Arguments> uppercaseSelectorTypeVariants() {
            var flows = List.of(
                Map.of(
                    "name",
                    "f1",
                    "enabled",
                    true,
                    "selectors",
                    List.of(Map.of("type", "HTTP", "path", "/api", "pathOperator", "EQUALS")),
                    "request",
                    List.of(stepMap("s1", "p1"))
                )
            );
            return bothFlowsReplaceVariants(flows);
        }

        static Stream<Arguments> validatorRejectionCases() {
            return Stream.of(
                Map.entry("unknown policy id", "/flows/0/request/0/policy"),
                Map.entry("missing step name", "/flows/0/request/0/name"),
                Map.entry("invalid condition expression", "/flows/0/request/0/condition")
            ).flatMap(entry ->
                Stream.of(MERGE_PATCH_TYPE, JSON_PATCH_TYPE).map(contentType -> Arguments.of(entry.getKey(), entry.getValue(), contentType))
            );
        }

        @ParameterizedTest
        @MethodSource("unknownSelectorTypeVariants")
        void patch_with_unknown_selector_type_returns_400_with_invalidValue_envelope_and_field_path_location(
            String body,
            String contentType
        ) {
            givenApiWithFlows(List.of(flow("f1", List.of(step("s1", "p1")))));

            var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

            var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
            Assertions.assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
            Assertions.assertThat(error.getParameters()).containsKey("location");
            Assertions.assertThat(error.getParameters().get("location")).startsWith("/flows");
        }

        @ParameterizedTest
        @MethodSource("uppercaseSelectorTypeVariants")
        void patch_with_uppercase_http_selector_type_returns_400_with_invalidValue_envelope(String body, String contentType) {
            givenApiWithFlows(List.of(flow("f1", List.of(step("s1", "p1")))));

            var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

            var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
            Assertions.assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
            Assertions.assertThat(error.getParameters()).containsKey("location");
            Assertions.assertThat(error.getParameters().get("location")).startsWith("/flows");
        }

        @ParameterizedTest
        @MethodSource("validatorRejectionCases")
        void post_patch_validator_failure_returns_400_with_invalidValue_envelope_and_location(
            String message,
            String location,
            String contentType
        ) {
            givenApiWithFlows(List.of(flow("f1", List.of(step("s1", "policy-a")))));
            doThrow(new ValidationDomainException(message, Map.of("location", location), "invalidValue"))
                .when(updateApiDomainService)
                .updateV4(any(), any());

            var flows = List.of(flowMap("f1", List.of(stepMap("s1", "broken-policy"))));
            var body = contentType.equals(JSON_PATCH_TYPE) ? jsonPatchReplaceFlows(flows) : mergePatchFlows(flows);
            var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

            var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
            Assertions.assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
            Assertions.assertThat(error.getParameters()).containsEntry("location", location);
        }

        @Test
        void json_patch_remove_targeting_non_existent_flow_index_returns_400_with_field_path_pointing_error() {
            givenApiWithFlows(List.of(flow("f1", List.of(step("s1", "p1")))));

            var response = rootTarget(API)
                .request()
                .method("PATCH", Entity.entity("[{\"op\":\"remove\",\"path\":\"/flows/99\"}]", JSON_PATCH_TYPE));

            var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
            Assertions.assertThat(error.getMessage()).containsIgnoringCase("path");
        }
    }
}
