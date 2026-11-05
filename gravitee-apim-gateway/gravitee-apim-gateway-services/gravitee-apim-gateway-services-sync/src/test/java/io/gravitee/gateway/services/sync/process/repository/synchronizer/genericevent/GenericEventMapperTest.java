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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GenericEventMapperTest {

    private final GenericEventMapper mapper = new GenericEventMapper(new ObjectMapper());

    private static Event event(String payload) {
        var event = new Event();
        event.setId("event-1");
        event.setPayload(payload);
        return event;
    }

    @Test
    void should_read_the_envelope_written_by_the_control_plane() {
        var payload = """
            {"kind":"aihub.key","id":"app-1","environmentId":"env","organizationId":"org",\
            "updatedAt":"2026-10-23T10:00:00Z","payload":{"teamId":"t1","groupIds":["g1"]}}""";

        var deployable = mapper.to(event(payload), SyncAction.DEPLOY).blockingGet();

        assertThat(deployable.syncAction()).isEqualTo(SyncAction.DEPLOY);
        var genericEvent = deployable.genericEvent();
        assertThat(genericEvent.kind()).isEqualTo("aihub.key");
        assertThat(genericEvent.id()).isEqualTo("app-1");
        assertThat(genericEvent.environmentId()).isEqualTo("env");
        assertThat(genericEvent.organizationId()).isEqualTo("org");
        assertThat(genericEvent.updatedAt()).isEqualTo(Instant.parse("2026-10-23T10:00:00Z"));
        assertThat(genericEvent.payload()).isEqualTo("{\"teamId\":\"t1\",\"groupIds\":[\"g1\"]}");
    }

    @Test
    void should_read_the_expiry() {
        var payload = """
            {"kind":"runtime.tracing","id":"s1","environmentId":"env","expiresAt":"2026-10-23T11:00:00Z","payload":{}}""";

        var genericEvent = mapper.to(event(payload), SyncAction.DEPLOY).blockingGet().genericEvent();

        assertThat(genericEvent.expiresAt()).isEqualTo(Instant.parse("2026-10-23T11:00:00Z"));
    }

    @Test
    void should_undeploy_without_payload() {
        var payload = """
            {"kind":"aihub.key","id":"app-1","environmentId":"env"}""";

        assertThat(mapper.to(event(payload), SyncAction.UNDEPLOY).blockingGet().syncAction()).isEqualTo(SyncAction.UNDEPLOY);
        assertThat(mapper.to(event(payload), SyncAction.DEPLOY).blockingGet()).isNull();
    }

    @Test
    void should_skip_an_unreadable_payload() {
        assertThat(mapper.to(event("not json"), SyncAction.DEPLOY).blockingGet()).isNull();
    }
}
