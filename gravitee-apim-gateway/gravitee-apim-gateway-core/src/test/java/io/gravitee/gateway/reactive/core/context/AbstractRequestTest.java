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
package io.gravitee.gateway.reactive.core.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.reactive.core.BufferFlow;
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

        @Test
        void should_init_context_path_and_path_info() {
            String initialPath = "/contextPath/pathInfo";

            cut.path = initialPath;
            assertThat(cut.path()).isEqualTo(initialPath);
            assertThat(cut.contextPath()).isNull();
            assertThat(cut.pathInfo()).isNull();

            String newContextPath = "/contextPath/";

            cut.contextPath(newContextPath);
            assertThat(cut.path()).isEqualTo(initialPath);
            assertThat(cut.contextPath()).isEqualTo(newContextPath);
            assertThat(cut.pathInfo()).isEqualTo("/pathInfo");
        }

        @Test
        void should_override_context_path_and_path_and_path_info() {
            String initialPath = "/eventId-contextPath/pathInfo";
            String initialContextPath = "/eventId-contextPath/";

            cut.path = initialPath;
            cut.contextPath = initialContextPath;
            assertThat(cut.path()).isEqualTo(initialPath);
            assertThat(cut.contextPath()).isEqualTo(initialContextPath);
            assertThat(cut.pathInfo()).isNull();

            String newContextPath = "/contextPath/";

            cut.contextPath(newContextPath);
            assertThat(cut.path()).isEqualTo("/contextPath/pathInfo");
            assertThat(cut.contextPath()).isEqualTo(newContextPath);
            assertThat(cut.pathInfo()).isEqualTo("/pathInfo");
        }

        @Test
        void should_set_path_info_to_path() {
            String initialPath = "/unknownContextPath/pathInfo";

            cut.path = initialPath;
            assertThat(cut.path()).isEqualTo(initialPath);
            assertThat(cut.contextPath()).isNull();
            assertThat(cut.pathInfo()).isNull();

            cut.contextPath("/");

            assertThat(cut.path()).isEqualTo(initialPath);
            assertThat(cut.contextPath()).isEqualTo("/");
            assertThat(cut.pathInfo()).isEqualTo(initialPath);
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
