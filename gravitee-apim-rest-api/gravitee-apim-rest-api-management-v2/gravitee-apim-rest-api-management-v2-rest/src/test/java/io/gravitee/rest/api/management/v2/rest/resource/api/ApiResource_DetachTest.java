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
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.AuditCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_DetachTest extends ApiResourceTest {

    @Autowired
    private AuditCrudServiceInMemory auditCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_detach";
    }

    @Override
    @BeforeEach
    public void init() throws TechnicalException {
        super.init();
        apiCrudService.reset();
        auditCrudService.reset();
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        var response = rootTarget().request().post(Entity.json(""));

        assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    public void should_return_404_if_api_not_found() {
        var response = rootTarget().request().post(Entity.json(""));

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    public void should_detach_api_with_kubernetes_origin() {
        var kubernetesApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(API)
            .environmentId(ENVIRONMENT)
            .originContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED))
            .build();

        apiCrudService.initWith(List.of(kubernetesApi));

        var response = rootTarget().request().post(Entity.json(""));

        assertThat(response).hasStatus(OK_200);

        var updatedApi = apiCrudService.get(API);

        assertThat(updatedApi.getOriginContext()).isInstanceOf(OriginContext.Management.class);

        var auditLogs = auditCrudService.storage();
        assertThat(auditLogs).hasSize(1);

        var auditLog = auditLogs.getFirst();
        assertThat(auditLog.getReferenceId()).isEqualTo(API);
        assertThat(auditLog.getReferenceType()).isEqualTo(AuditEntity.AuditReferenceType.API);
        assertThat(auditLog.getEvent()).isEqualTo("AUTOMATION_DETACHED");
    }

    @Test
    public void should_return_400_when_api_has_management_origin() {
        var managementApi = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(API)
            .environmentId(ENVIRONMENT)
            .originContext(new OriginContext.Management())
            .build();

        apiCrudService.initWith(List.of(managementApi));

        var response = rootTarget().request().post(Entity.json(""));

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasHttpStatus(BAD_REQUEST_400)
            .hasMessage("The API must be managed by an automation tool in order to be detached.");

        var unchangedApi = apiCrudService.get(API);

        assertThat(unchangedApi.getOriginContext()).isInstanceOf(OriginContext.Management.class);
    }

    @Test
    public void should_return_400_when_api_has_null_origin() {
        var nullOriginApi = ApiFixtures.aProxyApiV4().toBuilder().id(API).environmentId(ENVIRONMENT).originContext(null).build();

        apiCrudService.initWith(List.of(nullOriginApi));

        var response = rootTarget().request().post(Entity.json(""));

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasHttpStatus(BAD_REQUEST_400)
            .hasMessage("The API must be managed by an automation tool in order to be detached.");

        var unchangedApi = apiCrudService.get(API);

        assertThat(unchangedApi.getOriginContext()).isNull();
    }
}
