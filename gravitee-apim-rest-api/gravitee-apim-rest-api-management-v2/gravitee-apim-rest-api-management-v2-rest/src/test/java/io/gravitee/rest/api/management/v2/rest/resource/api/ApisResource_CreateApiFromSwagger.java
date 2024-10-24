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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ImportSwaggerDescriptor;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApisResource_CreateApiFromSwagger extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "my-env";

    @Autowired
    private OAIDomainService oaiDomainService;

    @Autowired
    private CreateApiDomainService createApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/apis/_import/swagger";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        reset(apiServiceV4);
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT_ID)).thenReturn(environmentEntity);

        parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                )
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_NAME).build()));
    }

    @Test
    public void should_not_import_when_no_definition_permission() {
        // Given
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_API,
                ENVIRONMENT_ID,
                RolePermissionAction.CREATE
            )
        )
            .thenReturn(false);

        // When
        var response = rootTarget().request().post(null);

        // Then
        assertThat(FORBIDDEN_403).isEqualTo(response.getStatus());
    }

    @Test
    @SneakyThrows
    public void should_throw_invalid_paths_exception() {
        // Given
        when(oaiDomainService.convert(any(), any(), any()))
            .thenReturn(
                ImportDefinition
                    .builder()
                    .apiExport(
                        ApiExport
                            .builder()
                            .definitionVersion(DefinitionVersion.V4)
                            .listeners(List.of(HttpListener.builder().paths(List.of(Path.builder().path("/path").build())).build()))
                            .build()
                    )
                    .build()
            );
        when(createApiDomainService.create(any(), any(), any(), any(), any())).thenThrow(new InvalidPathsException("Invalid paths"));

        var resource = Resources.getResource("io/gravitee/rest/api/management/service/openapi-withExtensions.json");

        // When
        var response = rootTarget()
            .request()
            .post(Entity.json(ImportSwaggerDescriptor.builder().payload(Resources.toString(resource, Charsets.UTF_8)).build()));

        // Then
        var error = response.readEntity(Error.class);
        assertThat(BAD_REQUEST_400).isEqualTo((int) error.getHttpStatus());
        assertThat("Cannot import API with invalid paths (Invalid paths)").isEqualTo(error.getMessage());
    }
}
