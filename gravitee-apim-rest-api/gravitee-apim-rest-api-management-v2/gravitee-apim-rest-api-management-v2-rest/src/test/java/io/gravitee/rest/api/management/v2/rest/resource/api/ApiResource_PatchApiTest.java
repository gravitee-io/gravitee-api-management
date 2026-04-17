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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.UNSUPPORTED_MEDIA_TYPE_415;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.management.v2.rest.model.ApiServices;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.FlowMode;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiTest extends ApiResourceTest {

    private static final String MERGE_PATCH_TYPE = "application/merge-patch+json";
    private static final String JSON_PATCH_TYPE = "application/json-patch+json";

    @Inject
    UpdateApiDomainService updateApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void setUpApiAndPrimaryOwner() {
        var existingApi = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(List.of(existingApi));

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
        reset(updateApiDomainService);
    }

    @Test
    void should_return_200_when_merge_patch_updates_name() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"patched-via-merge\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("patched-via-merge");
    }

    @Test
    void should_return_etag_and_last_modified_headers_on_successful_patch() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"hdr\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        org.assertj.core.api.Assertions.assertThat(response.getHeaderString("ETag")).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(response.getHeaderString("Last-Modified")).isNotBlank();
    }

    @Test
    void should_return_415_when_content_type_is_unsupported() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("name=plain", MediaType.TEXT_PLAIN_TYPE));

        assertThat(response).hasStatus(UNSUPPORTED_MEDIA_TYPE_415);
    }

    @Test
    void should_return_200_when_json_patch_updates_name() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"json-patched\"}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("json-patched");
    }

    @Test
    void should_return_200_with_projected_result_when_dry_run() {
        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"name\":\"dry-run-result\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("dry-run-result");
        org.mockito.Mockito.verify(updateApiDomainService).validateV4(any(), any());
        org.mockito.Mockito.verify(updateApiDomainService, org.mockito.Mockito.never()).updateV4(any(), any());
    }

    @Test
    void should_return_200_with_sanitized_tags_when_dry_run() {
        doAnswer(inv -> {
            io.gravitee.apim.core.api.model.Api api = inv.getArgument(0);
            var sanitizedDef = api.getApiDefinitionHttpV4().toBuilder().tags(java.util.Set.of("allowed")).build();
            return api.toBuilder().apiDefinitionValue(sanitizedDef).build();
        })
            .when(updateApiDomainService)
            .validateV4(any(), any());

        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"tags\":[\"requested\",\"forbidden\"]}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getTags).asList().containsExactly("allowed");
    }

    @Test
    void should_return_200_when_dry_run_with_json_patch() {
        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"dry\"}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDescription).isEqualTo("dry");
        org.mockito.Mockito.verify(updateApiDomainService).validateV4(any(), any());
        org.mockito.Mockito.verify(updateApiDomainService, org.mockito.Mockito.never()).updateV4(any(), any());
    }

    @Test
    void should_return_400_when_dry_run_fails_domain_validation() {
        doThrow(new ValidationDomainException("tag 'forbidden' not allowed")).when(updateApiDomainService).validateV4(any(), any());

        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"tags\":[\"forbidden\"]}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining("forbidden");
        org.mockito.Mockito.verify(updateApiDomainService, org.mockito.Mockito.never()).updateV4(any(), any());
    }

    @Test
    void should_return_400_when_dry_run_rejects_allow_list_field() {
        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"state\":\"STARTED\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining("state");
        org.mockito.Mockito.verify(updateApiDomainService, org.mockito.Mockito.never()).validateV4(any(), any());
        org.mockito.Mockito.verify(updateApiDomainService, org.mockito.Mockito.never()).updateV4(any(), any());
    }

    @Test
    void should_return_400_when_patching_state_field() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"state\":\"STARTED\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining("state");
    }

    @Test
    void should_return_400_when_patching_listeners_field() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"listeners\":[]}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining("listeners");
    }

    @Test
    void should_return_400_when_json_patch_body_is_malformed() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("this is not json", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_patched_api_fails_validation() {
        doThrow(new ValidationDomainException("name must not be blank", Map.of("name", "must not be blank")))
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_403_when_missing_update_permission() {
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

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"new\"}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_return_404_when_api_not_found() {
        apiCrudService.reset();

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"new\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_400_when_patching_v2_api() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"new\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_patching_v4_message_api() {
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4()));

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"new\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_treat_application_json_as_merge_patch() {
        var response = rootTarget(API).request().method("PATCH", Entity.json("{\"name\":\"patched-via-json\"}"));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("patched-via-json");
    }

    @Test
    void should_return_200_when_merge_patch_updates_disable_membership_notifications() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"disableMembershipNotifications\":true}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDisableMembershipNotifications).isEqualTo(true);
    }

    @Test
    void should_return_200_when_merge_patch_updates_groups() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"groups\":[\"g-1\",\"g-2\"]}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getGroups)
            .asList()
            .containsExactlyInAnyOrder("g-1", "g-2");
    }

    @Test
    void should_return_200_when_merge_patch_updates_properties() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"properties\":[{\"key\":\"k1\",\"value\":\"v1\"}]}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_200_when_merge_patch_updates_response_templates() {
        var body = "{\"responseTemplates\":{\"FOO_KEY\":{\"application/json\":{\"status\":400,\"body\":\"{}\"}}}}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_200_when_merge_patch_updates_description() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"description\":\"new-description\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDescription).isEqualTo("new-description");
    }

    @Test
    void should_return_200_when_merge_patch_updates_api_version() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"apiVersion\":\"2.0.0\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getApiVersion).isEqualTo("2.0.0");
    }

    @Test
    void should_return_200_when_merge_patch_updates_visibility() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"visibility\":\"PRIVATE\"}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getVisibility().name())
            .isEqualTo("PRIVATE");
    }

    @Test
    void should_return_200_when_merge_patch_updates_labels() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"labels\":[\"infra\",\"prod\"]}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getLabels)
            .asList()
            .containsExactlyInAnyOrder("infra", "prod");
    }

    @Test
    void should_return_200_when_merge_patch_updates_tags() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"tags\":[\"tag-a\",\"tag-b\"]}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getTags)
            .asList()
            .containsExactlyInAnyOrder("tag-a", "tag-b");
    }

    @Test
    void should_return_200_when_merge_patch_updates_lifecycle_state() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"lifecycleState\":\"DEPRECATED\"}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getLifecycleState().name())
            .isEqualTo("DEPRECATED");
    }

    @Test
    void should_return_200_when_merge_patch_updates_categories() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"categories\":[\"cat-a\",\"cat-b\"]}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getCategories)
            .asList()
            .containsExactlyInAnyOrder("cat-a", "cat-b");
    }

    @Test
    void should_return_200_when_merge_patch_updates_analytics() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"analytics\":{\"enabled\":true}}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getAnalytics().getEnabled())
            .isEqualTo(true);
    }

    @Test
    void should_return_200_when_merge_patch_updates_failover() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"failover\":{\"enabled\":true}}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getFailover().getEnabled())
            .isEqualTo(true);
    }

    @Test
    void should_return_200_when_merge_patch_updates_flow_execution() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"flowExecution\":{\"mode\":\"best-match\",\"matchRequired\":true}}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getFlowExecution)
            .isEqualTo(new io.gravitee.rest.api.management.v2.rest.model.FlowExecution().mode(FlowMode.BEST_MATCH).matchRequired(true));
    }

    @Test
    void should_return_200_when_merge_patch_updates_services() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"services\":{}}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getServices).isEqualTo(new ApiServices());
    }

    @Test
    void should_return_200_when_merge_patch_updates_allowed_in_api_products() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"allowedInApiProducts\":true}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getAllowedInApiProducts).isEqualTo(true);
    }

    @Test
    void should_return_200_when_merge_patch_updates_allow_multi_jwt_oauth2_subscriptions() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"allowMultiJwtOauth2Subscriptions\":true}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getAllowMultiJwtOauth2Subscriptions).isEqualTo(true);
    }

    @Test
    void should_include_primary_owner_in_response() {
        primaryOwnerDomainService.initWith(
            List.of(
                Map.entry(
                    API,
                    PrimaryOwnerEntity.builder()
                        .id(USER_NAME)
                        .email("jane.doe@gravitee.io")
                        .displayName("Jane Doe")
                        .type(PrimaryOwnerEntity.Type.USER)
                        .build()
                )
            )
        );

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"with-owner\"}", MERGE_PATCH_TYPE));

        var primaryOwner = assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getPrimaryOwner)
            .isNotNull()
            .actual();
        org.assertj.core.api.Assertions.assertThat(primaryOwner.getId()).isEqualTo(USER_NAME);
        org.assertj.core.api.Assertions.assertThat(primaryOwner.getEmail()).isEqualTo("jane.doe@gravitee.io");
        org.assertj.core.api.Assertions.assertThat(primaryOwner.getDisplayName()).isEqualTo("Jane Doe");
        org.assertj.core.api.Assertions.assertThat(primaryOwner.getType()).isEqualTo(
            io.gravitee.rest.api.management.v2.rest.model.MembershipMemberType.USER
        );
    }

    @Test
    void should_return_200_when_json_patch_replaces_a_complex_field() {
        var body = "[{\"op\":\"replace\",\"path\":\"/analytics\",\"value\":{\"enabled\":true}}]";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, JSON_PATCH_TYPE));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getAnalytics().getEnabled())
            .isEqualTo(true);
    }

    @Test
    void should_return_400_when_merge_patch_visibility_is_invalid() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"visibility\":\"NOT_A_VISIBILITY\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_merge_patch_lifecycle_state_is_invalid() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"lifecycleState\":\"NOT_A_LIFECYCLE_STATE\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void patch_merge_null_on_name_returns_400() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":null}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("name").hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_name_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":null}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("name").hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_description_returns_400() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"description\":null}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("description").hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_description_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":null}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("description").hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_apiVersion_returns_400() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"apiVersion\":null}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("apiVersion").hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_apiVersion_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/apiVersion\",\"value\":null}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("apiVersion").hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_visibility_returns_400() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"visibility\":null}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("visibility").hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_visibility_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/visibility\",\"value\":null}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("visibility").hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_lifecycleState_returns_400() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"lifecycleState\":null}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("lifecycleState").hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_lifecycleState_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"replace\",\"path\":\"/lifecycleState\",\"value\":null}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("lifecycleState").hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_allowedInApiProducts_returns_400() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"allowedInApiProducts\":null}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("allowedInApiProducts").hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_allowedInApiProducts_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("[{\"op\":\"add\",\"path\":\"/allowedInApiProducts\",\"value\":null}]", JSON_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining("allowedInApiProducts").hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_allowMultiJwtOauth2Subscriptions_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"allowMultiJwtOauth2Subscriptions\":null}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasMessageContaining("allowMultiJwtOauth2Subscriptions")
            .hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_allowMultiJwtOauth2Subscriptions_returns_400() {
        var response = rootTarget(API)
            .request()
            .method(
                "PATCH",
                Entity.entity("[{\"op\":\"replace\",\"path\":\"/allowMultiJwtOauth2Subscriptions\",\"value\":null}]", JSON_PATCH_TYPE)
            );

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasMessageContaining("allowMultiJwtOauth2Subscriptions")
            .hasMessageContaining("null");
    }

    @Test
    void patch_merge_null_on_disableMembershipNotifications_returns_400() {
        var response = rootTarget(API)
            .request()
            .method("PATCH", Entity.entity("{\"disableMembershipNotifications\":null}", MERGE_PATCH_TYPE));

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasMessageContaining("disableMembershipNotifications")
            .hasMessageContaining("null");
    }

    @Test
    void patch_json_null_on_disableMembershipNotifications_returns_400() {
        var response = rootTarget(API)
            .request()
            .method(
                "PATCH",
                Entity.entity("[{\"op\":\"replace\",\"path\":\"/disableMembershipNotifications\",\"value\":null}]", JSON_PATCH_TYPE)
            );

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasMessageContaining("disableMembershipNotifications")
            .hasMessageContaining("null");
    }

    @Test
    void patch_merge_omitting_name_returns_200_with_name_unchanged() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"description\":\"changed\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("My Api");
    }

    @Test
    void patch_merge_valid_name_returns_200_with_updated_name() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"updated-name\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("updated-name");
    }

    @Test
    void should_render_null_allowed_in_api_products_as_false_in_patch_response() {
        var storedApi = ApiFixtures.aProxyApiV4();
        org.assertj.core.api.Assertions.assertThat(storedApi.getApiDefinitionHttpV4().getAllowedInApiProducts())
            .as("precondition: fixture has null allowedInApiProducts")
            .isNull();
        apiCrudService.initWith(List.of(storedApi));

        var capturedApi = new io.gravitee.apim.core.api.model.Api[] { null };
        doAnswer(inv -> {
            capturedApi[0] = inv.getArgument(0);
            return capturedApi[0];
        })
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"description\":\"changed\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getAllowedInApiProducts).isEqualTo(false);
        org.assertj.core.api.Assertions.assertThat(capturedApi[0].getApiDefinitionHttpV4().getAllowedInApiProducts())
            .as("stored value must not be coalesced before persistence")
            .isNull();
    }
}
