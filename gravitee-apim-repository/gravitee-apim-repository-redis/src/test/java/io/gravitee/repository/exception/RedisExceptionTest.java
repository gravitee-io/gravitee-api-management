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
package io.gravitee.repository.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisExceptionTest {

    @Test
    void redis_operation_timeout_exception_does_not_capture_stack_trace() {
        var ex = new RedisOperationTimeoutException(10);

        assertThat(ex.getMessage()).isEqualTo("Operation on Redis took more than 10ms");
        assertThat(ex.getStackTrace()).isEmpty();
    }

    @Test
    void redis_not_connected_exception_does_not_capture_stack_trace() {
        var ex = new RedisNotConnectedException();

        assertThat(ex.getMessage()).isEqualTo("Connection to Redis not available");
        assertThat(ex.getStackTrace()).isEmpty();
    }
}
