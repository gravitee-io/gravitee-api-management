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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.WorkflowQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiPatchDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiPatchNotAllowedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.domain_service.api.ApiJsonPatchServiceImpl;
import io.gravitee.apim.infra.domain_service.api.JsonMergePatchServiceImpl;
import io.gravitee.apim.infra.json.jackson.JsonMapperFactory;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PatchApiUseCaseTest {

    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    static final ObjectMapper OBJECT_MAPPER = JsonMapperFactory.build();

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    WorkflowQueryServiceInMemory workflowQueryService = new WorkflowQueryServiceInMemory();
    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService = mock(ApiPrimaryOwnerDomainService.class);
    UpdateApiDomainService updateApiDomainService = mock(UpdateApiDomainService.class);

    PatchApiUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new PatchApiUseCase(
            apiCrudService,
            apiPrimaryOwnerDomainService,
            updateApiDomainService,
            new ApiPatchDomainService(new JsonMergePatchServiceImpl(), new ApiJsonPatchServiceImpl()),
            workflowQueryService,
            OBJECT_MAPPER
        );

        var primaryOwner = PrimaryOwnerEntity.builder().id(USER_ID).type(PrimaryOwnerEntity.Type.USER).build();
        when(apiPrimaryOwnerDomainService.getApiPrimaryOwner(any(), eq(API_ID))).thenReturn(primaryOwner);

        givenExistingApi(ApiFixtures.aProxyApiV4());
        stubUpdateV4ReturnsArgument();
    }

    void givenExistingApi(Api api) {
        apiCrudService.initWith(List.of(api));
    }

    void stubUpdateV4ReturnsArgument() {
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    void stubValidateV4ReturnsArgument() {
        when(updateApiDomainService.validateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    PatchApiUseCase.Output execute(PatchApiUseCase.PatchType type, String body, boolean dryRun) {
        return cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(type)
                .patchBody(body)
                .dryRun(dryRun)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );
    }

    static String mergePatch(String key, Object value) {
        var node = OBJECT_MAPPER.createObjectNode();
        if (value == null) {
            node.putNull(key);
        } else {
            node.set(key, OBJECT_MAPPER.valueToTree(value));
        }
        return node.toString();
    }

    static String patch(String op, String path) {
        return patchNodes(patchOp(op, path));
    }

    static String patch(String op, String path, Object value) {
        return patchNodes(patchOp(op, path, value));
    }

    static String copyPatch(String from, String path) {
        return patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "copy").put("from", from).put("path", path));
    }

    static String movePatch(String from, String path) {
        return patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "move").put("from", from).put("path", path));
    }

    static String patchNodes(ObjectNode... nodes) {
        var arr = OBJECT_MAPPER.createArrayNode();
        for (var n : nodes) {
            arr.add(n);
        }
        return arr.toString();
    }

    static ObjectNode patchOp(String op, String path) {
        return OBJECT_MAPPER.createObjectNode().put("op", op).put("path", path);
    }

    static ObjectNode patchOp(String op, String path, Object value) {
        var node = patchOp(op, path);
        if (value == null) {
            node.putNull("value");
        } else {
            node.set("value", OBJECT_MAPPER.copy().setSerializationInclusion(JsonInclude.Include.ALWAYS).valueToTree(value));
        }
        return node;
    }

    static String setField(PatchApiUseCase.PatchType type, String field, Object value) {
        return switch (type) {
            case MERGE_PATCH -> mergePatch(field, value);
            case JSON_PATCH -> patch("replace", "/" + field, value);
        };
    }

    static Stream<Arguments> clearFieldVariants(String field) {
        return Stream.of(
            Arguments.of(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch(field, null)),
            Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/" + field, null)),
            Arguments.of(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/" + field))
        );
    }

    static Stream<Arguments> clearAnalyticsVariants() {
        return clearFieldVariants("analytics");
    }

    static Stream<Arguments> clearFailoverVariants() {
        return clearFieldVariants("failover");
    }

    static Stream<Arguments> clearFlowExecutionVariants() {
        return clearFieldVariants("flowExecution");
    }

    static Stream<Arguments> clearServicesVariants() {
        return clearFieldVariants("services");
    }

    static Stream<Arguments> clearResponseTemplatesVariants() {
        return clearFieldVariants("responseTemplates");
    }

    static Stream<Arguments> clearPropertiesVariants() {
        return clearFieldVariants("properties");
    }

    static Stream<Arguments> clearLabelsVariants() {
        return clearFieldVariants("labels");
    }

    static Stream<Arguments> clearCategoriesVariants() {
        return clearFieldVariants("categories");
    }

    static Stream<Arguments> clearTagsVariants() {
        return clearFieldVariants("tags");
    }

    static Stream<Arguments> clearDescriptionVariants() {
        return clearFieldVariants("description");
    }

    static Api apiWithServices(ApiServices services) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().services(services).build()).build();
    }

    static Api apiWithResponseTemplates(Map<String, Map<String, ResponseTemplate>> templates) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().responseTemplates(templates).build()).build();
    }

    static Api apiWithProperties(List<Property> properties) {
        var base = ApiFixtures.aProxyApiV4();
        return base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().properties(properties).build()).build();
    }

    static void assertNonPatchedFieldsPreserved(Api api, Api existing, String expectedName) {
        var httpV4 = api.getApiDefinitionHttpV4();
        var originalHttpV4 = existing.getApiDefinitionHttpV4();

        assertThat(api.getName()).isEqualTo(expectedName);
        assertThat(api.getDescription()).isEqualTo(existing.getDescription());
        assertThat(api.getVersion()).isEqualTo(existing.getVersion());
        assertThat(api.getVisibility()).isEqualTo(existing.getVisibility());
        assertThat(api.getApiLifecycleState()).isEqualTo(existing.getApiLifecycleState());
        assertThat(api.getLabels()).isEqualTo(existing.getLabels());
        assertThat(api.getCategories()).isEqualTo(existing.getCategories());
        assertThat(api.getGroups()).isEqualTo(existing.getGroups());
        assertThat(api.isDisableMembershipNotifications()).isEqualTo(existing.isDisableMembershipNotifications());
        assertThat(api.isAllowMultiJwtOauth2Subscriptions()).isEqualTo(existing.isAllowMultiJwtOauth2Subscriptions());
        assertThat(httpV4.getTags()).isEqualTo(originalHttpV4.getTags());
        assertThat(httpV4.getAnalytics()).isEqualTo(originalHttpV4.getAnalytics());
        assertThat(httpV4.getFailover()).isEqualTo(originalHttpV4.getFailover());
        assertThat(httpV4.getFlowExecution()).isEqualTo(originalHttpV4.getFlowExecution());
        assertThat(httpV4.getServices()).isEqualTo(originalHttpV4.getServices());
        assertThat(httpV4.getAllowedInApiProducts()).isEqualTo(originalHttpV4.getAllowedInApiProducts());
        assertThat(httpV4.getProperties()).isEqualTo(originalHttpV4.getProperties());
        assertThat(httpV4.getResponseTemplates()).isEqualTo(originalHttpV4.getResponseTemplates());
    }

    @Nested
    class ApiScopeValidation {

        @Test
        void non_v4_api_is_rejected() {
            givenExistingApi(ApiFixtures.aProxyApiV2());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false)).isInstanceOf(
                ApiInvalidDefinitionVersionException.class
            );
        }

        @Test
        void v4_message_api_is_rejected() {
            givenExistingApi(ApiFixtures.aMessageApiV4());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false)).isInstanceOf(
                ApiInvalidTypeException.class
            );
        }

        @Test
        void federated_api_is_rejected() {
            givenExistingApi(ApiFixtures.aFederatedApi());

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false)).isInstanceOf(
                ApiInvalidDefinitionVersionException.class
            );
        }
    }

    @Nested
    class UseCaseBehavior {

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void updates_allowed_field_and_persists(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "name", "patched-name"), false);

            assertThat(output.api().getName()).isEqualTo("patched-name");
            verify(updateApiDomainService).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_returns_result_without_persistence(PatchApiUseCase.PatchType type) {
            stubValidateV4ReturnsArgument();

            var output = execute(type, setField(type, "name", "dry-run-name"), true);

            assertThat(output.api().getName()).isEqualTo("dry-run-name");
            verify(updateApiDomainService).validateV4(any(), any());
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_invokes_validation_with_patched_api(PatchApiUseCase.PatchType type) {
            stubValidateV4ReturnsArgument();

            execute(type, setField(type, "description", "dry-run-desc"), true);

            var apiCaptor = ArgumentCaptor.forClass(Api.class);
            verify(updateApiDomainService).validateV4(apiCaptor.capture(), any());
            assertThat(apiCaptor.getValue().getDescription()).isEqualTo("dry-run-desc");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void dry_run_propagates_validation_failure(PatchApiUseCase.PatchType type) {
            doThrow(new ValidationDomainException("tag not allowed")).when(updateApiDomainService).validateV4(any(), any());

            assertThatThrownBy(() -> execute(type, setField(type, "tags", List.of("forbidden")), true))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("tag not allowed");
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @Test
        void update_domain_service_is_called_when_not_dry_run() {
            execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("name", "new-name"), false);

            verify(updateApiDomainService).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void update_domain_service_is_not_called_when_dry_run(PatchApiUseCase.PatchType type) {
            execute(type, setField(type, "name", "new-name"), true);

            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void primary_owner_is_included_in_output(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "name", "new-name"), false);

            assertThat(output.primaryOwner()).isNotNull();
            assertThat(output.primaryOwner().id()).isEqualTo(USER_ID);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void audit_info_is_forwarded_to_update(PatchApiUseCase.PatchType type) {
            var auditInfoCaptor = ArgumentCaptor.forClass(AuditInfo.class);

            execute(type, setField(type, "name", "patched-name"), false);

            verify(updateApiDomainService).updateV4(any(), auditInfoCaptor.capture());
            var auditInfo = auditInfoCaptor.getValue();
            assertThat(auditInfo.organizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(auditInfo.environmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(auditInfo.actor().userId()).isEqualTo(USER_ID);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void validation_failure_after_patch_surfaces_as_400(PatchApiUseCase.PatchType type) {
            doThrow(new ValidationDomainException("name must not be blank", Map.of("name", "must not be blank")))
                .when(updateApiDomainService)
                .updateV4(any(), any());

            assertThatThrownBy(() -> execute(type, setField(type, "name", ""), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("name");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void preserves_every_non_patched_field(PatchApiUseCase.PatchType type) {
            var existing = apiCrudService.storage().get(0);

            var output = execute(type, setField(type, "name", "only-name-changed"), false);

            assertNonPatchedFieldsPreserved(output.api(), existing, "only-name-changed");
        }
    }

    @Nested
    class BlockedFields {

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void state_field_is_rejected_with_hint(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "state", "STARTED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state")
                .hasMessageContaining("_start");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void listeners_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "listeners", List.of()), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("listeners");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void endpointGroups_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "endpointGroups", List.of()), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("endpointGroups");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void flows_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "flows", List.of()), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("flows");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void resources_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "resources", List.of()), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("resources");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void definitionVersion_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "definitionVersion", "V4"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("definitionVersion");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void type_field_is_rejected(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "type", "MESSAGE"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("type");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void groups_field_is_rejected(PatchApiUseCase.PatchType type) {
            givenExistingApi(ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(List.of("g-1"))).build());

            assertThatThrownBy(() -> execute(type, setField(type, "groups", List.of("g-2")), false)).isInstanceOf(
                ValidationDomainException.class
            );
        }
    }

    @Nested
    class AllowedFields {

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void name_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "name", "new-name"), false);

            assertThat(output.api().getName()).isEqualTo("new-name");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearDescriptionVariants")
        void description_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getDescription()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void visibility_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "visibility", "PRIVATE"), false);

            assertThat(output.api().getVisibility()).isEqualTo(Api.Visibility.PRIVATE);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void invalid_visibility_value_produces_400(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "visibility", "NOT_A_VISIBILITY"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("visibility")
                .hasMessageContaining("NOT_A_VISIBILITY");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void lifecycleState_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "lifecycleState", "DEPRECATED"), false);

            assertThat(output.api().getApiLifecycleState()).isEqualTo(Api.ApiLifecycleState.DEPRECATED);
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void invalid_lifecycleState_value_produces_400(PatchApiUseCase.PatchType type) {
            assertThatThrownBy(() -> execute(type, setField(type, "lifecycleState", "NOT_A_LIFECYCLE_STATE"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("lifecycleState")
                .hasMessageContaining("NOT_A_LIFECYCLE_STATE");
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void allowMultiJwtOauth2Subscriptions_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "allowMultiJwtOauth2Subscriptions", true), false);

            assertThat(output.api().isAllowMultiJwtOauth2Subscriptions()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void disableMembershipNotifications_field_is_updated(PatchApiUseCase.PatchType type) {
            var output = execute(type, setField(type, "disableMembershipNotifications", false), false);

            assertThat(output.api().isDisableMembershipNotifications()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearLabelsVariants")
        void labels_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getLabels()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearCategoriesVariants")
        void categories_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getCategories()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearTagsVariants")
        void tags_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getTags()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearAnalyticsVariants")
        void analytics_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getAnalytics()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void analytics_sampling_sets_message_sampling(PatchApiUseCase.PatchType type) {
            var base = ApiFixtures.aProxyApiV4();
            givenExistingApi(
                base
                    .toBuilder()
                    .apiDefinitionValue(
                        base.getApiDefinitionHttpV4().toBuilder().analytics(Analytics.builder().enabled(true).build()).build()
                    )
                    .build()
            );
            var body = switch (type) {
                case MERGE_PATCH -> mergePatch("analytics", Map.of("sampling", Map.of("type", "count", "value", "50")));
                case JSON_PATCH -> patch("add", "/analytics/sampling", Map.of("type", "count", "value", "50"));
            };

            var output = execute(type, body, false);

            var messageSampling = output.api().getApiDefinitionHttpV4().getAnalytics().getMessageSampling();
            assertThat(messageSampling).isNotNull();
            assertThat(messageSampling.getType()).isEqualTo(SamplingType.COUNT);
            assertThat(messageSampling.getValue()).isEqualTo("50");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearFailoverVariants")
        void failover_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getFailover()).isNull();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearFlowExecutionVariants")
        void flowExecution_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getFlowExecution()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void flowExecution_mode_is_updated(PatchApiUseCase.PatchType type) {
            var body = switch (type) {
                case MERGE_PATCH -> mergePatch("flowExecution", Map.of("mode", "BEST_MATCH"));
                case JSON_PATCH -> patch("replace", "/flowExecution/mode", "BEST_MATCH");
            };

            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getFlowExecution().getMode()).isEqualTo(FlowMode.BEST_MATCH);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearServicesVariants")
        void services_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithServices(new ApiServices()));

            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getServices()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PatchApiUseCase.PatchType.class)
        void responseTemplates_map_is_replaced(PatchApiUseCase.PatchType type) {
            var value = Map.of("DEFAULT", Map.of("application/json", Map.of("statusCode", 400, "body", "patched")));

            var output = execute(type, setField(type, "responseTemplates", value), false);

            var templates = output.api().getApiDefinitionHttpV4().getResponseTemplates();
            assertThat(templates).containsKey("DEFAULT");
            assertThat(templates.get("DEFAULT").get("application/json").getStatusCode()).isEqualTo(400);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearResponseTemplatesVariants")
        void responseTemplates_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            givenExistingApi(apiWithResponseTemplates(Map.of("KEY", Map.of("application/json", new ResponseTemplate()))));

            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getResponseTemplates()).isNull();
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.api.use_case.PatchApiUseCaseTest#clearPropertiesVariants")
        void properties_field_is_cleared(PatchApiUseCase.PatchType type, String body) {
            var existing = new Property();
            existing.setKey("k1");
            existing.setValue("v1");
            givenExistingApi(apiWithProperties(List.of(existing)));

            var output = execute(type, body, false);

            assertThat(output.api().getApiDefinitionHttpV4().getProperties()).isNull();
        }
    }

    @Nested
    class JsonPatchSpecific {

        @Test
        void malformed_body_produces_validation_exception() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, "not valid json", false)).isInstanceOf(
                ValidationDomainException.class
            );
        }

        @Test
        void operation_without_path_field_is_rejected() {
            var noPathOp = OBJECT_MAPPER.createObjectNode().put("op", "replace").put("value", "x");

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patchNodes(noPathOp), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("path");
        }

        @Test
        void path_without_leading_slash_produces_validation_exception() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "name", "test"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("/");
        }

        @Test
        void root_pointer_path_produces_clear_validation_error() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "", "test"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not target a specific field");
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/", "test"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not target a specific field");
        }

        @Test
        void more_than_200_operations_is_rejected() {
            var ops = new ObjectNode[201];
            for (int i = 0; i < 201; i++) {
                ops[i] = patchOp("replace", "/name", "name-" + i);
            }

            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patchNodes(ops), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("exceeds maximum");
        }

        @Test
        void move_op_without_from_field_is_rejected() {
            assertThatThrownBy(() ->
                execute(
                    PatchApiUseCase.PatchType.JSON_PATCH,
                    patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "move").put("path", "/name")),
                    false
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("missing");
        }

        @Test
        void copy_op_without_from_field_is_rejected() {
            assertThatThrownBy(() ->
                execute(
                    PatchApiUseCase.PatchType.JSON_PATCH,
                    patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "copy").put("path", "/name")),
                    false
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("missing");
        }

        @Test
        void move_op_with_non_string_from_field_is_rejected() {
            assertThatThrownBy(() ->
                execute(
                    PatchApiUseCase.PatchType.JSON_PATCH,
                    patchNodes(OBJECT_MAPPER.createObjectNode().put("op", "move").put("path", "/name").put("from", 123)),
                    false
                )
            )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must be a string");
        }

        @Test
        void move_op_with_disallowed_from_field_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, movePatch("/listeners/0", "/name"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("listeners");
        }

        @Test
        void copy_op_with_disallowed_from_field_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, copyPatch("/listeners/0", "/name"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("listeners");
        }

        @Test
        void move_op_with_disallowed_destination_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, movePatch("/name", "/listeners/0"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("listeners");
        }

        @Test
        void copy_op_with_disallowed_destination_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, copyPatch("/name", "/endpointGroups"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("endpointGroups");
        }

        @Test
        void disallowed_field_via_path_segment_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/listeners/0/type", "HTTP"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("listeners");
        }

        @Test
        void test_op_passes_when_assertion_matches() {
            var existing = ApiFixtures.aProxyApiV4();
            givenExistingApi(existing);

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patchNodes(patchOp("test", "/name", existing.getName()), patchOp("replace", "/name", "new-name")),
                false
            );

            assertThat(output.api().getName()).isEqualTo("new-name");
        }

        @Test
        void test_op_throws_when_assertion_fails() {
            assertThatThrownBy(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("test", "/name", "wrong-value"), false)
            ).isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void test_op_on_blocked_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("test", "/state", "STOPPED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state");
        }

        @Test
        void add_op_updates_field_like_replace() {
            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/name", "new-name-via-add"), false);

            assertThat(output.api().getName()).isEqualTo("new-name-via-add");
        }

        @Test
        void copy_op_copies_value_to_target_path() {
            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, copyPatch("/description", "/name"), false);

            assertThat(output.api().getName()).isEqualTo("api-description");
            assertThat(output.api().getDescription()).isEqualTo("api-description");
        }

        @Test
        void remove_op_on_name_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/name"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("name");
        }

        @Test
        void remove_op_on_apiVersion_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/apiVersion"), false))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("apiVersion");
        }

        @Test
        void add_op_on_blocked_path_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/listeners/0", Map.of()), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("listeners");
        }

        @Test
        void remove_op_on_flows_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/flows"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("flows");
        }

        @Test
        void remove_op_on_endpointGroups_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/endpointGroups"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("endpointGroups");
        }

        @Test
        void add_op_on_state_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/state", "STARTED"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("state")
                .hasMessageContaining("_start");
        }

        @Test
        void add_op_on_flows_subpath_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/flows/-", Map.of()), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("flows");
        }

        @Test
        void null_flowExecution_is_preserved_when_patching_other_fields() {
            var base = ApiFixtures.aProxyApiV4();
            givenExistingApi(
                base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().flowExecution(null).build()).build()
            );

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/name", "patched-name"), false);

            assertThat(output.api().getApiDefinitionHttpV4().getFlowExecution()).isNull();
        }

        @Test
        void replace_null_on_null_responseTemplates_passes_null_to_domain_service() {
            givenExistingApi(apiWithResponseTemplates(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/responseTemplates", null), false);

            assertThat(output.api().getApiDefinitionHttpV4().getResponseTemplates()).isNull();
        }

        @Test
        void remove_op_on_null_responseTemplates_sets_responseTemplates_to_null() {
            givenExistingApi(apiWithResponseTemplates(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/responseTemplates"), false);

            assertThat(output.api().getApiDefinitionHttpV4().getResponseTemplates()).isNull();
        }

        @Test
        void add_op_on_null_responseTemplates_sets_them() {
            givenExistingApi(apiWithResponseTemplates(null));
            var value = Map.of("DEFAULT", Map.of("application/json", Map.of("statusCode", 201, "body", "created")));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/responseTemplates", value), false);

            var templates = output.api().getApiDefinitionHttpV4().getResponseTemplates();
            assertThat(templates).containsKey("DEFAULT");
            assertThat(templates.get("DEFAULT").get("application/json").getStatusCode()).isEqualTo(201);
        }

        @Test
        void add_null_inner_map_for_responseTemplates_key_does_not_throw_npe() {
            givenExistingApi(apiWithResponseTemplates(null));
            var value = new HashMap<String, Object>();
            value.put("DEFAULT", null);

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("add", "/responseTemplates", value), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void any_patch_on_api_with_null_inner_responseTemplates_map_in_storage_does_not_throw_npe() {
            var templates = new HashMap<String, Map<String, ResponseTemplate>>();
            templates.put("DEFAULT", null);
            givenExistingApi(apiWithResponseTemplates(templates));

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/name", "patched-name"), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void replace_responseTemplates_with_null_leaf_template_does_not_throw_npe() {
            var innerMap = new HashMap<String, Object>();
            innerMap.put("application/json", null);
            var value = new HashMap<String, Object>();
            value.put("DEFAULT", innerMap);

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/responseTemplates", value), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void any_patch_on_api_with_null_leaf_responseTemplate_in_storage_does_not_throw_npe() {
            var innerMap = new HashMap<String, ResponseTemplate>();
            innerMap.put("application/json", null);
            var templates = new HashMap<String, Map<String, ResponseTemplate>>();
            templates.put("DEFAULT", innerMap);
            givenExistingApi(apiWithResponseTemplates(templates));

            assertThatCode(() ->
                execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/name", "patched-name"), false)
            ).doesNotThrowAnyException();
        }

        @Test
        void replace_null_on_null_services_passes_null_to_domain_service() {
            givenExistingApi(apiWithServices(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/services", null), false);

            assertThat(output.api().getApiDefinitionHttpV4().getServices()).isNull();
        }

        @Test
        void remove_op_on_null_services_sets_services_to_null() {
            givenExistingApi(apiWithServices(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/services"), false);

            assertThat(output.api().getApiDefinitionHttpV4().getServices()).isNull();
        }

        @Test
        void replace_on_null_properties_sets_properties() {
            givenExistingApi(apiWithProperties(null));

            var output = execute(
                PatchApiUseCase.PatchType.JSON_PATCH,
                patch("replace", "/properties", List.of(Map.of("key", "k", "value", "v"))),
                false
            );

            assertThat(output.api().getApiDefinitionHttpV4().getProperties()).hasSize(1);
        }

        @Test
        void replace_null_on_null_properties_is_a_noop() {
            givenExistingApi(apiWithProperties(null));

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("replace", "/properties", null), false);

            assertThat(output.api().getApiDefinitionHttpV4().getProperties()).isNull();
        }

        @Test
        void remove_op_on_null_categories_clears_categories() {
            givenExistingApi(ApiFixtures.aProxyApiV4().toBuilder().categories(null).build());

            var output = execute(PatchApiUseCase.PatchType.JSON_PATCH, patch("remove", "/categories"), false);

            assertThat(output.api().getCategories()).isEmpty();
        }
    }

    @Nested
    class MergePatchSpecific {

        @Test
        void unknown_field_is_rejected() {
            assertThatThrownBy(() -> execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("unknownField", "value"), false))
                .isInstanceOf(ApiPatchNotAllowedException.class)
                .hasMessageContaining("unknownField");
        }

        @Test
        void validation_failure_during_dry_run_still_surfaces() {
            assertThatThrownBy(() ->
                execute(PatchApiUseCase.PatchType.MERGE_PATCH, mergePatch("responseTemplates", 42), true)
            ).isInstanceOf(ValidationDomainException.class);
        }
    }

    @Test
    void patchable_response_template_from_returns_null_when_domain_is_null() {
        assertThat(PatchApiUseCase.PatchableResponseTemplate.from(null)).isNull();
    }
}
