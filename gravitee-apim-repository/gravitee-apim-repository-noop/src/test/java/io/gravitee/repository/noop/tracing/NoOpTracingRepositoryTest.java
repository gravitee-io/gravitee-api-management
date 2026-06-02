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
package io.gravitee.repository.noop.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.tracing.model.Trace;
import io.gravitee.repository.tracing.model.TraceSearchCriteria;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class NoOpTracingRepositoryTest {

    private final NoOpTracingRepository repository = new NoOpTracingRepository();

    @Test
    public void searchTraces_should_return_empty_list() {
        List<Trace> traces = repository
            .searchTraces(
                new QueryContext("org#1", "env#1"),
                new TraceSearchCriteria(Map.of(), 100, null, null, Map.of("gravitee.api.id", "api-1"))
            )
            .blockingGet();

        assertThat(traces).isEmpty();
    }

    @Test
    public void getTrace_should_return_empty_maybe() {
        Trace trace = repository.getTrace(new QueryContext("org#1", "env#1"), "a1b2c3d4", Map.of("gravitee.api.id", "api-1")).blockingGet();

        assertThat(trace).isNull();
    }
}
