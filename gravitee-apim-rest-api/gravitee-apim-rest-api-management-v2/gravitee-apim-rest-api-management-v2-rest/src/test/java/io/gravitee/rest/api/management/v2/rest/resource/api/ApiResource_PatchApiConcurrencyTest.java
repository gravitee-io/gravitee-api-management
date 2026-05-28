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
import static io.gravitee.common.http.HttpStatusCode.PRECONDITION_FAILED_412;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiConcurrencyTest extends ApiResourceTest {

    private static final String MERGE_PATCH_TYPE = "application/merge-patch+json";

    private static final Instant updatedAt = Instant.parse("2026-06-01T13:45:30Z");
    private static final String updatedAtStr = String.format("\"%s\"", updatedAt.toEpochMilli());

    @Inject
    UpdateApiDomainService updateApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void setUpApiAndPrimaryOwner() {
        var existingApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .updatedAt(ZonedDateTime.ofInstant(updatedAt, ZoneId.systemDefault()))
            .build();
        apiCrudService.initWith(List.of(existingApi));

        // Mock apiSearchService to return the same entity
        var apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setUpdatedAt(Date.from(updatedAt));
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

    @Test
    void should_succeed_when_If_Match_matches_ETag() {
        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, updatedAtStr)
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_412_when_If_Match_does_not_match_ETag() {
        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"2000\"")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
        org.assertj.core.api.Assertions.assertThat(response.readEntity(String.class)).isEmpty();
    }

    @Test
    void should_return_412_when_If_Match_is_malformed() {
        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, "not-a-number")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_succeed_when_If_Match_is_missing() {
        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_succeed_when_If_Match_is_wildcard() {
        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, "*")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_reject_with_scope_error_for_v2_api_even_when_If_Match_is_mismatched() {
        var v2Entity = new ApiEntity();
        v2Entity.setId(API);
        v2Entity.setDefinitionVersion(DefinitionVersion.V2);
        v2Entity.setType(ApiType.PROXY);
        v2Entity.setUpdatedAt(Date.from(updatedAt));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            v2Entity
        );

        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"2000\"")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_412_when_dryRun_with_mismatched_If_Match() {
        var response = rootTarget(API)
            .queryParam("dryRun", true)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"2000\"")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(PRECONDITION_FAILED_412);
    }

    @Test
    void should_succeed_when_currentEntity_updatedAt_is_null() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().updatedAt(null).build();
        apiCrudService.initWith(List.of(existingApi));

        var apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setUpdatedAt(null);
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            apiEntity
        );

        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"2000\"")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_return_new_ETag_and_Last_Modified_on_successful_patch() {
        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, updatedAtStr)
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        org.assertj.core.api.Assertions.assertThat(response.getHeaderString(HttpHeaders.ETAG)).isEqualTo(updatedAtStr);
        org.assertj.core.api.Assertions.assertThat(response.getHeaderString(HttpHeaders.LAST_MODIFIED)).isNotNull();
    }

    @Test
    void should_round_trip_GET_PATCH_GET_with_consistent_ETag_chain() {
        var firstGetEntity = fixtures.ApiFixtures.aModelHttpApiV4().toBuilder().id(API).name(API).build();
        firstGetEntity.setUpdatedAt(Date.from(updatedAt));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            firstGetEntity
        );

        var firstGetResponse = rootTarget(API).request().get();
        assertThat(firstGetResponse).hasStatus(OK_200);
        var firstEtag = firstGetResponse.getHeaderString(HttpHeaders.ETAG);
        org.assertj.core.api.Assertions.assertThat(firstEtag).isEqualTo(updatedAtStr);

        var newUpdatedAt = Instant.ofEpochMilli(2000);
        doAnswer(inv -> {
            Api input = inv.getArgument(0);
            return input.toBuilder().updatedAt(ZonedDateTime.ofInstant(newUpdatedAt, ZoneId.systemDefault())).build();
        })
            .when(updateApiDomainService)
            .updateV4(any(), any());

        Response patchResponse = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, firstEtag)
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(patchResponse).hasStatus(OK_200);
        var patchEtag = patchResponse.getHeaderString(HttpHeaders.ETAG);
        org.assertj.core.api.Assertions.assertThat(patchEtag).isEqualTo("\"2000\"");
        org.assertj.core.api.Assertions.assertThat(patchEtag).isNotEqualTo(firstEtag);

        var updatedApiEntity = fixtures.ApiFixtures.aModelHttpApiV4().toBuilder().id(API).name(API).build();
        updatedApiEntity.setUpdatedAt(Date.from(newUpdatedAt));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            updatedApiEntity
        );

        var secondGetResponse = rootTarget(API).request().get();
        assertThat(secondGetResponse).hasStatus(OK_200);
        var secondEtag = secondGetResponse.getHeaderString(HttpHeaders.ETAG);
        org.assertj.core.api.Assertions.assertThat(secondEtag).isEqualTo(patchEtag);
        org.assertj.core.api.Assertions.assertThat(secondEtag).isNotEqualTo(firstEtag);
    }

    @Test
    void should_suppress_cache_headers_when_updatedAt_is_null_on_patch() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().updatedAt(null).build();
        apiCrudService.initWith(List.of(existingApi));

        var apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setUpdatedAt(null);
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            apiEntity
        );

        var response = rootTarget(API).request().method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        org.assertj.core.api.Assertions.assertThat(response.getEntityTag()).isNull();
        org.assertj.core.api.Assertions.assertThat(response.getLastModified()).isNull();
    }

    @Test
    void should_have_Last_Modified_encoding_same_instant_as_ETag_on_successful_patch() {
        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, updatedAtStr)
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(OK_200);
        org.assertj.core.api.Assertions.assertThat(response.getHeaderString(HttpHeaders.ETAG)).isEqualTo(updatedAtStr);
        org.assertj.core.api.Assertions.assertThat(response.getLastModified().getTime()).isEqualTo(updatedAt.toEpochMilli());
    }

    @Test
    void should_reject_with_scope_error_for_native_api_even_when_If_Match_is_mismatched() {
        var nativeEntity = new NativeApiEntity();
        nativeEntity.setId(API);
        nativeEntity.setDefinitionVersion(DefinitionVersion.V4);
        nativeEntity.setType(ApiType.NATIVE);
        nativeEntity.setUpdatedAt(Date.from(updatedAt));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            nativeEntity
        );

        var response = rootTarget(API)
            .request()
            .header(HttpHeaders.IF_MATCH, "\"2000\"")
            .method("PATCH", Entity.entity("{\"name\":\"patched\"}", MERGE_PATCH_TYPE));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }
}
