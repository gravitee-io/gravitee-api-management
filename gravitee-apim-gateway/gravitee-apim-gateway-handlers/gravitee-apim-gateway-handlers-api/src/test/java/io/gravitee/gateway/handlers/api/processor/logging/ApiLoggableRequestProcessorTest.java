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
package io.gravitee.gateway.handlers.api.processor.logging;

import io.gravitee.definition.model.Logging;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApiLoggableRequestProcessorTest {

    @Mock
    private Logging logging;

    @Test
    public void shouldBeDisabled_emptyValue() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);

        Assertions.assertEquals(-1, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeDisabled_negativeValue() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("-1");

        Assertions.assertEquals(-1, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_defaultToMB() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1");

        Assertions.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_MB() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1MB");
        Assertions.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1M");
        Assertions.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1mb");
        Assertions.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1m");
        Assertions.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_KB() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1KB");
        Assertions.assertEquals(1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1K");
        Assertions.assertEquals(1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1kb");
        Assertions.assertEquals(1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1k");
        Assertions.assertEquals(1024, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_B() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1B");
        Assertions.assertEquals(1, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1b");
        Assertions.assertEquals(1, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeDisabled_invalidValue() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("d12B");
        Assertions.assertEquals(-1, processor.getMaxSizeLogMessage());
    }
}
