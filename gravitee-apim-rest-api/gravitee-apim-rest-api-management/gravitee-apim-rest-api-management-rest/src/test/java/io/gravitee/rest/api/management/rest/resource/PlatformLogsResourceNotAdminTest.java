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
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_LOG;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at gravitee.io
 * @author GraviteeSource Team
 */
public class PlatformLogsResourceNotAdminTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "platform/logs/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Before
    public void init() {
        reset(apiAuthorizationServiceV4);
        reset(applicationService);
        reset(logsService);
    }

    @Test
    public void shouldGetPlatformLogsAsNonAdmin() {
        when(logsService.findPlatform(any(ExecutionContext.class), any(LogQuery.class))).thenReturn(new SearchLogResponse<>(10));
        when(
            applicationService.findIdsByUserAndPermission(
                any(ExecutionContext.class),
                eq(USER_NAME),
                isNull(),
                eq(APPLICATION_LOG),
                eq(READ)
            )
        ).thenReturn(Set.of("app1"));
        when(apiAuthorizationServiceV4.findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true))).thenReturn(
            Set.of("api1")
        );
        Response logs = sendRequest();
        assertEquals(OK_200, logs.getStatus());

        verify(applicationService).findIdsByUserAndPermission(
            any(ExecutionContext.class),
            eq(USER_NAME),
            isNull(),
            eq(APPLICATION_LOG),
            eq(READ)
        );
        verify(apiAuthorizationServiceV4).findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true));
        verify(logsService).findPlatform(
            any(ExecutionContext.class),
            argThat(
                query ->
                    Objects.equals(query.getQuery(), "foo:bar") &&
                    query.getTerms().size() == 2 &&
                    query.getTerms().containsKey("application") &&
                    query.getTerms().get("application").equals(Set.of("app1")) &&
                    query.getTerms().containsKey("api") &&
                    query.getTerms().get("api").equals(Set.of("api1")) &&
                    query.getPage() == 1 &&
                    query.getSize() == 10 &&
                    query.getFrom() == 0 &&
                    query.getTo() == 1 &&
                    Objects.equals(query.getField(), "@timestamp") &&
                    query.isOrder()
            )
        );
    }

    @Test
    public void shouldGetPlatformLogsAsNonAdminFilterOnlyApp() {
        when(logsService.findPlatform(any(ExecutionContext.class), any(LogQuery.class))).thenReturn(new SearchLogResponse<>(10));
        when(
            applicationService.findIdsByUserAndPermission(
                any(ExecutionContext.class),
                eq(USER_NAME),
                isNull(),
                eq(APPLICATION_LOG),
                eq(READ)
            )
        ).thenReturn(Set.of("app1"));
        when(apiAuthorizationServiceV4.findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true))).thenReturn(
            emptySet()
        );
        Response logs = sendRequest();
        assertEquals(OK_200, logs.getStatus());

        verify(applicationService).findIdsByUserAndPermission(
            any(ExecutionContext.class),
            eq(USER_NAME),
            isNull(),
            eq(APPLICATION_LOG),
            eq(READ)
        );
        verify(apiAuthorizationServiceV4).findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true));
        verify(logsService).findPlatform(
            any(ExecutionContext.class),
            argThat(
                query ->
                    Objects.equals(query.getQuery(), "foo:bar") &&
                    query.getTerms().size() == 1 &&
                    query.getTerms().containsKey("application") &&
                    query.getTerms().get("application").equals(Set.of("app1")) &&
                    query.getPage() == 1 &&
                    query.getSize() == 10 &&
                    query.getFrom() == 0 &&
                    query.getTo() == 1 &&
                    Objects.equals(query.getField(), "@timestamp") &&
                    query.isOrder()
            )
        );
    }

    @Test
    public void shouldGetPlatformLogsAsNonAdminFilterOnlyAPI() {
        when(logsService.findPlatform(any(ExecutionContext.class), any(LogQuery.class))).thenReturn(new SearchLogResponse<>(10));
        when(
            applicationService.findIdsByUserAndPermission(
                any(ExecutionContext.class),
                eq(USER_NAME),
                isNull(),
                eq(APPLICATION_LOG),
                eq(READ)
            )
        ).thenReturn(emptySet());
        when(apiAuthorizationServiceV4.findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true))).thenReturn(
            Set.of("api1")
        );

        Response logs = sendRequest();
        assertEquals(OK_200, logs.getStatus());

        verify(applicationService).findIdsByUserAndPermission(
            any(ExecutionContext.class),
            eq(USER_NAME),
            isNull(),
            eq(APPLICATION_LOG),
            eq(READ)
        );
        verify(apiAuthorizationServiceV4).findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true));
        verify(logsService).findPlatform(
            any(ExecutionContext.class),
            argThat(
                query ->
                    Objects.equals(query.getQuery(), "foo:bar") &&
                    query.getTerms().size() == 1 &&
                    query.getTerms().containsKey("api") &&
                    query.getTerms().get("api").equals(Set.of("api1")) &&
                    query.getPage() == 1 &&
                    query.getSize() == 10 &&
                    query.getFrom() == 0 &&
                    query.getTo() == 1 &&
                    Objects.equals(query.getField(), "@timestamp") &&
                    query.isOrder()
            )
        );
    }

    @Test
    public void shouldGetNoLogsAsNonAdminWithNoAPINoApp() {
        when(
            applicationService.findIdsByUserAndPermission(
                any(ExecutionContext.class),
                eq(USER_NAME),
                isNull(),
                eq(APPLICATION_LOG),
                eq(READ)
            )
        ).thenReturn(emptySet());
        when(apiAuthorizationServiceV4.findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true))).thenReturn(
            emptySet()
        );
        Response logs = sendRequest();
        assertEquals(OK_200, logs.getStatus());

        JsonNode jsonNode = logs.readEntity(JsonNode.class);
        assertEquals(0, jsonNode.get("total").intValue());
        assertNull(jsonNode.get("logs"));
        verify(applicationService).findIdsByUserAndPermission(
            any(ExecutionContext.class),
            eq(USER_NAME),
            isNull(),
            eq(APPLICATION_LOG),
            eq(READ)
        );
        verify(apiAuthorizationServiceV4).findIdsByUser(any(ExecutionContext.class), eq(USER_NAME), isNull(), eq(true));
        verify(logsService, never()).findPlatform(any(ExecutionContext.class), any(LogQuery.class));
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
