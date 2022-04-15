/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApplicationMembersOrganizationAdminTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "application-id";

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.isUserInRole(AbstractResource.ORGANIZATION_ADMIN)).thenReturn(true);
            requestContext.setSecurityContext(securityContext);
        }
    }

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @Test
    public void shouldGetAllPermissions() {
        final Map<String, char[]> expectedPermissions = Map.ofEntries(
            Pair.of(ApplicationPermission.DEFINITION.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.NOTIFICATION.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.LOG.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.ANALYTICS.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.SUBSCRIPTION.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.ALERT.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.MEMBER.name(), "CRUD".toCharArray()),
            Pair.of(ApplicationPermission.METADATA.name(), "CRUD".toCharArray())
        );

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(new ApplicationEntity());

        Response response = envTarget(APPLICATION_ID).path("members/permissions").request().get();
        Map<String, char[]> permissions = response.readEntity(new GenericType<>() {});

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        expectedPermissions.forEach((permission, acl) -> assertArrayEquals(acl, permissions.get(permission)));
    }
}
