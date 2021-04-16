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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.portal.rest.model.Token;
import io.gravitee.rest.api.portal.rest.model.Token.TokenTypeEnum;
import java.util.Collections;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "auth";
    }

    @AfterClass
    public static void afterClass() {
        // Clean up Spring security context.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
    }

    @Test
    public void shouldLogout() {
        final Response response = target()
            .path("logout")
            .request()
            .header(HttpHeaders.SET_COOKIE, "Auth-Graviteeio-APIM=Bearer%20xxxxxx;Path=/;HttpOnly")
            .post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.SET_COOKIE));
    }

    @Test
    public void shouldLogin() {
        final UserDetails userDetails = new UserDetails(USER_NAME, "PASSWORD", Collections.emptyList());

        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
        SecurityContextHolder.setContext(securityContext);

        Cookie bearer = new Cookie("FOO", "BAR");
        doReturn(bearer).when(cookieGenerator).generate(any());

        final Response response = target().path("login").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Token token = response.readEntity(Token.class);
        assertNotNull(token);
        assertNotNull(token.getToken());
        assertNotEquals("", token.getToken());
        assertEquals(TokenTypeEnum.BEARER, token.getTokenType());
        //APIPortal: can't test Cookie, since servletResponse is mocked

    }
}
