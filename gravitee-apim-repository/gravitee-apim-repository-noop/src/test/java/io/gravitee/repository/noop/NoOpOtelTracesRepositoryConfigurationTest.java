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
package io.gravitee.repository.noop;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.noop.tracing.NoOpTracingRepository;
import io.gravitee.repository.tracing.api.TracingRepository;
import org.junit.jupiter.api.Test;

public class NoOpOtelTracesRepositoryConfigurationTest {

    @Test
    public void tracingRepository_should_expose_no_op_instance() {
        // Verifies the @Bean method wires the no-op impl so consumers can resolve the TracingRepository
        // bean even when no OTel traces backend is configured (repositories.otel-traces.type=none path).
        TracingRepository repository = new NoOpOtelTracesRepositoryConfiguration().tracingRepository();

        assertThat(repository).isInstanceOf(NoOpTracingRepository.class);
    }
}
