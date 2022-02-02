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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.impl.ReCaptchaServiceImpl;
import io.vertx.core.buffer.Buffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.reflections.ReflectionUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ReCaptchaServiceImplTest {

    @InjectMocks
    private ReCaptchaServiceImpl reCaptchaService = new ReCaptchaServiceImpl();

    @Mock
    private HttpClientService httpClientService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() {
        ReflectionTestUtils.setField(reCaptchaService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(reCaptchaService, "serviceUrl", "https://verif");
    }

    @Test
    public void isValidWhenDisabled() {
        ReflectionTestUtils.setField(reCaptchaService, "enabled", false);

        assertTrue(reCaptchaService.isValid(null));
        assertTrue(reCaptchaService.isValid(""));
        assertTrue(reCaptchaService.isValid("any"));
    }

    @Test
    public void isNotValidIfNoToken() {
        ReflectionTestUtils.setField(reCaptchaService, "enabled", true);

        assertFalse(reCaptchaService.isValid(null));
        assertFalse(reCaptchaService.isValid(""));
    }

    @Test
    public void isValid() {
        ReflectionTestUtils.setField(reCaptchaService, "minScore", 0.5d);
        ReflectionTestUtils.setField(reCaptchaService, "enabled", true);

        when(httpClientService.request(any(), any(), any(), any(), any()))
            .thenReturn(Buffer.buffer("{ \"success\": true, \"score\": 1.0 }"));

        assertTrue(reCaptchaService.isValid("any"));
    }

    @Test
    public void isValidAboveMinScore() {
        ReflectionTestUtils.setField(reCaptchaService, "minScore", 0.5d);
        ReflectionTestUtils.setField(reCaptchaService, "enabled", true);

        when(httpClientService.request(any(), any(), any(), any(), any()))
            .thenReturn(Buffer.buffer("{ \"success\": true, \"score\": 1.0 }"));

        assertTrue(reCaptchaService.isValid("any"));
    }

    @Test
    public void isNotValidBelowMinScore() {
        ReflectionTestUtils.setField(reCaptchaService, "minScore", 0.5d);
        ReflectionTestUtils.setField(reCaptchaService, "enabled", true);

        when(httpClientService.request(any(), any(), any(), any(), any()))
            .thenReturn(Buffer.buffer("{ \"success\": true, \"score\": 0.4 }"));

        assertFalse(reCaptchaService.isValid("any"));
    }

    @Test
    public void isNotValidNoSuccess() {
        ReflectionTestUtils.setField(reCaptchaService, "minScore", 0.5d);
        ReflectionTestUtils.setField(reCaptchaService, "enabled", true);

        when(httpClientService.request(any(), any(), any(), any(), any())).thenReturn(Buffer.buffer("{ \"success\": false }"));

        assertFalse(reCaptchaService.isValid("any"));
    }

    @Test
    public void isEnabled() {
        ReflectionTestUtils.setField(reCaptchaService, "enabled", true);

        assertTrue(reCaptchaService.isEnabled());
    }

    @Test
    public void isNotEnabled() {
        ReflectionTestUtils.setField(reCaptchaService, "enabled", false);
        assertFalse(reCaptchaService.isEnabled());
    }

    @Test
    public void getSiteKeyNullByDefault() {
        assertNull(reCaptchaService.getSiteKey());
    }

    @Test
    public void getSiteKey() {
        ReflectionTestUtils.setField(reCaptchaService, "siteKey", "test");

        assertEquals("test", reCaptchaService.getSiteKey());
    }
}
