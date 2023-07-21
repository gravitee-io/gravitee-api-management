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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at gravitee.io
 * @author GraviteeSource Team
 */
public class PlatformLogsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "platform/logs/";
    }

    @Before
    public void setUp() {
        reset(logsService, applicationService, apiAuthorizationServiceV4);
    }

    @Test
    public void shouldGetPlatformLogsAsAdmin() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_PLATFORM,
                "DEFAULT",
                RolePermissionAction.READ
            )
        )
            .thenReturn(true);
        when(logsService.findPlatform(any(ExecutionContext.class), any(LogQuery.class))).thenReturn(new SearchLogResponse<>(10));
        Response logs = sendRequest();
        assertEquals(OK_200, logs.getStatus());

        verify(applicationService, never()).findIdsByUser(any(ExecutionContext.class), any());
        verify(apiAuthorizationServiceV4, never())
            .findIdsByUser(any(ExecutionContext.class), anyString(), any(ApiQuery.class), anyBoolean());

        verify(logsService)
            .findPlatform(
                any(ExecutionContext.class),
                argThat(query ->
                    Objects.equals(query.getQuery(), "foo:bar") &&
                    query.getPage() == 1 &&
                    query.getSize() == 10 &&
                    query.getFrom() == 0 &&
                    query.getTo() == 1 &&
                    Objects.equals(query.getField(), "@timestamp") &&
                    query.isOrder()
                )
            );
    }

    private Response sendRequest() {
        return envTarget()
            .queryParam("query", "foo:bar")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .queryParam("from", 0)
            .queryParam("to", 1)
            .queryParam("field", "@timestamp")
            .queryParam("order", true)
            .request()
            .get();
    }
}
