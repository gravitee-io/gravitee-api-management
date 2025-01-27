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
package io.gravitee.gateway.core.logging.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.logging.LoggingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LoggingUtilsTest {

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private LoggingContext loggingContext;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(LoggingUtils.class, "EXCLUDED_CONTENT_TYPES_PATTERN", null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/json", "foo/bar" })
    void shouldLogImageByDefault(String input) {
        assertThat(LoggingUtils.isContentTypeLoggable(input, executionContext)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/png", "audio/ogg", "video/ogg", "application/pdf" })
    void shouldNotLogImageByDefault(String input) {
        assertThat(LoggingUtils.isContentTypeLoggable(input, executionContext)).isFalse();
    }

    @Test
    void shouldNotLogCustom() {
        when(loggingContext.getExcludedResponseTypes()).thenReturn("foo/bar");
        when(executionContext.getAttribute(LoggingContext.LOGGING_ATTRIBUTE)).thenReturn(loggingContext);
        assertThat(LoggingUtils.isContentTypeLoggable("foo/bar", executionContext)).isFalse();
    }

    @Test
    void shouldAppendBufferWhenUnlimitedMaxSize() {
        final Buffer buffer = Buffer.buffer("Existing buffer ");
        final Buffer chunk = Buffer.buffer("Chunk to append");

        LoggingUtils.appendBuffer(buffer, chunk, -1);

        assertThat(buffer).hasToString("Existing buffer Chunk to append");
    }

    @Test
    void shouldAppendBufferWhenMaxSizeNotExceed() {
        final Buffer buffer = Buffer.buffer("Existing buffer ");
        final Buffer chunk = Buffer.buffer("Chunk to append");

        LoggingUtils.appendBuffer(buffer, chunk, 50);

        assertThat(buffer).hasToString("Existing buffer Chunk to append");
    }

    @Test
    void shouldAppendPartialBufferWhenMaxSizeExceed() {
        final Buffer buffer = Buffer.buffer("Existing buffer ");
        final Buffer chunk = Buffer.buffer("Chunk to append");

        LoggingUtils.appendBuffer(buffer, chunk, 21);

        assertThat(buffer).hasToString("Existing buffer Chunk");
    }

    @Test
    void shouldNotAppendBufferWhenMaxSizeExceed() {
        final Buffer buffer = Buffer.buffer("Existing buffer ");
        final Buffer chunk = Buffer.buffer("Chunk to append");

        LoggingUtils.appendBuffer(buffer, chunk, 1);

        assertThat(buffer).hasToString("Existing buffer ");
    }

    @Test
    void shouldGetLoggingContextFromApi() {
        final Api api = new Api();
        final Proxy proxy = new Proxy();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        proxy.setLogging(logging);
        api.setProxy(proxy);

        final LoggingContext loggingContext = LoggingUtils.getLoggingContext(api);
        assertThat(loggingContext).isNotNull();
        assertThat(loggingContext.clientMode()).isTrue();
        assertThat(loggingContext.proxyMode()).isTrue();
    }

    @Test
    void shouldGetNullLoggingContextFromApiWhenNullLoggingConfiguration() {
        final Api api = new Api();
        final Proxy proxy = new Proxy();
        proxy.setLogging(null);
        api.setProxy(proxy);

        assertThat(LoggingUtils.getLoggingContext(api)).isNull();
    }

    @Test
    void shouldGetNullLoggingContextFromApiWhenNoneLoggingConfiguration() {
        final Api api = new Api();
        final Proxy proxy = new Proxy();
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        proxy.setLogging(logging);
        api.setProxy(proxy);

        assertThat(LoggingUtils.getLoggingContext(api)).isNull();
    }

    @Test
    void shouldReturnInfiniteMaxSizeLogMessageWhenLoggingContextIsNull() {
        when(executionContext.getAttribute(LoggingContext.LOGGING_ATTRIBUTE)).thenReturn(null);
        assertThat(LoggingUtils.getMaxSizeLogMessage(executionContext)).isEqualTo(-1);
    }

    @Test
    void shouldReturnConfigureMaxSizeLogMessageWhenLoggingContextIsNull() {
        final int maxSizeLogMessage = 10;
        when(loggingContext.getMaxSizeLogMessage()).thenReturn(maxSizeLogMessage);
        when(executionContext.getAttribute(LoggingContext.LOGGING_ATTRIBUTE)).thenReturn(loggingContext);

        assertThat(LoggingUtils.getMaxSizeLogMessage(executionContext)).isEqualTo(maxSizeLogMessage);
    }

    @Test
    void shouldReturnInfiniteMaxSizeLogMessageWhenExceptionOccurs() {
        when(executionContext.getAttribute(LoggingContext.LOGGING_ATTRIBUTE)).thenThrow(new RuntimeException("Mock Exception"));
        assertThat(LoggingUtils.getMaxSizeLogMessage(executionContext)).isEqualTo(-1);
    }
}
