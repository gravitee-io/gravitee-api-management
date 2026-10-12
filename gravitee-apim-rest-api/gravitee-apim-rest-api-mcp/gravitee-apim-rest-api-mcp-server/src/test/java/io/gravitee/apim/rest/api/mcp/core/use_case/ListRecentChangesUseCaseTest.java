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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.Change.Source;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListRecentChangesUseCaseTest {

    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");

    private final Fakes.Access access = new Fakes.Access();
    private final Fakes.InMemoryChangeLog changeLog = new Fakes.InMemoryChangeLog();
    private final ListRecentChangesUseCase useCase = new ListRecentChangesUseCase(access, Fakes.apis(ORDERS), changeLog);

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_merge_deployments_and_audit_entries_newest_first() {
        changeLog.deployments.add(change("2026-01-31T13:10:00Z", Source.DEPLOYMENT, "PUBLISH_API"));
        changeLog.audit.add(change("2026-01-31T13:05:00Z", Source.AUDIT, "PLAN_UPDATED"));
        changeLog.audit.add(change("2026-01-31T13:50:00Z", Source.AUDIT, "APIKEY_REVOKED"));

        var changes = execute("Orders");

        assertThat(changes.changes()).extracting(Change::event).containsExactly("APIKEY_REVOKED", "PUBLISH_API", "PLAN_UPDATED");
        assertThat(changes.omittedSources()).isEmpty();
        assertThat(changes.api()).isEqualTo(ORDERS);
    }

    @Test
    void should_look_one_hour_back_by_default() {
        assertThat(Duration.between(execute("Orders").window().from(), NOW)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void should_name_a_source_the_caller_cannot_read_instead_of_reporting_no_change() {
        access.audit = false;
        changeLog.audit.add(change("2026-01-31T13:50:00Z", Source.AUDIT, "APIKEY_REVOKED"));

        var changes = execute("Orders");

        assertThat(changes.changes()).isEmpty();
        assertThat(changes.omittedSources()).containsExactly(Source.AUDIT);
        assertThat(changes.summary()).contains("Not allowed to read: [AUDIT]");
    }

    @Test
    void should_require_an_api() {
        assertThatThrownBy(() -> execute(null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("api is required");
    }

    private ListRecentChangesUseCase.Output execute(String api) {
        return useCase.execute(new ListRecentChangesUseCase.Input(CALLER, api, null, null));
    }

    private static Change change(String at, Source source, String event) {
        return new Change(Instant.parse(at), source, event, "admin", Map.of());
    }
}
