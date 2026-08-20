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
package io.gravitee.definition.model.v4.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class HttpClientOptionsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_default_max_wait_queue_size_to_unbounded_when_absent() throws Exception {
        final HttpClientOptions options = mapper.readValue("{}", HttpClientOptions.class);

        // Absent must resolve to -1 (unbounded), NOT the Java primitive default of 0 which would mean "no wait queue".
        assertThat(options.getMaxWaitQueueSize()).isEqualTo(-1);
    }

    @Test
    void should_treat_explicit_null_max_wait_queue_size_as_unbounded() throws Exception {
        final HttpClientOptions options = mapper.readValue("{\"maxWaitQueueSize\":null}", HttpClientOptions.class);

        // Explicit null must be handled the same as absent (-1), not coerced to 0.
        assertThat(options.getMaxWaitQueueSize()).isEqualTo(-1);
    }

    @Test
    void should_keep_explicit_max_wait_queue_size() throws Exception {
        final HttpClientOptions options = mapper.readValue("{\"maxWaitQueueSize\":50}", HttpClientOptions.class);

        assertThat(options.getMaxWaitQueueSize()).isEqualTo(50);
    }
}
