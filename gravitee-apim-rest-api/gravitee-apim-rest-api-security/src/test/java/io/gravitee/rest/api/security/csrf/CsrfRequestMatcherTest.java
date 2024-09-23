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
package io.gravitee.rest.api.security.csrf;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class CsrfRequestMatcherTest {

    private CsrfRequestMatcher csrfRequestMatcher;

    @BeforeEach
    void setUp() {
        csrfRequestMatcher = new CsrfRequestMatcher();
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "DELETE", "PATCH" })
    void should_match_with_request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/");
        request.addHeader("Origin", "http://localhost:8083");
        request.addHeader("Referer", "http://localhost:8083");
        request.setCookies(new Cookie(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "cookie"));

        assertTrue(csrfRequestMatcher.matches(request));
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD", "TRACE", "OPTIONS" })
    void should_not_match_with_request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/");
        request.addHeader("Origin", "http://localhost:8083");
        request.addHeader("Referer", "http://localhost:8083");
        request.setCookies(new Cookie(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "cookie"));

        assertFalse(csrfRequestMatcher.matches(request));
    }

    @Test
    void should_not_match_with_POST_request_and_no_referer_and_no_origin_and_no_cookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");

        assertFalse(csrfRequestMatcher.matches(request));
    }

    @Test
    void should_match_with_POST_request_and_referer() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
        request.addHeader("Referer", "http://localhost:8083");

        assertTrue(csrfRequestMatcher.matches(request));
    }

    @Test
    void should_match_with_POST_request_and_cookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
        request.setCookies(new Cookie(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "cookie"));

        assertTrue(csrfRequestMatcher.matches(request));
    }

    @Test
    void should_match_with_POST_request_and_origin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
        request.addHeader("Origin", "http://localhost:8083");

        assertTrue(csrfRequestMatcher.matches(request));
    }

    @Test
    void should_not_match_with_POST_request_and_login_path() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/organizations/1/user/login");
        request.setPathInfo("/organizations/1/user/login");
        request.addHeader("Origin", "http://localhost:8083");

        assertFalse(csrfRequestMatcher.matches(request));
    }

    @Test
    void should_match_with_POST_request_and_not_login_path() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/organizations/1/user/info");
        request.setPathInfo("/organizations/1/user/info");
        request.addHeader("Origin", "http://localhost:8083");

        assertTrue(csrfRequestMatcher.matches(request));
    }
}
