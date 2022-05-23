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
package io.gravitee.rest.api.security.cookies;

import static org.junit.Assert.*;

import javax.servlet.http.Cookie;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author GraviteeSource Team
 */
public class CookieGeneratorTest {

    @Test
    public void shouldSetDefaultCookieOptions() {
        final Environment environment = new MockEnvironment();
        final CookieGenerator cookieGenerator = new CookieGenerator(environment);
        final Cookie cookie = cookieGenerator.generate("test");
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals("", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertEquals(604800, cookie.getMaxAge());
    }

    @Test
    public void shouldSetCookieOptionsFromEnvironment() {
        final Environment environment = new MockEnvironment()
            .withProperty("jwt.cookie-secure", "false")
            .withProperty("jwt.cookie-path", "/test")
            .withProperty("jwt.cookie-domain", "gravitee.io")
            .withProperty("jwt.expire-after", "1");

        final CookieGenerator cookieGenerator = new CookieGenerator(environment);
        final Cookie cookie = cookieGenerator.generate("test");
        assertEquals("test", cookie.getValue());
        assertFalse(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals("gravitee.io", cookie.getDomain());
        assertEquals("/test", cookie.getPath());
        assertEquals(1, cookie.getMaxAge());
    }

    @Test
    public void shouldRevokeCookie() {
        final Environment environment = new MockEnvironment();

        final CookieGenerator cookieGenerator = new CookieGenerator(environment);
        final Cookie cookie = cookieGenerator.generate(null);
        assertNull(cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
    }
}
