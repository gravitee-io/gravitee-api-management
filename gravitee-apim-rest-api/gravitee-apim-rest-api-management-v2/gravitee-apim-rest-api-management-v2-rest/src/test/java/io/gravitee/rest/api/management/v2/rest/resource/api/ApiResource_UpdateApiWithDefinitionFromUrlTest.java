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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.UpdateApiDefinitionFromImportUseCase;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.vertx.core.buffer.Buffer;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class ApiResource_UpdateApiWithDefinitionFromUrlTest extends ApiResourceTest {

    private static final String TEST_URL = "https://example.com/api-definition.json";

    /**
     * The {@code api.id} in this fixture intentionally differs from the path {@link #API}: the contract is that the path
     * parameter wins (the use case receives the path apiId), so the body's id must be ignored.
     */
    private static final String BODY_API_ID = "id-from-body-should-be-ignored";
    private static final String VALID_API_DEFINITION_JSON = """
        {
          "api": {
            "id": "%s",
            "name": "Test API",
            "description": "Test API Description",
            "version": "1.0.0",
            "definitionVersion": "V4",
            "type": "PROXY",
            "apiVersion": "1.0.0",
            "listeners": [
              {
                "type": "HTTP",
                "paths": [
                  {
                    "path": "/test"
                  }
                ],
                "entrypoints": [
                  {
                    "type": "http-proxy"
                  }
                ]
              }
            ],
            "endpointGroups": [
              {
                "name": "default-group",
                "type": "http-proxy",
                "endpoints": [
                  {
                    "name": "default",
                    "type": "http-proxy",
                    "weight": 1,
                    "inheritConfiguration": false,
                    "configuration": {
                      "target": "http://localhost:8080/endpoint"
                    }
                  }
                ]
              }
            ],
            "flowExecution": {
              "mode": "DEFAULT",
              "matchRequired": false
            },
            "flows": [],
            "responseTemplates": {},
            "analytics": {
              "enabled": false
            },
            "lifecycleState": "CREATED"
          },
          "plans": []
        }
        """.formatted(BODY_API_ID);

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private ImportConfiguration importConfiguration;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_import/definition-url";
    }

    @BeforeEach
    public void initUrlImportMocks() {
        reset(httpClientService, importConfiguration);
        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.emptyList());
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
    }

    @Test
    public void should_return_403_when_no_definition_update_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
    }

    @Test
    public void should_update_api_from_url_when_url_is_valid() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);

        Buffer buffer = Buffer.buffer(VALID_API_DEFINITION_JSON);
        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenReturn(buffer);

        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API).environmentId(ENVIRONMENT).build();
        var apiWithFlows = new ApiWithFlows(existingApi, List.of());
        when(updateApiDefinitionUseCase.execute(any())).thenReturn(new UpdateApiDefinitionFromImportUseCase.Output(apiWithFlows));
        when(apiSearchServiceV4.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(
            io.gravitee.rest.api.model.v4.api.ApiEntity.builder().id(API).name("Test API").apiVersion("1.0").build()
        );

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        verify(httpClientService).request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any());

        ArgumentCaptor<UpdateApiDefinitionFromImportUseCase.Input> inputCaptor = ArgumentCaptor.forClass(
            UpdateApiDefinitionFromImportUseCase.Input.class
        );
        verify(updateApiDefinitionUseCase).execute(inputCaptor.capture());
        assertThat(inputCaptor.getValue().apiId())
            .as("Path apiId must win over body id; got %s", inputCaptor.getValue().apiId())
            .isEqualTo(API)
            .isNotEqualTo(BODY_API_ID);
    }

    @Test
    public void should_return_bad_request_when_url_is_private_and_private_not_allowed() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);

        String privateUrl = "http://localhost:8080/api-definition.json";
        Response response = rootTarget().request().put(Entity.text(privateUrl));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    public void should_return_bad_request_when_url_returns_invalid_json() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);

        Buffer buffer = Buffer.buffer("not valid json");
        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenReturn(buffer);

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

        // Guard against leaking Jackson internals back to the caller (review feedback on PR #16844).
        String body = response.readEntity(String.class);
        assertThat(body).contains("Invalid API definition format").doesNotContain("io.gravitee", "fasterxml", "JsonProcessingException");
    }

    @Test
    public void should_return_bad_request_when_remote_returns_error_status() {
        // Defect A: the remote URL responds with a non-success status (e.g. 404). HttpClientService surfaces this
        // as a TechnicalManagementException; it must become a clean 4xx, not a 500 leaking the remote response.
        grantUpdatePermission();

        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenThrow(
            new TechnicalManagementException(" Error on url '" + TEST_URL + "'. Status code: 404. Message: Not Found", null)
        );

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        String body = response.readEntity(String.class);
        assertThat(body)
            .contains("Unable to fetch the API definition")
            .doesNotContain("io.gravitee", "TechnicalManagementException", "Status code", "404");
    }

    @Test
    public void should_return_bad_request_when_remote_host_unreachable() {
        // Defect B: the remote host cannot be resolved/reached. Must become a clean 4xx without leaking the
        // UnknownHostException or the failing host.
        grantUpdatePermission();

        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenThrow(
            new TechnicalManagementException(
                "Failed to resolve 'nonexistent-host-xyz.invalid'",
                new java.net.UnknownHostException("nonexistent-host-xyz.invalid")
            )
        );

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        String body = response.readEntity(String.class);
        assertThat(body)
            .contains("Unable to fetch the API definition")
            .doesNotContain("io.gravitee", "UnknownHostException", "Failed to resolve", "nonexistent-host");
    }

    @Test
    public void should_return_bad_request_when_definition_is_not_a_gravitee_export() {
        // Defect C: valid JSON that is not a Gravitee export (missing 'api'). Must be a clean 400, not an NPE → 500.
        grantUpdatePermission();

        Buffer buffer = Buffer.buffer("{\"hello\":\"world\"}");
        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenReturn(buffer);

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        String body = response.readEntity(String.class);
        assertThat(body).contains("Invalid API definition").doesNotContain("io.gravitee", "NullPointerException");
    }

    @Test
    public void should_return_bad_request_when_definition_fails_bean_validation() {
        // Valid JSON that deserializes into an ExportApiV4 with a non-null 'api' but violates a Bean Validation
        // constraint (member displayName is @Size(min=1)). Must surface as a clean 400, not a 500.
        grantUpdatePermission();

        Buffer buffer = Buffer.buffer("{\"api\":{\"definitionVersion\":\"V4\"},\"members\":[{\"displayName\":\"\"}]}");
        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenReturn(buffer);

        Response response = rootTarget().request().put(Entity.text(TEST_URL));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        String body = response.readEntity(String.class);
        assertThat(body).contains("Validation error").doesNotContain("io.gravitee", "ConstraintViolationException");
    }

    private void grantUpdatePermission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);
    }

    @Test
    public void should_reject_url_not_in_whitelist() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);

        when(importConfiguration.getImportWhitelist()).thenReturn(List.of("https://allowed.example.com"));

        String notWhitelistedUrl = "https://other.example.com/api-definition.json";
        Response response = rootTarget().request().put(Entity.text(notWhitelistedUrl));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }
}
