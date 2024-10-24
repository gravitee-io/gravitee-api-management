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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.apim.core.api.use_case.GetApiDefinitionUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_DeploymentsCurrentTest extends ApiResourceTest {

    private static final String UNKNOWN_API = "unknown";

    @Autowired
    GetApiDefinitionUseCase getApiDefinitionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    public void should_get_api_v4_deployments() {
        when(getApiDefinitionUseCase.execute(any()))
            .thenReturn(new GetApiDefinitionUseCase.Output(DefinitionVersion.V4, ApiDefinitionFixtures.anApiV4(), null));

        final Response response = rootTarget(API + "/deployments/current").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void should_get_api_v2_deployments() {
        when(getApiDefinitionUseCase.execute(any()))
            .thenReturn(new GetApiDefinitionUseCase.Output(DefinitionVersion.V2, null, ApiDefinitionFixtures.anApiV2()));

        final Response response = rootTarget(API + "/deployments/current").request().get();
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void should_throw_with_insufficient_rights() {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any()))
            .thenReturn(false);
        final Response response = rootTarget(API + "/deployments/current").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }
}
