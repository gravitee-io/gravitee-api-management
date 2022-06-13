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
package io.gravitee.gateway.handlers.api.processor.logging;

import io.gravitee.definition.model.Logging;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiLoggableRequestProcessorTest {

    @Mock
    private Logging logging;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBeDisabled_emptyValue() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);

        Assert.assertEquals(-1, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeDisabled_negativeValue() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("-1");

        Assert.assertEquals(-1, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_defaultToMB() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1");

        Assert.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_MB() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1MB");
        Assert.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1M");
        Assert.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1mb");
        Assert.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1m");
        Assert.assertEquals(1024 * 1024, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_KB() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1KB");
        Assert.assertEquals(1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1K");
        Assert.assertEquals(1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1kb");
        Assert.assertEquals(1024, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1k");
        Assert.assertEquals(1024, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeEnabled_B() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("1B");
        Assert.assertEquals(1, processor.getMaxSizeLogMessage());

        processor.setMaxSizeLogMessage("1b");
        Assert.assertEquals(1, processor.getMaxSizeLogMessage());
    }

    @Test
    public void shouldBeDisabled_invalidValue() {
        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(logging);
        processor.setMaxSizeLogMessage("d12B");
        Assert.assertEquals(-1, processor.getMaxSizeLogMessage());
    }
}
