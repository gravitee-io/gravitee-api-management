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
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.ImportApiDefinitionUseCase;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.vertx.core.buffer.Buffer;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApisResource_CreateApiWithDefinitionFromUrlTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "my-env";
    private static final String TEST_URL = "https://example.com/api-definition.json";
    private static final String VALID_API_DEFINITION_JSON = """
        {
          "api": {
            "id": "test-api-id",
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
        """;

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private ImportConfiguration importConfiguration;

    @Autowired
    private ImportApiDefinitionUseCase importApiDefinitionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/apis/_import/definition-url";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        reset(apiServiceV4, httpClientService, importConfiguration, importApiDefinitionUseCase);
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT_ID)).thenReturn(environmentEntity);

        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.emptyList());
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);
    }

    @Test
    public void should_not_import_when_no_definition_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            )
        ).thenReturn(false);
        Response response = rootTarget().request(MediaType.APPLICATION_JSON).post(Entity.text(TEST_URL));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_import_api_from_url_when_url_is_valid() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            )
        ).thenReturn(true);

        Buffer buffer = Buffer.buffer(VALID_API_DEFINITION_JSON);
        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenReturn(buffer);

        ApiWithFlows mockApi = ApiWithFlows.builder().id("test-api-id").name("Test API").build();
        when(importApiDefinitionUseCase.execute(any())).thenReturn(new ImportApiDefinitionUseCase.Output(mockApi));

        Response response = rootTarget().request(MediaType.APPLICATION_JSON).post(Entity.text(TEST_URL));
        assertEquals(CREATED_201, response.getStatus());
    }

    @Test
    public void should_return_bad_request_when_url_is_forbidden() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            )
        ).thenReturn(true);

        when(importConfiguration.getImportWhitelist()).thenReturn(Collections.emptyList());
        when(importConfiguration.isAllowImportFromPrivate()).thenReturn(false);

        String privateUrl = "http://localhost:8080/api-definition.json";
        Response response = rootTarget().request(MediaType.APPLICATION_JSON).post(Entity.text(privateUrl));
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_return_bad_request_when_url_returns_invalid_json() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            )
        ).thenReturn(true);

        Buffer buffer = Buffer.buffer("invalid json content");
        when(httpClientService.request(eq(HttpMethod.GET), eq(TEST_URL), any(), any(), any())).thenReturn(buffer);

        Response response = rootTarget().request(MediaType.APPLICATION_JSON).post(Entity.text(TEST_URL));
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }
}
