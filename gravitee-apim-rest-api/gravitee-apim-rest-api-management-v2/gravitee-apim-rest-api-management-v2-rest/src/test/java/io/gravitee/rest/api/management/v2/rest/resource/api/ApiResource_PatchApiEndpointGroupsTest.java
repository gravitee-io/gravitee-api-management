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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiEndpointGroupsTest extends ApiResourceTest {

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
        givenApiWithEndpointGroups(List.of(defaultGroup("default-group")));

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

    private void givenApiWithEndpointGroups(List<EndpointGroup> groups) {
        var base = ApiFixtures.aProxyApiV4();
        var api = base.toBuilder().apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().endpointGroups(groups).build()).build();
        apiCrudService.initWith(List.of(api));
    }

    private static EndpointGroup defaultGroup(String name) {
        return EndpointGroup.builder()
            .name(name)
            .type("http-proxy")
            .sharedConfiguration("{}")
            .endpoints(
                List.of(
                    io.gravitee.definition.model.v4.endpointgroup.Endpoint.builder()
                        .name("default-endpoint")
                        .type("http-proxy")
                        .weight(1)
                        .inheritConfiguration(true)
                        .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                        .build()
                )
            )
            .build();
    }

    private static Map<String, Object> groupMap(String name) {
        return Map.of(
            "name",
            name,
            "type",
            "http-proxy",
            "sharedConfiguration",
            "{}",
            "endpoints",
            List.of(
                Map.of(
                    "name",
                    "default-endpoint",
                    "type",
                    "http-proxy",
                    "weight",
                    1,
                    "inheritConfiguration",
                    true,
                    "configuration",
                    "{\"target\":\"https://api.gravitee.io/echo\"}"
                )
            )
        );
    }

    private static String mergePatchEndpointGroups(List<Map<String, Object>> groups) {
        return "{\"endpointGroups\":" + toJson(groups) + "}";
    }

    private static String jsonPatchReplaceEndpointGroups(List<Map<String, Object>> groups) {
        return "[{\"op\":\"replace\",\"path\":\"/endpointGroups\",\"value\":" + toJson(groups) + "}]";
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void patch_endpoint_group_type_http_proxy_returns_200_and_persists() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));

        var groups = List.of(groupMap("group-a"));
        var body = "{\"endpointGroups\":" + toJson(groups) + "}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getEndpointGroups()).isNotEmpty();
        Assertions.assertThat(apiV4.getEndpointGroups().getFirst().getName()).isEqualTo("group-a");

        var persistedGroups = capturePersistedEndpointGroups();
        Assertions.assertThat(persistedGroups).hasSize(1);
        Assertions.assertThat(persistedGroups.getFirst().getType()).isEqualTo("http-proxy");
    }

    private List<EndpointGroup> capturePersistedEndpointGroups() {
        var captor = ArgumentCaptor.forClass(Api.class);
        Mockito.verify(updateApiDomainService).updateV4(captor.capture(), any());
        return captor.getValue().getApiDefinitionHttpV4().getEndpointGroups();
    }

    @Test
    void merge_patch_omitting_endpoint_groups_preserves_previous_endpoint_groups() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));

        var body = "{\"name\":\"updated-name\"}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);

        var persistedGroups = capturePersistedEndpointGroups();
        Assertions.assertThat(persistedGroups).hasSize(1);
        Assertions.assertThat(persistedGroups.getFirst().getType()).isEqualTo("http-proxy");
        Assertions.assertThat(persistedGroups.getFirst().getName()).isEqualTo("group-a");
    }

    @Test
    void merge_patch_endpoint_groups_null_clears_endpoint_groups() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));

        var body = "{\"endpointGroups\":null}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getEndpointGroups()).isNullOrEmpty();
    }

    @Test
    void json_patch_remove_endpoint_groups_clears_endpoint_groups() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));

        var body = "[{\"op\":\"remove\",\"path\":\"/endpointGroups\"}]";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, JSON_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getEndpointGroups()).isNullOrEmpty();
    }

    @Test
    void patch_with_empty_endpointGroups_returns_400() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));
        doThrow(new ValidationDomainException("endpointGroups must not be empty", Map.of("location", "/endpointGroups"), "invalidValue"))
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"endpointGroups\":[]}", MERGE_PATCH_TYPE));

        var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
        Assertions.assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
        Assertions.assertThat(error.getParameters()).containsEntry("location", "/endpointGroups");
    }

    static Stream<Arguments> validatorRejectionCases() {
        return Stream.of(
            Map.entry("endpoint group name must not be blank", "/endpointGroups/0/name"),
            Map.entry("endpoint type is invalid", "/endpointGroups/0/type")
        ).flatMap(entry ->
            Stream.of(MERGE_PATCH_TYPE, JSON_PATCH_TYPE).map(contentType -> Arguments.of(entry.getKey(), entry.getValue(), contentType))
        );
    }

    @ParameterizedTest
    @MethodSource("validatorRejectionCases")
    void post_patch_validator_failure_returns_400_with_invalidValue_envelope_and_location(
        String message,
        String location,
        String contentType
    ) {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));
        doThrow(new ValidationDomainException(message, Map.of("location", location), "invalidValue"))
            .when(updateApiDomainService)
            .updateV4(any(), any());

        var groups = List.of(groupMap("group-a"));
        var body = contentType.equals(JSON_PATCH_TYPE) ? jsonPatchReplaceEndpointGroups(groups) : mergePatchEndpointGroups(groups);
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, contentType));

        var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
        Assertions.assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
        Assertions.assertThat(error.getParameters()).containsEntry("location", location);
    }

    @Test
    void dry_run_patch_returns_200_without_persisting_endpointGroups() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));

        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"endpointGroups\":" + toJson(List.of(groupMap("group-b"))) + "}", MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getEndpointGroups().getFirst().getName()).isEqualTo("group-b");
    }

    @Test
    void dry_run_validation_failure_returns_400() {
        givenApiWithEndpointGroups(List.of(defaultGroup("group-a")));
        doThrow(new ValidationDomainException("invalid endpoint group", Map.of("location", "/endpointGroups/0"), "invalidValue"))
            .when(updateApiDomainService)
            .validateV4(any(), any());

        var response = rootTarget(API)
            .queryParam("dryRun", "true")
            .request()
            .method("PATCH", Entity.entity("{\"endpointGroups\":" + toJson(List.of(groupMap("bad-group"))) + "}", MERGE_PATCH_TYPE));

        var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
        Assertions.assertThat(error.getTechnicalCode()).isEqualTo("invalidValue");
        Assertions.assertThat(error.getParameters()).containsEntry("location", "/endpointGroups/0");
    }
}
