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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.management.v2.rest.model.ApiServices;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.FlowExecution;
import io.gravitee.rest.api.management.v2.rest.model.FlowMode;
import io.gravitee.rest.api.management.v2.rest.model.MembershipMemberType;
import io.gravitee.rest.api.management.v2.rest.model.Property;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // ---- General (format-independent) tests ----

    @Test
    void should_return_415_when_content_type_is_unsupported() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("name=plain", MediaType.TEXT_PLAIN_TYPE));

        assertThat(response).hasStatus(UNSUPPORTED_MEDIA_TYPE_415);
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
    void should_return_etag_and_last_modified_headers_on_successful_patch() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"hdr\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        Assertions.assertThat(response.getHeaderString("ETag")).isNotBlank();
        Assertions.assertThat(response.getHeaderString("Last-Modified")).isNotBlank();
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
        Assertions.assertThat(primaryOwner.getId()).isEqualTo(USER_NAME);
        Assertions.assertThat(primaryOwner.getEmail()).isEqualTo("jane.doe@gravitee.io");
        Assertions.assertThat(primaryOwner.getDisplayName()).isEqualTo("Jane Doe");
        Assertions.assertThat(primaryOwner.getType()).isEqualTo(MembershipMemberType.USER);
    }

    @Test
    void should_render_null_allowed_in_api_products_as_false_in_patch_response() {
        var storedApi = ApiFixtures.aProxyApiV4();
        Assertions.assertThat(storedApi.getApiDefinitionHttpV4().getAllowedInApiProducts())
            .as("precondition: fixture has null allowedInApiProducts")
            .isNull();
        apiCrudService.initWith(List.of(storedApi));

        var capturedApi = new Api[] { null };
        doAnswer(inv -> {
            capturedApi[0] = inv.getArgument(0);
            return capturedApi[0];
        })
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"description\":\"changed\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getAllowedInApiProducts).isEqualTo(false);
        Assertions.assertThat(capturedApi[0].getApiDefinitionHttpV4().getAllowedInApiProducts())
            .as("stored value must not be coalesced before persistence")
            .isNull();
    }

    @Test
    void patch_merge_omitting_name_returns_200_with_name_unchanged() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"description\":\"changed\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("My Api");
    }

    // ---- Format-specific validation ----

    @Nested
    class FormatSpecificValidation {

        @Test
        void should_treat_application_json_as_merge_patch() {
            var response = rootTarget(API).request().method("PATCH", Entity.json("{\"name\":\"patched-via-json\"}"));

            assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("patched-via-json");
        }

        @Test
        void should_return_400_when_json_patch_body_is_malformed() {
            var response = rootTarget(API).request().method("PATCH", Entity.entity("this is not json", JSON_PATCH_TYPE));

            assertThat(response).hasStatus(BAD_REQUEST_400);
        }
    }

    // ---- Validation common to both formats ----

    @ParameterizedTest
    @MethodSource("disallowedFieldCases")
    void should_return_400_when_patching_disallowed_field(String body, String contentType, String fieldName) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining(fieldName);
    }

    static Stream<Arguments> disallowedFieldCases() {
        return Stream.of(
            Arguments.of("{\"state\":\"STARTED\"}", MERGE_PATCH_TYPE, "state"),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/state\",\"value\":\"STARTED\"}]", JSON_PATCH_TYPE, "state"),
            Arguments.of("{\"listeners\":[]}", MERGE_PATCH_TYPE, "listeners"),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/listeners\",\"value\":[]}]", JSON_PATCH_TYPE, "listeners")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidEnumValueCases")
    void should_return_400_when_enum_field_has_invalid_value(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    static Stream<Arguments> invalidEnumValueCases() {
        return Stream.of(
            Arguments.of("{\"visibility\":\"NOT_A_VISIBILITY\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/visibility\",\"value\":\"NOT_A_VISIBILITY\"}]", JSON_PATCH_TYPE),
            Arguments.of("{\"lifecycleState\":\"NOT_A_LIFECYCLE_STATE\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/lifecycleState\",\"value\":\"NOT_A_LIFECYCLE_STATE\"}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("domainValidationFailureCases")
    void should_return_400_when_domain_validation_fails(String body, String contentType) {
        doThrow(new ValidationDomainException("name must not be blank", Map.of("name", "must not be blank")))
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
    }

    static Stream<Arguments> domainValidationFailureCases() {
        return Stream.of(
            Arguments.of("{\"name\":\"\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"\"}]", JSON_PATCH_TYPE)
        );
    }

    // ---- Dry-run ----

    @Nested
    class DryRun {

        @ParameterizedTest
        @MethodSource("withoutPersistingCases")
        void should_apply_patch_without_persisting(String body, String contentType, String expectedDescription) {
            var response = rootTarget(API).queryParam("dryRun", "true").request().method("PATCH", Entity.entity(body, contentType));

            assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDescription).isEqualTo(expectedDescription);
            verify(updateApiDomainService).validateV4(any(), any());
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        static Stream<Arguments> withoutPersistingCases() {
            return Stream.of(
                Arguments.of("{\"description\":\"dry\"}", MERGE_PATCH_TYPE, "dry"),
                Arguments.of("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"dry\"}]", JSON_PATCH_TYPE, "dry")
            );
        }

        @Test
        void should_return_200_with_sanitized_tags() {
            doAnswer(inv -> {
                Api api = inv.getArgument(0);
                var sanitizedDef = api.getApiDefinitionHttpV4().toBuilder().tags(Set.of("allowed")).build();
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

        @ParameterizedTest
        @MethodSource("allowListRejectionCases")
        void should_return_400_when_allow_list_field_rejected(String body, String contentType) {
            var response = rootTarget(API).queryParam("dryRun", "true").request().method("PATCH", Entity.entity(body, contentType));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining("state");
            verify(updateApiDomainService, never()).validateV4(any(), any());
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        static Stream<Arguments> allowListRejectionCases() {
            return Stream.of(
                Arguments.of("{\"state\":\"STARTED\"}", MERGE_PATCH_TYPE),
                Arguments.of("[{\"op\":\"replace\",\"path\":\"/state\",\"value\":\"STARTED\"}]", JSON_PATCH_TYPE)
            );
        }

        @ParameterizedTest
        @MethodSource("domainValidationFailureCases")
        void should_return_400_when_domain_validation_fails(String body, String contentType) {
            doThrow(new ValidationDomainException("tag 'forbidden' not allowed")).when(updateApiDomainService).validateV4(any(), any());

            var response = rootTarget(API).queryParam("dryRun", "true").request().method("PATCH", Entity.entity(body, contentType));

            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessageContaining("forbidden");
            verify(updateApiDomainService, never()).updateV4(any(), any());
        }

        static Stream<Arguments> domainValidationFailureCases() {
            return Stream.of(
                Arguments.of("{\"tags\":[\"forbidden\"]}", MERGE_PATCH_TYPE),
                Arguments.of("[{\"op\":\"replace\",\"path\":\"/tags\",\"value\":[\"forbidden\"]}]", JSON_PATCH_TYPE)
            );
        }
    }

    // ---- Null field behavior ----

    @ParameterizedTest
    @MethodSource("nullOnRequiredFieldCases")
    void setting_required_field_to_null_returns_400(String body, String contentType, String fieldName) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessageContaining(fieldName).hasMessageContaining("null");
    }

    static Stream<Arguments> nullOnRequiredFieldCases() {
        return Stream.of(
            Arguments.of("{\"name\":null}", MERGE_PATCH_TYPE, "name"),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":null}]", JSON_PATCH_TYPE, "name"),
            Arguments.of("{\"apiVersion\":null}", MERGE_PATCH_TYPE, "apiVersion"),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/apiVersion\",\"value\":null}]", JSON_PATCH_TYPE, "apiVersion"),
            Arguments.of("{\"visibility\":null}", MERGE_PATCH_TYPE, "visibility"),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/visibility\",\"value\":null}]", JSON_PATCH_TYPE, "visibility"),
            Arguments.of("{\"lifecycleState\":null}", MERGE_PATCH_TYPE, "lifecycleState"),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/lifecycleState\",\"value\":null}]", JSON_PATCH_TYPE, "lifecycleState"),
            Arguments.of("{\"allowedInApiProducts\":null}", MERGE_PATCH_TYPE, "allowedInApiProducts"),
            Arguments.of("[{\"op\":\"add\",\"path\":\"/allowedInApiProducts\",\"value\":null}]", JSON_PATCH_TYPE, "allowedInApiProducts"),
            Arguments.of("{\"allowMultiJwtOauth2Subscriptions\":null}", MERGE_PATCH_TYPE, "allowMultiJwtOauth2Subscriptions"),
            Arguments.of(
                "[{\"op\":\"replace\",\"path\":\"/allowMultiJwtOauth2Subscriptions\",\"value\":null}]",
                JSON_PATCH_TYPE,
                "allowMultiJwtOauth2Subscriptions"
            ),
            Arguments.of("{\"disableMembershipNotifications\":null}", MERGE_PATCH_TYPE, "disableMembershipNotifications"),
            Arguments.of(
                "[{\"op\":\"replace\",\"path\":\"/disableMembershipNotifications\",\"value\":null}]",
                JSON_PATCH_TYPE,
                "disableMembershipNotifications"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("nullOnOptionalFieldCases")
    void setting_optional_field_to_null_clears_it(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDescription).isNull();
    }

    static Stream<Arguments> nullOnOptionalFieldCases() {
        return Stream.of(
            Arguments.of("{\"description\":null}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":null}]", JSON_PATCH_TYPE)
        );
    }

    // ---- Field update tests (both formats) ----

    @ParameterizedTest
    @MethodSource("nameUpdateVariants")
    void should_update_name(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getName).isEqualTo("patched");
    }

    static Stream<Arguments> nameUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"name\":\"patched\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"patched\"}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("descriptionUpdateVariants")
    void should_update_description(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDescription).isEqualTo("new-description");
    }

    static Stream<Arguments> descriptionUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"description\":\"new-description\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/description\",\"value\":\"new-description\"}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("apiVersionUpdateVariants")
    void should_update_api_version(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getApiVersion).isEqualTo("2.0.0");
    }

    static Stream<Arguments> apiVersionUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"apiVersion\":\"2.0.0\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/apiVersion\",\"value\":\"2.0.0\"}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("visibilityUpdateVariants")
    void should_update_visibility(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getVisibility().name())
            .isEqualTo("PRIVATE");
    }

    static Stream<Arguments> visibilityUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"visibility\":\"PRIVATE\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/visibility\",\"value\":\"PRIVATE\"}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("labelsUpdateVariants")
    void should_update_labels(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getLabels)
            .asList()
            .containsExactlyInAnyOrder("infra", "prod");
    }

    static Stream<Arguments> labelsUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"labels\":[\"infra\",\"prod\"]}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/labels\",\"value\":[\"infra\",\"prod\"]}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("tagsUpdateVariants")
    void should_update_tags(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getTags)
            .asList()
            .containsExactlyInAnyOrder("tag-a", "tag-b");
    }

    static Stream<Arguments> tagsUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"tags\":[\"tag-a\",\"tag-b\"]}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/tags\",\"value\":[\"tag-a\",\"tag-b\"]}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("lifecycleStateUpdateVariants")
    void should_update_lifecycle_state(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getLifecycleState().name())
            .isEqualTo("DEPRECATED");
    }

    static Stream<Arguments> lifecycleStateUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"lifecycleState\":\"DEPRECATED\"}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/lifecycleState\",\"value\":\"DEPRECATED\"}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("categoriesUpdateVariants")
    void should_update_categories(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getCategories)
            .asList()
            .containsExactlyInAnyOrder("cat-a", "cat-b");
    }

    static Stream<Arguments> categoriesUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"categories\":[\"cat-a\",\"cat-b\"]}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/categories\",\"value\":[\"cat-a\",\"cat-b\"]}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("analyticsUpdateVariants")
    void should_update_analytics(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getAnalytics().getEnabled())
            .isEqualTo(true);
    }

    static Stream<Arguments> analyticsUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"analytics\":{\"enabled\":true}}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/analytics\",\"value\":{\"enabled\":true}}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("failoverUpdateVariants")
    void should_update_failover(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(a -> a.getFailover().getEnabled())
            .isEqualTo(true);
    }

    static Stream<Arguments> failoverUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"failover\":{\"enabled\":true}}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/failover\",\"value\":{\"enabled\":true}}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("flowExecutionUpdateVariants")
    void should_update_flow_execution(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getFlowExecution)
            .isEqualTo(new FlowExecution().mode(FlowMode.BEST_MATCH).matchRequired(true));
    }

    static Stream<Arguments> flowExecutionUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"flowExecution\":{\"mode\":\"BEST_MATCH\",\"matchRequired\":true}}", MERGE_PATCH_TYPE),
            Arguments.of(
                "[{\"op\":\"replace\",\"path\":\"/flowExecution\",\"value\":{\"mode\":\"BEST_MATCH\",\"matchRequired\":true}}]",
                JSON_PATCH_TYPE
            )
        );
    }

    @ParameterizedTest
    @MethodSource("servicesUpdateVariants")
    void should_update_services(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getServices).isEqualTo(new ApiServices());
    }

    static Stream<Arguments> servicesUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"services\":{}}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/services\",\"value\":{}}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("allowedInApiProductsUpdateVariants")
    void should_update_allowed_in_api_products(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getAllowedInApiProducts).isEqualTo(true);
    }

    static Stream<Arguments> allowedInApiProductsUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"allowedInApiProducts\":true}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/allowedInApiProducts\",\"value\":true}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("allowMultiJwtOauth2UpdateVariants")
    void should_update_allow_multi_jwt_oauth2_subscriptions(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getAllowMultiJwtOauth2Subscriptions).isEqualTo(true);
    }

    static Stream<Arguments> allowMultiJwtOauth2UpdateVariants() {
        return Stream.of(
            Arguments.of("{\"allowMultiJwtOauth2Subscriptions\":true}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/allowMultiJwtOauth2Subscriptions\",\"value\":true}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("disableMembershipNotificationsUpdateVariants")
    void should_update_disable_membership_notifications(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).extracting(ApiV4::getDisableMembershipNotifications).isEqualTo(true);
    }

    static Stream<Arguments> disableMembershipNotificationsUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"disableMembershipNotifications\":true}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"replace\",\"path\":\"/disableMembershipNotifications\",\"value\":true}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("propertiesUpdateVariants")
    void should_update_properties(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getProperties)
            .asList()
            .hasSize(1)
            .first()
            .satisfies(p -> {
                var prop = (Property) p;
                Assertions.assertThat(prop.getKey()).isEqualTo("foo");
                Assertions.assertThat(prop.getValue()).isEqualTo("bar");
            });
    }

    static Stream<Arguments> propertiesUpdateVariants() {
        return Stream.of(
            Arguments.of("{\"properties\":[{\"key\":\"foo\",\"value\":\"bar\"}]}", MERGE_PATCH_TYPE),
            Arguments.of("[{\"op\":\"add\",\"path\":\"/properties\",\"value\":[{\"key\":\"foo\",\"value\":\"bar\"}]}]", JSON_PATCH_TYPE)
        );
    }

    @ParameterizedTest
    @MethodSource("responseTemplatesUpdateVariants")
    void should_update_response_templates(String body, String contentType) {
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiV4.class)
            .extracting(ApiV4::getResponseTemplates)
            .satisfies(templates -> {
                Assertions.assertThat(templates).containsKey("FOO_KEY");
                var fooTemplate = templates.get("FOO_KEY");
                Assertions.assertThat(fooTemplate).containsKey("application/json");
                var rt = fooTemplate.get("application/json");
                Assertions.assertThat(rt.getStatusCode()).isEqualTo(400);
                Assertions.assertThat(rt.getBody()).isEqualTo("{}");
            });
    }

    static Stream<Arguments> responseTemplatesUpdateVariants() {
        return Stream.of(
            Arguments.of(
                "{\"responseTemplates\":{\"FOO_KEY\":{\"application/json\":{\"statusCode\":400,\"body\":\"{}\"}}}}",
                MERGE_PATCH_TYPE
            ),
            Arguments.of(
                "[{\"op\":\"replace\",\"path\":\"/responseTemplates\",\"value\":{\"FOO_KEY\":{\"application/json\":{\"statusCode\":400,\"body\":\"{}\"}}}}]",
                JSON_PATCH_TYPE
            )
        );
    }
}
