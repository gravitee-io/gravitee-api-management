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

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.ImportApiDefinitionUseCase;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApisResource_CreateApiWithDefinitionTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "my-env";

    @Autowired
    private ImportApiDefinitionUseCase importApiDefinitionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/apis/_import/definition";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        reset(apiServiceV4, importApiDefinitionUseCase);
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT_ID)).thenReturn(environmentEntity);
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
        Response response = rootTarget().request().post(null);
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_return_resources_in_response_on_success() {
        var resource = Resource.builder().name("cache-resource").type("cache").enabled(true).configuration("{\"key\":\"value\"}").build();
        var baseApi = ApiFixtures.aProxyApiV4();
        var createdApi = baseApi
            .toBuilder()
            .id("imported-api-id")
            .environmentId(ENVIRONMENT_ID)
            .apiDefinitionValue(baseApi.getApiDefinitionHttpV4().toBuilder().resources(List.of(resource)).build())
            .build();
        var apiWithFlows = new ApiWithFlows(createdApi, List.of());

        when(importApiDefinitionUseCase.execute(any())).thenReturn(new ImportApiDefinitionUseCase.Output(apiWithFlows));

        Response response = rootTarget().request().post(Entity.json(buildMinimalExportApiV4()));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var body = response.readEntity(ApiV4.class);
        assertThat(body.getResources()).isNotNull().hasSize(1);
        assertThat(body.getResources().getFirst().getName()).isEqualTo("cache-resource");
    }

    private ExportApiV4 buildMinimalExportApiV4() {
        var apiV4 = new ApiV4();
        apiV4.setId("imported-api-id");
        apiV4.setName("Test API");
        apiV4.setApiVersion("1.0");
        apiV4.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);
        apiV4.setType(io.gravitee.rest.api.management.v2.rest.model.ApiType.PROXY);

        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(apiV4);
        return exportApiV4;
    }
}
