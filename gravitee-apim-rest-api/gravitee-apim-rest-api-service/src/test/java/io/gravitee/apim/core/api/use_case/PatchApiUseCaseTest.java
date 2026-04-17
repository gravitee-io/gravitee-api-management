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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.WorkflowQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiPatchDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiPatchNotAllowedException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.service.ApiServices;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PatchApiUseCaseTest {

    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    WorkflowQueryServiceInMemory workflowQueryService = new WorkflowQueryServiceInMemory();
    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService = mock(ApiPrimaryOwnerDomainService.class);
    UpdateApiDomainService updateApiDomainService = mock(UpdateApiDomainService.class);
    ObjectMapper objectMapper = new ObjectMapper();

    PatchApiUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new PatchApiUseCase(
            apiCrudService,
            apiPrimaryOwnerDomainService,
            updateApiDomainService,
            new ApiPatchDomainService(),
            workflowQueryService,
            objectMapper
        );

        var primaryOwner = PrimaryOwnerEntity.builder().id(USER_ID).type(PrimaryOwnerEntity.Type.USER).build();
        when(apiPrimaryOwnerDomainService.getApiPrimaryOwner(any(), eq(API_ID))).thenReturn(primaryOwner);
    }

    @Test
    void merge_patch_updates_allowed_field_and_persists() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"name\":\"patched-name\"}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getName()).isEqualTo("patched-name");
        verify(updateApiDomainService).updateV4(any(), any());
    }

    @Test
    void json_patch_replace_op_updates_allowed_field_and_persists() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"json-patched-name\"}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getName()).isEqualTo("json-patched-name");
        verify(updateApiDomainService).updateV4(any(), any());
    }

    @Test
    void dry_run_returns_projected_result_without_persistence() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.validateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"name\":\"dry-run-name\"}")
                .dryRun(true)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getName()).isEqualTo("dry-run-name");
        verify(updateApiDomainService).validateV4(any(), any());
        verify(updateApiDomainService, never()).updateV4(any(), any());
    }

    @Test
    void dry_run_invokes_validation_with_patched_api_and_primary_owner() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.validateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"description\":\"dry-run-desc\"}")
                .dryRun(true)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        var apiCaptor = org.mockito.ArgumentCaptor.forClass(io.gravitee.apim.core.api.model.Api.class);
        verify(updateApiDomainService).validateV4(apiCaptor.capture(), any());
        assertThat(apiCaptor.getValue().getDescription()).isEqualTo("dry-run-desc");
    }

    @Test
    void dry_run_propagates_validation_failure() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        doThrow(new ValidationDomainException("tag not allowed")).when(updateApiDomainService).validateV4(any(), any());

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"tags\":[\"forbidden\"]}")
                    .dryRun(true)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("tag not allowed");
        verify(updateApiDomainService, never()).updateV4(any(), any());
    }

    @Test
    void patching_state_field_is_rejected_with_hint() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"state\":\"STARTED\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("state")
            .hasMessageContaining("_start");
    }

    @Test
    void patching_listeners_is_rejected() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"listeners\":[]}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("listeners");
    }

    @Test
    void patching_endpointGroups_is_rejected() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"endpointGroups\":[]}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("endpointGroups");
    }

    @Test
    void patching_flows_is_rejected() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"flows\":[]}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("flows");
    }

    @Test
    void patching_resources_is_rejected() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"resources\":[]}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("resources");
    }

    @Test
    void patching_unknown_field_is_rejected() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"unknownField\":\"value\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("unknownField");
    }

    @Test
    void malformed_json_patch_body_produces_validation_exception() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                    .patchBody("not valid json")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        ).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void non_v4_api_is_rejected_with_scope_error() {
        var v2Api = ApiFixtures.aProxyApiV2();
        apiCrudService.initWith(List.of(v2Api));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"name\":\"new-name\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        ).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void v4_message_api_is_rejected_with_scope_error() {
        var messageApi = ApiFixtures.aMessageApiV4();
        apiCrudService.initWith(List.of(messageApi));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"name\":\"new-name\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        ).isInstanceOf(ApiInvalidTypeException.class);
    }

    @Test
    void federated_api_is_rejected_with_scope_error() {
        var federatedApi = ApiFixtures.aFederatedApi();
        apiCrudService.initWith(List.of(federatedApi));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"name\":\"new-name\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        ).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void validation_failure_after_patch_surfaces_as_400() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        doThrow(new ValidationDomainException("name must not be blank", Map.of("name", "must not be blank")))
            .when(updateApiDomainService)
            .updateV4(any(), any());

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"name\":\"\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("name");
    }

    @Test
    void merge_patch_preserves_every_non_patched_field() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"name\":\"only-name-changed\"}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        var api = output.api();
        var httpV4 = api.getApiDefinitionHttpV4();
        var originalHttpV4 = existing.getApiDefinitionHttpV4();

        assertThat(api.getName()).isEqualTo("only-name-changed");
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

    @Test
    void json_patch_preserves_every_non_patched_field() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"json-only-name-changed\"}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        var api = output.api();
        var httpV4 = api.getApiDefinitionHttpV4();
        var originalHttpV4 = existing.getApiDefinitionHttpV4();

        assertThat(api.getName()).isEqualTo("json-only-name-changed");
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

    @Test
    void update_domain_service_is_called_when_not_dry_run() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"name\":\"new-name\"}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        verify(updateApiDomainService).updateV4(any(), any());
    }

    @Test
    void update_domain_service_is_not_called_when_dry_run() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"name\":\"new-name\"}")
                .dryRun(true)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        verify(updateApiDomainService, never()).updateV4(any(), any());
    }

    @Test
    void json_patch_disallowed_field_via_path_segment_is_rejected() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                    .patchBody("[{\"op\":\"replace\",\"path\":\"/listeners/0/type\",\"value\":\"HTTP\"}]")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ApiPatchNotAllowedException.class)
            .hasMessageContaining("listeners");
    }

    @Test
    void primary_owner_is_included_in_output() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"name\":\"new-name\"}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.primaryOwner()).isNotNull();
        assertThat(output.primaryOwner().id()).isEqualTo(USER_ID);
    }

    @Test
    void validation_failure_during_dry_run_still_surfaces() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"responseTemplates\":42}")
                    .dryRun(true)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        ).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void merge_patch_null_clears_groups() {
        var existing = ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(List.of("g-1"))).build();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"groups\":null}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getGroups()).isEmpty();
    }

    @Test
    void merge_patch_replaces_response_templates_map() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var body = "{\"responseTemplates\":{\"FOO_KEY\":{\"application/json\":{\"status\":400,\"body\":\"{}\"}}}}";
        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody(body)
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        var templates = output.api().getApiDefinitionHttpV4().getResponseTemplates();
        assertThat(templates).containsKey("FOO_KEY");
        assertThat(templates.get("FOO_KEY")).containsKey("application/json");
    }

    @Test
    void merge_patch_updates_visibility() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"visibility\":\"PRIVATE\"}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getVisibility()).isEqualTo(io.gravitee.apim.core.api.model.Api.Visibility.PRIVATE);
    }

    @Test
    void merge_patch_updates_lifecycle_state() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"lifecycleState\":\"DEPRECATED\"}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiLifecycleState()).isEqualTo(io.gravitee.apim.core.api.model.Api.ApiLifecycleState.DEPRECATED);
    }

    @Test
    void merge_patch_invalid_visibility_value_produces_400() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"visibility\":\"NOT_A_VISIBILITY\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("visibility")
            .hasMessageContaining("NOT_A_VISIBILITY");
    }

    @Test
    void merge_patch_invalid_lifecycle_state_value_produces_400() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));

        assertThatThrownBy(() ->
            cut.execute(
                PatchApiUseCase.Input.builder()
                    .apiId(API_ID)
                    .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                    .patchBody("{\"lifecycleState\":\"NOT_A_LIFECYCLE_STATE\"}")
                    .dryRun(false)
                    .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                    .build()
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("lifecycleState")
            .hasMessageContaining("NOT_A_LIFECYCLE_STATE");
    }

    // -------------------------------------------------------------------------
    // Null-behaviour characterisation tests for object fields
    //
    // These tests document what PATCH passes to the domain service when each
    // field is set to null.  The mock returns the argument unchanged, so the
    // assertion captures the value produced by applyPatchedValues() before any
    // downstream validation (e.g. AnalyticsValidationServiceImpl) runs.
    // They serve as a baseline for aligning PATCH behaviour with PUT.
    // -------------------------------------------------------------------------

    @Test
    void merge_patch_null_on_analytics_passes_null_to_domain_service() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"analytics\":null}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getAnalytics()).isNull();
    }

    @Test
    void merge_patch_null_on_failover_passes_null_to_domain_service() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"failover\":null}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getFailover()).isNull();
    }

    @Test
    void merge_patch_null_on_flowExecution_passes_null_to_domain_service() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"flowExecution\":null}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getFlowExecution()).isNull();
    }

    @Test
    void merge_patch_null_on_services_passes_null_to_domain_service() {
        var services = new ApiServices();
        var existing = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .apiDefinitionValue(ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4().toBuilder().services(services).build())
            .build();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"services\":null}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getServices()).isNull();
    }

    @Test
    void merge_patch_null_on_responseTemplates_passes_null_to_domain_service() {
        var templates = Map.of("KEY", Map.of("application/json", new ResponseTemplate()));
        var existing = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .apiDefinitionValue(ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4().toBuilder().responseTemplates(templates).build())
            .build();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.MERGE_PATCH)
                .patchBody("{\"responseTemplates\":null}")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getResponseTemplates()).isNull();
    }

    @Test
    void json_patch_replace_null_on_analytics_passes_null_to_domain_service() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/analytics\",\"value\":null}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getAnalytics()).isNull();
    }

    @Test
    void json_patch_replace_null_on_failover_passes_null_to_domain_service() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/failover\",\"value\":null}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getFailover()).isNull();
    }

    @Test
    void json_patch_replace_null_on_flowExecution_passes_null_to_domain_service() {
        var existing = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/flowExecution\",\"value\":null}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getFlowExecution()).isNull();
    }

    @Test
    void json_patch_replace_null_on_responseTemplates_passes_null_to_domain_service() {
        var templates = Map.of("KEY", Map.of("application/json", new ResponseTemplate()));
        var existing = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .apiDefinitionValue(ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4().toBuilder().responseTemplates(templates).build())
            .build();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/responseTemplates\",\"value\":null}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getResponseTemplates()).isNull();
    }

    @Test
    void json_patch_replace_null_on_services_passes_null_to_domain_service() {
        var services = new ApiServices();
        var existing = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .apiDefinitionValue(ApiFixtures.aProxyApiV4().getApiDefinitionHttpV4().toBuilder().services(services).build())
            .build();
        apiCrudService.initWith(List.of(existing));
        when(updateApiDomainService.updateV4(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        var output = cut.execute(
            PatchApiUseCase.Input.builder()
                .apiId(API_ID)
                .patchType(PatchApiUseCase.PatchType.JSON_PATCH)
                .patchBody("[{\"op\":\"replace\",\"path\":\"/services\",\"value\":null}]")
                .dryRun(false)
                .auditInfo(AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID))
                .build()
        );

        assertThat(output.api().getApiDefinitionHttpV4().getServices()).isNull();
    }
}
