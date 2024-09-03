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
package fixtures.repository;

import io.gravitee.repository.management.model.AsyncJob;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;

public class AsyncJobFixture {

    private AsyncJobFixture() {}

    public static final Supplier<AsyncJob.AsyncJobBuilder> BASE = () ->
        AsyncJob
            .builder()
            .id("job-id")
            .sourceId("integration-id")
            .environmentId("environment-id")
            .initiatorId("initiator-id")
            .status("PENDING")
            .upperLimit(10L)
            .createdAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));

    public static AsyncJob anAsyncJob() {
        return BASE.get().build();
    }
}
