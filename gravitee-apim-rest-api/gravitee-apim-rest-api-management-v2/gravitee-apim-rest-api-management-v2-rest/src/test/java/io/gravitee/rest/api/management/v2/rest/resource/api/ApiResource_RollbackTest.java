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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.use_case.RollbackApiUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.ApiRollback;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiResource_RollbackTest extends ApiResourceTest {

    private static final String EVENT_ID = "event-id";

    @Inject
    private RollbackApiUseCase rollbackApiUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_rollback";
    }

    @BeforeEach
    public void setUp() {
        super.setUp();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Test
    void should_return_400_if_not_eventId() {
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget().request().post(Entity.json(aRollbackPayload(null)));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(error.getMessage()).isEqualTo("Event ID is required");
    }

    @Test
    void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        final Response response = rootTarget().request().post(Entity.json(aRollbackPayload(EVENT_ID)));
        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);

        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(FORBIDDEN_403);
        assertThat(error.getMessage()).isEqualTo("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_rollback_api() {
        final Response response = rootTarget().request().post(Entity.json(aRollbackPayload(EVENT_ID)));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);

        var captor = ArgumentCaptor.forClass(RollbackApiUseCase.Input.class);
        verify(rollbackApiUseCase).execute(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var input = captor.getValue();
            soft.assertThat(input.eventId()).isEqualTo(EVENT_ID);
            soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
        });
    }

    private ApiRollback aRollbackPayload(String eventId) {
        return new ApiRollback().eventId(eventId);
    }
}
