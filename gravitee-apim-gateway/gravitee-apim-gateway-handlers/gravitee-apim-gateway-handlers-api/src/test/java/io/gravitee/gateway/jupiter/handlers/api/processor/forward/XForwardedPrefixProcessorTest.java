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
package io.gravitee.gateway.jupiter.handlers.api.processor.forward;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardedPrefixProcessorTest extends AbstractProcessorTest {

    private static final String CONTEXT_PATH = "/context_path";
    private XForwardedPrefixProcessor xForwardedPrefixProcessor;

    @BeforeEach
    public void beforeEach() {
        xForwardedPrefixProcessor = XForwardedPrefixProcessor.instance();
        when(mockRequest.contextPath()).thenReturn(CONTEXT_PATH);
    }

    @Test
    public void shouldAddXForwardedPrefixHeaderWhenNoInHeader() {
        xForwardedPrefixProcessor.execute(ctx).test().assertResult();

        assertThat(spyRequestHeaders.getAll(HttpHeaderNames.X_FORWARDED_PREFIX).size()).isEqualTo(1);
        assertThat(spyRequestHeaders.get(HttpHeaderNames.X_FORWARDED_PREFIX)).isEqualTo(CONTEXT_PATH);
    }

    @Test
    public void shouldOverrideXForwardedPrefixHeaderWhenAlreadyInHeader() {
        spyRequestHeaders.add(HttpHeaderNames.X_FORWARDED_PREFIX, "randomPrefix");
        xForwardedPrefixProcessor.execute(ctx).test().assertResult();

        assertThat(spyRequestHeaders.getAll(HttpHeaderNames.X_FORWARDED_PREFIX).size()).isEqualTo(1);
        assertThat(spyRequestHeaders.get(HttpHeaderNames.X_FORWARDED_PREFIX)).isEqualTo(CONTEXT_PATH);
    }
}
