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
package io.gravitee.rest.api.portal.rest.resource.auth;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Gaurav SHARMA (gaurav.sharma at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsoleAuthenticationResourceTest {

    private ConsoleAuthenticationResource resource;
    private InstallationAccessQueryService installationAccessQueryService;
    private AuthoritiesProvider authoritiesProvider;
    private UserService userService;
    private MockEnvironment environment;

    private static final String ORG_ID = "DEFAULT";
    private static final String USER_ID = "UnitTests";

    @Before
    public void setUp() throws Exception {
        resource = spy(new ConsoleAuthenticationResource());
        installationAccessQueryService = mock(InstallationAccessQueryService.class);
        authoritiesProvider = mock(AuthoritiesProvider.class);
        userService = mock(UserService.class);
        environment = new MockEnvironment();
        environment.setProperty("jwt.secret", "unit-test-secret");

        inject(resource, "installationAccessQueryService", installationAccessQueryService);
        inject(resource, "authoritiesProvider", authoritiesProvider);
        inject(resource, "userService", userService);
        inject(resource, "environment", environment);

        // Avoid executing parent connectUser logic (cookie/session), not relevant for this test
        doNothing().when(resource).connectUser(anyString(), any());

        when(installationAccessQueryService.getPortalUrl(anyString())).thenReturn("https://portal.example");
        when(authoritiesProvider.retrieveAuthorities(anyString(), anyString(), anyString())).thenReturn(Set.<GrantedAuthority>of());
        when(userService.findById(any(), anyString())).thenAnswer(
            (Answer<UserEntity>) invocation -> {
                UserEntity u = new UserEntity();
                u.setId(USER_ID);
                u.setOrganizationId(ORG_ID);
                u.setEmail("unit@test.local");
                return u;
            }
        );

        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void redirect_should_not_append_version_path_when_null() throws Exception {
        String token = buildJwtToken();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        Response response = resource.redirectTo(null, token, httpResponse);

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertEquals("https://portal.example", location);
    }

    @Test
    public void redirect_should_append_next_path() throws Exception {
        String token = buildJwtToken();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        Response response = resource.redirectTo("next", token, httpResponse);

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertEquals("https://portal.example/next", location);
    }

    @Test
    public void redirect_should_append_classic_path() throws Exception {
        String token = buildJwtToken();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        Response response = resource.redirectTo("classic", token, httpResponse);

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertEquals("https://portal.example/classic", location);
    }

    @Test
    public void redirect_should_ignore_unknown_version() throws Exception {
        String token = buildJwtToken();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        Response response = resource.redirectTo("foobar", token, httpResponse);

        assertEquals(307, response.getStatus());
        String location = ((URI) response.getLocation()).toString();
        assertEquals("https://portal.example", location);
    }

    private String buildJwtToken() {
        Algorithm alg = Algorithm.HMAC256(environment.getProperty("jwt.secret"));
        return JWT.create().withSubject(USER_ID).withClaim(JWTHelper.Claims.ORG, ORG_ID).sign(alg);
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
