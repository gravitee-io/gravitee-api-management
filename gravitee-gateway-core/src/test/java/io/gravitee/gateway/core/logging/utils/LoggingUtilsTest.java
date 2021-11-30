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
package io.gravitee.gateway.core.logging.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.logging.LoggingContext;
import java.lang.reflect.Field;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggingUtilsTest {

    @Test
    public void shouldLogByDefault() throws Exception {
        resetStatic();
        ExecutionContext executionContext = mock(ExecutionContext.class);
        assertTrue(LoggingUtils.isContentTypeLoggable("application/json", executionContext));
    }

    @Test
    public void shouldNotLogImageByDefault() throws Exception {
        resetStatic();
        ExecutionContext executionContext = mock(ExecutionContext.class);
        assertFalse(LoggingUtils.isContentTypeLoggable("image/png", executionContext));
    }

    @Test
    public void shouldNotLogAudioByDefault() throws Exception {
        resetStatic();
        ExecutionContext executionContext = mock(ExecutionContext.class);
        assertFalse(LoggingUtils.isContentTypeLoggable("audio/ogg", executionContext));
    }

    @Test
    public void shouldNotLogVideoByDefault() throws Exception {
        resetStatic();
        ExecutionContext executionContext = mock(ExecutionContext.class);
        assertFalse(LoggingUtils.isContentTypeLoggable("video/ogg", executionContext));
    }

    @Test
    public void shouldNotLogPDFByDefault() throws Exception {
        resetStatic();
        ExecutionContext executionContext = mock(ExecutionContext.class);
        assertFalse(LoggingUtils.isContentTypeLoggable("application/pdf", executionContext));
    }

    @Test
    public void shouldNotLogCustom() throws Exception {
        resetStatic();
        LoggingContext loggingContext = mock(LoggingContext.class);
        when(loggingContext.getExcludedResponseTypes()).thenReturn("foo/bar");
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(LoggingContext.LOGGING_ATTRIBUTE)).thenReturn(loggingContext);
        assertFalse(LoggingUtils.isContentTypeLoggable("foo/bar", executionContext));
    }

    @Test
    public void shouldLogCustom() throws Exception {
        resetStatic();
        ExecutionContext executionContext = mock(ExecutionContext.class);
        assertTrue(LoggingUtils.isContentTypeLoggable("foo/bar", executionContext));
    }

    private void resetStatic() throws NoSuchFieldException, IllegalAccessException {
        Field pathField = LoggingUtils.class.getDeclaredField("EXCLUDED_CONTENT_TYPES_PATTERN");
        pathField.setAccessible(true);
        pathField.set(null, null);
    }
}
