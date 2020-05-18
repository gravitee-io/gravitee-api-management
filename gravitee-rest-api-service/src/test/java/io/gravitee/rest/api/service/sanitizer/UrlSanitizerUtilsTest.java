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
package io.gravitee.rest.api.service.sanitizer;

import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.UrlForbiddenException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UrlSanitizerUtilsTest {

    @Test
    public void checkAllowed_allowPrivate() {

        UrlSanitizerUtils.checkAllowed("http://localhost:8080", Collections.emptyList(), true);
    }

    @Test(expected = UrlForbiddenException.class)
    public void checkAllowed_disallowPrivate() {

        UrlSanitizerUtils.checkAllowed("http://localhost:8080", Collections.emptyList(), false);
    }

    @Test
    public void checkAllowed_whitelisted() {

        UrlSanitizerUtils.checkAllowed("http://localhost:8080/test", Collections.singletonList("http://localhost:8080"), false);
    }

    @Test
    public void checkAllowed_public() {

        UrlSanitizerUtils.checkAllowed("https://demo.gravitee.io", Collections.emptyList(), false);
    }

    @Test(expected = InvalidDataException.class)
    public void checkAllowed_invalidUrl() {

        UrlSanitizerUtils.checkAllowed("https://invalid-url.not-exist" + RandomStringUtils.random(5), Collections.emptyList(), false);
    }

    @Test(expected = UrlForbiddenException.class)
    public void checkAllowed_notWhitelisted() {

        UrlSanitizerUtils.checkAllowed("https://demo.gravitee.io", Collections.singletonList("http://localhost:8080"), false);
    }

    @Test
    public void isPrivate() {

        String[] privateUrls = new String[]{
                "http://localhost:8080",
                "http://127.0.0.1",
                "http://192.168.0.1",
                "http://[::1]:8080",
                "http://[0:0:0:0:0:0:0:1]:8080",
                "http://10.0.2.3:8080",
                "http://169.254.1.2",
                "https://169.254.1.2:443",
                "http://172.31.1.2:443",
                "http://[fd8a:c424:312e:b006:cec8:ecf2:6cde:3c45]",
                "http://[fd8a:c424:312e:b006:cec8:ecf2:6cde:3c45]:8080",
                "http://[fc8a:c424:312e:b006:cec8:ecf2:6cde:3c45]",
                "http://[fe8a:c424:312e:b006:cec8:ecf2:6cde:3c45]",
                "http://[ff8a:c424:312e:b006:cec8:ecf2:6cde:3c45]"
        };

        for (String url : privateUrls) {
            assertTrue("Url [" + url + "] should be considered private", UrlSanitizerUtils.isPrivate(url));
        }
    }

    @Test
    public void isNotPrivate() {

        assertFalse("Url should not be considered private", UrlSanitizerUtils.isPrivate("https://demo.gravitee.io/"));
    }
}