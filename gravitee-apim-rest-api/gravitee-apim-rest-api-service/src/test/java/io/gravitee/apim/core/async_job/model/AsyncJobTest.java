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

import fixtures.core.model.AsyncJobFixture;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncJobTest {

    private static final Instant INSTANT_NOW = Instant.parse("2026-10-09T10:15:30.00Z");

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_mark_job_as_partial() {
        var job = AsyncJobFixture.aPendingFederatedApiIngestionJob().partial();

        assertThat(job.getStatus()).isEqualTo(AsyncJob.Status.PARTIAL);
        assertThat(job.getUpdatedAt()).isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
    }

    @Test
    void should_consider_partial_as_final() {
        assertThat(AsyncJob.Status.PARTIAL.isFinal()).isTrue();
    }

    @Test
    void should_not_consider_a_partial_job_late_once_its_deadline_has_passed() {
        var job = AsyncJobFixture.aPendingFederatedApiIngestionJob()
            .toBuilder()
            .deadLine(INSTANT_NOW.minusSeconds(60).atZone(ZoneId.systemDefault()))
            .build()
            .partial();

        assertThat(job.isLate()).isFalse();
    }
}
