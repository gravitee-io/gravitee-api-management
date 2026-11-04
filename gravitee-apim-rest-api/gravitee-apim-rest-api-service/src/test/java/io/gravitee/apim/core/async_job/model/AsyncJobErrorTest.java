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
package io.gravitee.apim.core.async_job.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AsyncJobErrorTest {

    @Test
    void should_cut_a_long_message() {
        var error = new AsyncJobError("provider_error", "x".repeat(AsyncJobError.MAX_MESSAGE_LENGTH + 10), "agent-1");

        assertThat(error.message()).hasSize(AsyncJobError.MAX_MESSAGE_LENGTH);
    }

    @Test
    void should_keep_a_short_or_null_message() {
        assertThat(new AsyncJobError("provider_error", "Timeout", null).message()).isEqualTo("Timeout");
        assertThat(new AsyncJobError("provider_error", null, null).message()).isNull();
    }
}
