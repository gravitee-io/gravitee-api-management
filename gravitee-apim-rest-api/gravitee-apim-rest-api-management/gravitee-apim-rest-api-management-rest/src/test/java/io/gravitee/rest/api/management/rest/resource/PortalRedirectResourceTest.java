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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Gaurav SHARMA (gaurav.sharma at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalRedirectResourceTest {

    private PortalRedirectResource resource;
    private InstallationAccessQueryService installationAccessQueryService;
    private UserService userService;
    private MembershipService membershipService;
    private MockEnvironment environment;

    @Before
    public void setUp() throws Exception {
        // Prepare SecurityContext with a Gravitee UserDetails principal
        var principal = new UserDetails("UnitTests", "", Collections.emptyList());
        principal.setOrganizationId("DEFAULT");
        principal.setId("UnitTests");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "n/a"));

        // Instantiate resource and mocks
        resource = new PortalRedirectResource();
        installationAccessQueryService = mock(InstallationAccessQueryService.class);
        userService = mock(UserService.class);
        membershipService = mock(MembershipService.class);
        environment = new MockEnvironment();
        environment.setProperty("jwt.secret", "unit-test-secret");
        environment.setProperty("jwt.issuer", "unit-test-issuer");
        environment.setProperty(PortalRedirectResource.PROPERTY_HTTP_API_PORTAL_ENTRYPOINT, "/portal");

        inject(resource, "installationAccessQueryService", installationAccessQueryService);
        inject(resource, "userService", userService);
        inject(resource, "membershipService", membershipService);
        inject(resource, "environment", environment);

        when(installationAccessQueryService.getPortalAPIUrl(anyString())).thenReturn("https://portal.example");
        when(userService.findById(any(), anyString())).thenAnswer(invocation -> {
            var u = new io.gravitee.rest.api.model.UserEntity();
            u.setId("UnitTests");
            u.setEmail("unit@test.local");
            u.setFirstname("Unit");
            u.setLastname("Tests");
            u.setOrganizationId("DEFAULT");
            return u;
        });
        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Set.of());

        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
        GraviteeContext.cleanContext();
    }

    @Test
    public void redirect_should_not_include_version_param_when_null() throws Exception {
        var httpRequest = new MockHttpServletRequest();
        Response response = resource.redirectToPortal(httpRequest, null);

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertTrue(location.startsWith("https://portal.example/environments/DEFAULT/auth/console?"));
        assertTrue(location.contains("token="));
        assertFalse(location.contains("version="));
    }

    @Test
    public void redirect_should_include_version_next() throws Exception {
        var httpRequest = new MockHttpServletRequest();
        Response response = resource.redirectToPortal(httpRequest, "next");

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertTrue(location.startsWith("https://portal.example/environments/DEFAULT/auth/console?"));
        assertTrue(location.contains("version=next&"));
        assertTrue(location.contains("token="));
    }

    @Test
    public void redirect_should_include_version_classic() throws Exception {
        var httpRequest = new MockHttpServletRequest();
        Response response = resource.redirectToPortal(httpRequest, "classic");

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertTrue(location.startsWith("https://portal.example/environments/DEFAULT/auth/console?"));
        assertTrue(location.contains("version=classic&"));
        assertTrue(location.contains("token="));
    }

    @Test
    public void redirect_should_ignore_unknown_version() throws Exception {
        var httpRequest = new MockHttpServletRequest();
        Response response = resource.redirectToPortal(httpRequest, "foobar");

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertTrue(location.startsWith("https://portal.example/environments/DEFAULT/auth/console?"));
        assertTrue(location.contains("token="));
        assertFalse(location.contains("version="));
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field f = type.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ex) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
