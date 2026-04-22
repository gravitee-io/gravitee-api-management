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
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.use_case.UpdateApiGroupsUseCase;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_UpdateGroupsTest extends ApiResourceTest {

    @Autowired
    private UpdateApiGroupsUseCase updateApiGroupsUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/groups";
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_MEMBER),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        var response = rootTarget().request().put(Entity.json(List.of("group-1")));

        assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    public void should_update_api_groups() {
        var groups = List.of("group-1", "group-2");
        when(updateApiGroupsUseCase.execute(any())).thenReturn(new UpdateApiGroupsUseCase.Output(Set.of("group-1", "group-2")));

        var response = rootTarget().request().put(Entity.json(groups));

        assertThat(response).hasStatus(OK_200);
        var captor = ArgumentCaptor.forClass(UpdateApiGroupsUseCase.Input.class);
        verify(updateApiGroupsUseCase).execute(captor.capture());
        var input = captor.getValue();
        assertThat(input.apiId()).isEqualTo(API);
        assertThat(input.groups()).containsExactlyInAnyOrder("group-1", "group-2");
    }
}
