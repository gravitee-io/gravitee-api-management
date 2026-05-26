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
package io.gravitee.repository.noop.otel.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.otel.log.model.OtelLogRecord;
import io.gravitee.repository.otel.log.model.OtelLogSearchCriteria;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class NoOpOtelLogRepositoryTest {

    private final NoOpOtelLogRepository repository = new NoOpOtelLogRepository();

    @Test
    public void findLogs_should_return_empty_list() {
        List<OtelLogRecord> records = repository
            .findLogs(
                new QueryContext("org#1", "env#1"),
                new OtelLogSearchCriteria("a1b2c3d4", Map.of(), Map.of("gravitee.api.id", "api-1"), null, null, null)
            )
            .blockingGet();

        assertThat(records).isEmpty();
    }
}
