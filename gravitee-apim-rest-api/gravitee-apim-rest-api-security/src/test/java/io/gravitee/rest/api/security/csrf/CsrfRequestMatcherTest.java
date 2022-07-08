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
package io.gravitee.rest.api.security.csrf;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CsrfRequestMatcherTest {

    private static final Cookie AUTH_COOKIE = mock(Cookie.class);
    private static final String ORIGIN = "http://gravitee.io";

    static {
        when(AUTH_COOKIE.getName()).thenReturn(TokenAuthenticationFilter.AUTH_COOKIE_NAME);
    }

    private final CsrfRequestMatcher matcher = new CsrfRequestMatcher();

    @Test
    public void shouldMatchWithUnAllowedMethodAndAuthCookieAndReferer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getCookies()).thenReturn(new Cookie[] { AUTH_COOKIE });
        when(request.getHeader(HttpHeaders.REFERER)).thenReturn(ORIGIN);
        assertTrue(matcher.matches(request));
    }

    @Test
    public void shouldMatchWithUnAllowedMethodAndAuthCookieAndOrigin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getCookies()).thenReturn(new Cookie[] { AUTH_COOKIE });
        when(request.getHeader(HttpHeaders.ORIGIN)).thenReturn(ORIGIN);
        assertTrue(matcher.matches(request));
    }

    @Test
    public void shouldNotMatchWithAllowedMethod() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        assertFalse(matcher.matches(request));
    }

    @Test
    public void shouldNotMatchWithOutAuthenticationCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT");
        assertFalse(matcher.matches(request));
    }

    @Test
    public void shouldNotMatchWithOutOriginOrReferer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getCookies()).thenReturn(new Cookie[] { AUTH_COOKIE });
        assertFalse(matcher.matches(request));
    }
}
