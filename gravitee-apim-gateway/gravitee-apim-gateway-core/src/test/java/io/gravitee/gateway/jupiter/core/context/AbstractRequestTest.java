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
package io.gravitee.gateway.jupiter.core.context;

import static io.gravitee.common.http.MediaType.APPLICATION_GRPC;
import static io.gravitee.common.http.MediaType.APPLICATION_OCTET_STREAM;
import static io.gravitee.common.http.MediaType.TEXT_EVENT_STREAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.core.BufferFlow;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.HttpVersion;
import io.vertx.rxjava3.core.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AbstractRequestTest {

    private AbstractRequest cut;

    @BeforeEach
    void setUp() {
        cut = new AbstractRequest() {};
    }

    @Nested
    class OverrideRequestTest {

        @Test
        void should_override_http_method() {
            cut.method(HttpMethod.PUT);

            assertThat(cut.method()).isEqualTo(HttpMethod.PUT);
        }

        @Test
        void should_throw_when_overriding_method_with_null() {
            assertThatThrownBy(() -> cut.method(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Http method should not be null");
        }
    }

    @Nested
    class StreamingRequestTest {

        @Test
        void should_not_be_streaming_by_default() {
            assertThat(cut.isStreaming()).isFalse();
        }

        @Test
        void should_create_buffer_flow_without_calling_is_streaming() {
            AbstractRequest spy = spy(cut);
            BufferFlow bufferFlow = spy.lazyBufferFlow();
            assertThat(bufferFlow).isNotNull();
            verify(spy, never()).isStreaming();
        }
    }
}
