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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthResourceTokenRenewalTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "auth";
    }

    @AfterAll
    public static void afterClass() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
    }

    @BeforeEach
    public void authenticateUserAsTheTokenFilterWould() {
        final UserDetails userDetails = new UserDetails(USER_NAME, "", Collections.emptyList());
        userDetails.setOrganizationId("DEFAULT");

        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
        SecurityContextHolder.setContext(securityContext);

        doReturn(new Cookie("FOO", "BAR")).when(cookieGenerator).generate(any());
    }

    @Test
    public void shouldNotExchangeAnExistingBearerTokenForANewOne() {
        final Response response = target()
            .path("login")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + UUID.randomUUID())
            .post(null);

        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldNotExchangeAnExistingAuthCookieForANewToken() {
        final Response response = target()
            .path("login")
            .request()
            .cookie("Auth-Graviteeio-APIM", "Bearer%20" + UUID.randomUUID())
            .post(null);

        assertEquals(HttpStatusCode.UNAUTHORIZED_401, response.getStatus());
    }
}
