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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.portal.rest.model.Token;
import io.gravitee.rest.api.portal.rest.model.Token.TokenTypeEnum;
import jakarta.servlet.http.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Collections;
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
    public void shouldLogin() throws NoSuchAlgorithmException, IOException, SignatureException, InvalidKeyException {
        final UserDetails userDetails = new UserDetails(USER_NAME, "PASSWORD", Collections.emptyList());
        userDetails.setOrganizationId("DEFAULT");

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

        verifyJwtToken(response);
        //APIPortal: can't test Cookie, since servletResponse is mocked
    }

    private void verifyJwtToken(Response response)
        throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException, JWTVerificationException {
        Token responseToken = response.readEntity(Token.class);
        assertEquals("BEARER", responseToken.getTokenType().name());

        String token = responseToken.getToken();

        Algorithm algorithm = Algorithm.HMAC256("myJWT4Gr4v1t33_S3cr3t");
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();

        DecodedJWT jwt = jwtVerifier.verify(token);

        assertEquals(jwt.getSubject(), USER_NAME);

        assertNull(jwt.getClaim("firstname").asString());
        assertEquals("gravitee-management-auth", jwt.getClaim("iss").asString());
        assertEquals(USER_NAME, jwt.getClaim("sub").asString());
        assertNull(jwt.getClaim("email").asString());
        assertNull(jwt.getClaim("lastname").asString());
        assertEquals("DEFAULT", jwt.getClaim("org").asString());
    }
}
