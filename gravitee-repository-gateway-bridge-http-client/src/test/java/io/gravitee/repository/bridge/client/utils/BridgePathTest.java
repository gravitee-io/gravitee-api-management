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
package io.gravitee.repository.bridge.client.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BridgePathTest {
    @Mock
    Environment environment;

    @Test
    public void shouldGetDefaultWithoutTrailingSlash() throws Exception {
        resetStatic();
        when(environment.getProperty("management.http.url", String.class)).thenReturn("http://127.0.0.1");
        when(environment.getProperty("management.http.strippedUrl", Boolean.class, true)).thenReturn(true);
        String path = BridgePath.get(environment);
        assertEquals("/_bridge", path);
    }

    @Test
    public void shouldGetDefaultWithTrailingSlash() throws Exception {
        resetStatic();
        when(environment.getProperty("management.http.url", String.class)).thenReturn("http://127.0.0.1/");
        when(environment.getProperty("management.http.strippedUrl", Boolean.class, true)).thenReturn(true);
        String path = BridgePath.get(environment);
        assertEquals("/_bridge", path);
    }

    @Test
    public void shouldGetNotStrippedWithoutTrailingSlash() throws Exception {
        resetStatic();
        when(environment.getProperty("management.http.url", String.class)).thenReturn("http://127.0.0.1");
        when(environment.getProperty("management.http.strippedUrl", Boolean.class, true)).thenReturn(false);
        String path = BridgePath.get(environment);
        assertEquals("", path);
    }

    @Test
    public void shouldGetNotStrippedWithTrailingSlash() throws Exception {
        resetStatic();
        when(environment.getProperty("management.http.url", String.class)).thenReturn("http://127.0.0.1/");
        when(environment.getProperty("management.http.strippedUrl", Boolean.class, true)).thenReturn(false);
        String path = BridgePath.get(environment);
        assertEquals("", path);
    }

    @Test
    public void shouldGetNotStrippedPathWithoutTrailingSlash() throws Exception {
        resetStatic();
        when(environment.getProperty("management.http.url", String.class)).thenReturn("http://127.0.0.1/foo");
        when(environment.getProperty("management.http.strippedUrl", Boolean.class, true)).thenReturn(false);
        String path = BridgePath.get(environment);
        assertEquals("/foo", path);
    }

    @Test
    public void shouldGetNotStrippedPathWithTrailingSlash() throws Exception {
        resetStatic();
        when(environment.getProperty("management.http.url", String.class)).thenReturn("http://127.0.0.1/foo/");
        when(environment.getProperty("management.http.strippedUrl", Boolean.class, true)).thenReturn(false);
        String path = BridgePath.get(environment);
        assertEquals("/foo", path);
    }

    private void resetStatic() throws NoSuchFieldException, IllegalAccessException {
        Field pathField = BridgePath.class.getDeclaredField("path");
        pathField.setAccessible( true );
        pathField.set(null, null);
    }
}
