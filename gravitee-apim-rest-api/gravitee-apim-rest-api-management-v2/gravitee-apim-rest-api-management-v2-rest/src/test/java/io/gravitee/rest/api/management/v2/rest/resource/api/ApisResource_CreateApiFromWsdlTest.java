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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.WsdlToImportApiUseCase;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ImportWsdlDescriptor;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApisResource_CreateApiFromWsdlTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "my-env";

    @Autowired
    private WsdlToImportApiUseCase wsdlToImportApiUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/apis/_import/wsdl";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        reset(wsdlToImportApiUseCase);
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
    public void should_return_403_when_no_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            )
        ).thenReturn(false);

        var response = rootTarget().request().post(null);

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
    }

    @Test
    public void should_return_201_with_created_api() {
        var createdApi = Api.builder()
            .id("wsdl-api-id")
            .name("CalculatorService")
            .environmentId(ENVIRONMENT_ID)
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .build();
        when(wsdlToImportApiUseCase.execute(any())).thenReturn(new WsdlToImportApiUseCase.Output(new ApiWithFlows(createdApi, List.of())));

        var descriptor = new ImportWsdlDescriptor().payload("<definitions/>").withDocumentation(false).withOASValidationPolicy(false);

        var response = rootTarget().request().post(Entity.json(descriptor));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var api = response.readEntity(ApiV4.class);
        assertThat(api.getId()).isEqualTo("wsdl-api-id");
        assertThat(api.getName()).isEqualTo("CalculatorService");
    }

    @Test
    public void should_return_resources_in_response_on_success() {
        var resource = Resource.builder().name("cache-resource").type("cache").enabled(true).configuration("{\"key\":\"value\"}").build();
        var baseApi = ApiFixtures.aProxyApiV4();
        var createdApi = baseApi
            .toBuilder()
            .id("wsdl-api-id")
            .environmentId(ENVIRONMENT_ID)
            .apiDefinitionValue(baseApi.getApiDefinitionHttpV4().toBuilder().resources(List.of(resource)).build())
            .build();
        when(wsdlToImportApiUseCase.execute(any())).thenReturn(new WsdlToImportApiUseCase.Output(new ApiWithFlows(createdApi, List.of())));

        var descriptor = new ImportWsdlDescriptor().payload("<definitions/>").withDocumentation(false).withOASValidationPolicy(false);

        var response = rootTarget().request().post(Entity.json(descriptor));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var api = response.readEntity(ApiV4.class);
        assertThat(api.getResources()).isNotNull().hasSize(1);
        assertThat(api.getResources().getFirst().getName()).isEqualTo("cache-resource");
    }

    @Test
    public void should_return_400_when_invalid_paths() {
        when(wsdlToImportApiUseCase.execute(any())).thenThrow(new InvalidPathsException("Invalid paths"));

        var descriptor = new ImportWsdlDescriptor().payload("<definitions/>");

        var response = rootTarget().request().post(Entity.json(descriptor));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(error.getMessage()).isEqualTo("Cannot import API with invalid paths (Invalid paths)");
    }
}
