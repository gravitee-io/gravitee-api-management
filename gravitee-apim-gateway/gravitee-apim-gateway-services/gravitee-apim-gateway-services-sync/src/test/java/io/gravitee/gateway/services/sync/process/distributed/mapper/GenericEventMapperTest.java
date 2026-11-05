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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent.GenericEventDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GenericEventMapperTest {

    private final GenericEventMapper cut = new GenericEventMapper(new ObjectMapper());

    @Test
    void should_carry_a_deployed_event_through_a_distributed_event() throws Exception {
        var genericEvent = new GenericEvent(
            "aihub.key",
            "app-1",
            "env-1",
            "org-1",
            Instant.parse("2030-01-01T00:00:00Z"),
            Instant.parse("2026-11-03T10:15:30Z"),
            "{\"teamId\":\"t1\",\"groupIds\":[\"g1\"]}"
        );
        var deployable = new GenericEventDeployable("event-1", SyncAction.DEPLOY, genericEvent);

        var event = cut.to(deployable).blockingGet();

        assertThat(event.getId()).isEqualTo("event-1");
        assertThat(event.getType()).isEqualTo(DistributedEventType.GENERIC_EVENT);
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
        var replayed = cut.to(event).blockingGet();
        assertThat(replayed.id()).isEqualTo("event-1");
        assertThat(replayed.syncAction()).isEqualTo(SyncAction.DEPLOY);
        assertThat(replayed.genericEvent()).usingRecursiveComparison().ignoringFields("payload").isEqualTo(genericEvent);
        assertThat(new ObjectMapper().readTree(replayed.genericEvent().payload())).isEqualTo(
            new ObjectMapper().readTree(genericEvent.payload())
        );
    }

    @Test
    void should_keep_what_an_undeploy_needs_to_find_the_event() {
        var genericEvent = new GenericEvent("aihub.key", "app-1", "env-1", "org-1", null, null, null);

        var event = cut.to(new GenericEventDeployable("event-1", SyncAction.UNDEPLOY, genericEvent)).blockingGet();
        var replayed = cut.to(event).blockingGet();

        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        assertThat(replayed.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
        assertThat(replayed.genericEvent().environmentId()).isEqualTo("env-1");
        assertThat(replayed.genericEvent().organizationId()).isEqualTo("org-1");
        assertThat(replayed.genericEvent().kind()).isEqualTo("aihub.key");
        assertThat(replayed.genericEvent().id()).isEqualTo("app-1");
    }

    @Test
    void should_skip_an_event_without_kind() {
        var event = DistributedEvent.builder()
            .id("event-1")
            .type(DistributedEventType.GENERIC_EVENT)
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload("{\"id\":\"app-1\",\"environmentId\":\"env-1\",\"payload\":{}}")
            .build();

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_skip_a_deploy_without_payload() {
        var event = DistributedEvent.builder()
            .id("event-1")
            .type(DistributedEventType.GENERIC_EVENT)
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload("{\"kind\":\"aihub.key\",\"id\":\"app-1\",\"environmentId\":\"env-1\"}")
            .build();

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_skip_an_unreadable_payload() {
        var event = DistributedEvent.builder()
            .id("event-1")
            .type(DistributedEventType.GENERIC_EVENT)
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload("not json")
            .build();

        cut.to(event).test().assertComplete().assertNoValues();
    }
}
