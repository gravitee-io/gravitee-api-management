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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.rest.api.management.v2.rest.mapper.DuplicateApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.DuplicateApiOptions;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDuplicateException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApiResource_DuplicateApiTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_duplicate";
    }

    @Test
    void should_return_404_if_not_found() {
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(NOT_FOUND_404);
        assertThat(error.getMessage()).isEqualTo("Api [" + API + "] cannot be found.");
    }

    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvSource(
        delimiterString = "|",
        useHeadersInDisplayName = true,
        textBlock = """
        API_DEFINITION[READ] |  ENVIRONMENT_API[CREATE]
        false                  |  false
        true                   |  false
        false                  |  true
     """
    )
    void should_return_403_if_incorrect_permissions(boolean apiDefinitionRead, boolean currentEnvironmentApiCreate) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(apiDefinitionRead);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq(ENVIRONMENT),
                eq(RolePermissionAction.CREATE)
            )
        )
            .thenReturn(currentEnvironmentApiCreate);
        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(FORBIDDEN_403);
        assertThat(error.getMessage()).isEqualTo("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_return_400_when_duplicate_v1_api() {
        var apiEntity = ApiFixtures.aModelApiV1().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(error.getMessage()).isEqualTo("Duplicating V1 API is not supported");
    }

    @Test
    void should_return_400_when_duplicate_exception_is_thrown() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        var duplicateOptions = aDuplicateApiOptions();
        when(
            apiDuplicateService.duplicate(
                eq(GraviteeContext.getExecutionContext()),
                eq(apiEntity),
                eq(DuplicateApiMapper.INSTANCE.map(duplicateOptions))
            )
        )
            .thenThrow(new ApiDuplicateException("duplication exception message"));

        final Response response = rootTarget().request().post(Entity.json(aDuplicateApiOptions()));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(error.getMessage()).isEqualTo("duplication exception message");
    }

    @Test
    void should_duplicate_v4_api() {
        ApiEntity apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        var duplicateOptions = aDuplicateApiOptions();
        when(
            apiDuplicateService.duplicate(
                eq(GraviteeContext.getExecutionContext()),
                eq(apiEntity),
                eq(DuplicateApiMapper.INSTANCE.map(duplicateOptions))
            )
        )
            .thenReturn(ApiFixtures.aModelHttpApiV4().toBuilder().id("duplicate").build());

        final Response response = rootTarget().request().post(Entity.json(duplicateOptions));
        assertThat(response.getStatus()).isEqualTo(OK_200);

        final ApiV4 duplicated = response.readEntity(ApiV4.class);
        assertThat(duplicated.getId()).isEqualTo("duplicate");

        verifyNoInteractions(apiDuplicatorService);
    }

    @Test
    void should_duplicate_v2_api() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = ApiFixtures.aModelApiV2().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        var duplicateOptions = aDuplicateApiOptions();
        when(
            apiDuplicatorService.duplicate(
                eq(GraviteeContext.getExecutionContext()),
                eq(apiEntity),
                eq(DuplicateApiMapper.INSTANCE.mapToV2(duplicateOptions))
            )
        )
            .thenReturn(ApiFixtures.aModelApiV2().toBuilder().id("duplicate").build());

        final Response response = rootTarget().request().post(Entity.json(duplicateOptions));
        assertThat(response.getStatus()).isEqualTo(OK_200);

        final ApiV2 duplicated = response.readEntity(ApiV2.class);
        assertThat(duplicated.getId()).isEqualTo("duplicate");

        verifyNoInteractions(apiDuplicateService);
    }

    private DuplicateApiOptions aDuplicateApiOptions() {
        return DuplicateApiOptions
            .builder()
            .contextPath("/duplicate")
            .filteredFields(Set.of(DuplicateApiOptions.FilteredFieldsEnum.GROUPS))
            .build();
    }
}
