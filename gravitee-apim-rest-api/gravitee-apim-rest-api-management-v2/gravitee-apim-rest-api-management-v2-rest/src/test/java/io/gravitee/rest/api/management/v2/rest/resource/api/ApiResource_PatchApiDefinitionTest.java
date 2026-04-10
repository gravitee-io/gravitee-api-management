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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.PRECONDITION_FAILED_412;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.model.JsonPatch;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests JSON Patch on {@link ApiResource} at {@code PATCH .../apis/{apiId}/definition}.
 */
class ApiResource_PatchApiDefinitionTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    void should_return_200_when_dry_run_without_persist() {
        Date fixedDate = new Date(1_700_000_000_000L);
        ApiEntity apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).updatedAt(fixedDate).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(apiEntity);
        when(apiExportDomainService.export(eq(API), any(), any())).thenReturn(minimalProxyExport("original-name"));

        Response response = rootTarget(API + "/definition")
            .queryParam("dryRun", true)
            .request(MediaType.APPLICATION_JSON)
            .method("PATCH", Entity.json(List.of(jsonPatchReplace("$.api.name", "dry-run-name-only"))));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        ExportApiV4 body = response.readEntity(ExportApiV4.class);
        assertThat(body.getApi().getName()).isEqualTo("dry-run-name-only");
        verify(apiServiceV4, never()).update(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void should_return_200_with_new_etag_when_persist_with_matching_if_match() {
        Date before = new Date(1_700_000_000_000L);
        Date after = new Date(1_700_000_000_001L);
        ApiEntity current = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).name("before").updatedAt(before).build();
        ApiEntity updated = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).name("tes-api-key-patched").updatedAt(after).build();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(current);
        when(apiExportDomainService.export(eq(API), any(), any())).thenReturn(minimalProxyExport("before"));
        when(
            apiServiceV4.update(eq(GraviteeContext.getExecutionContext()), eq(API), any(UpdateApiEntity.class), eq(false), eq(USER_NAME))
        ).thenReturn(updated);

        Response response = rootTarget(API + "/definition")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.IF_MATCH, "\"" + before.getTime() + "\"")
            .method("PATCH", Entity.json(List.of(jsonPatchReplace("$.api.name", "tes-api-key-patched"))));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.getHeaderString(HttpHeaders.ETAG)).isEqualTo("\"" + after.getTime() + "\"");
        assertThat(response.readEntity(Api.class).getApiV4().getName()).isEqualTo("tes-api-key-patched");
        verify(apiServiceV4, times(1)).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            any(UpdateApiEntity.class),
            eq(false),
            eq(USER_NAME)
        );
        verify(apiExportDomainService, times(1)).export(eq(API), any(), any());
    }

    @Test
    void should_return_200_when_persist_without_if_match() {
        Date updated = new Date(1_700_000_000_002L);
        ApiEntity current = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).updatedAt(updated).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(current);
        when(apiExportDomainService.export(eq(API), any(), any())).thenReturn(minimalProxyExport("api-name"));
        when(
            apiServiceV4.update(eq(GraviteeContext.getExecutionContext()), eq(API), any(UpdateApiEntity.class), eq(false), eq(USER_NAME))
        ).thenReturn(current);

        Response response = rootTarget(API + "/definition")
            .request(MediaType.APPLICATION_JSON)
            .method("PATCH", Entity.json(List.of(jsonPatchReplace("$.api.description", "Updated via PATCH"))));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        verify(apiServiceV4, times(1)).update(
            eq(GraviteeContext.getExecutionContext()),
            eq(API),
            any(UpdateApiEntity.class),
            eq(false),
            eq(USER_NAME)
        );
        verify(apiExportDomainService, times(1)).export(eq(API), any(), any());
    }

    @Test
    void should_return_412_when_if_match_does_not_match() {
        Date currentRevision = new Date(1_700_000_000_000L);
        ApiEntity current = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).updatedAt(currentRevision).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(current);
        when(apiExportDomainService.export(eq(API), any(), any())).thenReturn(minimalProxyExport("x"));

        Response response = rootTarget(API + "/definition")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.IF_MATCH, "\"0\"")
            .method("PATCH", Entity.json(List.of(jsonPatchReplace("$.api.name", "will-not-apply"))));

        assertThat(response.getStatus()).isEqualTo(PRECONDITION_FAILED_412);
        verify(apiServiceV4, never()).update(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void should_return_400_when_api_is_not_v4_http_proxy() {
        NativeApiEntity nativeApi = ApiFixtures.aModelNativeApiV4().withId(API);
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(nativeApi);

        Response response = rootTarget(API + "/definition")
            .request(MediaType.APPLICATION_JSON)
            .method("PATCH", Entity.json(List.of(jsonPatchReplace("$.api.name", "x"))));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        Error error = response.readEntity(Error.class);
        assertThat(error.getMessage())
            .contains("JSON Patch on the API definition is only available for V4 HTTP Proxy APIs")
            .contains("not supported");
        assertThat(error.getTechnicalCode()).isEqualTo("api.definition.patch.unsupported");
        assertThat(error.getParameters()).containsEntry("apiId", API);
    }

    @Test
    void should_return_403_when_missing_definition_update_permissions() {
        ApiEntity current = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(current);
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

        Response response = rootTarget(API + "/definition")
            .request(MediaType.APPLICATION_JSON)
            .method("PATCH", Entity.json(List.of(jsonPatchReplace("$.api.name", "x"))));

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
        Error error = response.readEntity(Error.class);
        assertThat(error.getMessage()).isEqualTo("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_return_412_when_json_patch_test_fails() {
        Date fixedDate = new Date(1_700_000_000_000L);
        ApiEntity apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).name("actual-name").updatedAt(fixedDate).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(apiEntity);
        when(apiExportDomainService.export(eq(API), any(), any())).thenReturn(minimalProxyExport("actual-name"));

        Response response = rootTarget(API + "/definition")
            .request(MediaType.APPLICATION_JSON)
            .method("PATCH", Entity.json(List.of(jsonPatchTest("$.api.name", "this-is-not-the-current-name"))));

        assertThat(response.getStatus()).isEqualTo(PRECONDITION_FAILED_412);
        Error error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(PRECONDITION_FAILED_412);
        assertThat(error.getTechnicalCode()).isEqualTo("jsonPatch.testFailed");
        assertThat(error.getMessage()).isEqualTo(
            "The json patch does not validate the condition $.api.name == this-is-not-the-current-name"
        );
        verify(apiServiceV4, never()).update(any(), any(), any(), anyBoolean(), any());
    }

    private static GraviteeDefinition minimalProxyExport(String apiName) {
        return GraviteeDefinition.from(
            ApiDescriptor.ApiDescriptorV4.builder().id(API).crossId("cross").name(apiName).apiVersion("1.0").type(ApiType.PROXY).build(),
            Set.of(),
            List.of(),
            List.of(),
            List.<PlanDescriptor.V4>of(),
            List.of(),
            null,
            null
        );
    }

    private static JsonPatch jsonPatchReplace(String path, String value) {
        JsonPatch p = new JsonPatch();
        p.setJsonPath(path);
        p.setOperation(JsonPatch.Operation.REPLACE);
        p.setValue(value);
        return p;
    }

    private static JsonPatch jsonPatchTest(String path, String value) {
        JsonPatch p = new JsonPatch();
        p.setJsonPath(path);
        p.setOperation(JsonPatch.Operation.TEST);
        p.setValue(value);
        return p;
    }
}
