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
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import org.junit.jupiter.api.Test;

class AuthzEntityMapperTest {

    private final AuthzEntityMapper mapper = new AuthzEntityMapper(new ObjectMapper());

    @Test
    void to_typed_entityType_payload_produces_deployable_with_typed_engine_uid() {
        DistributedEvent event = event(
            """
            {
              "entityId": "alice",
              "kind": "PRINCIPAL",
              "entityType": "User",
              "attributes": {"email": "alice@example.com"},
              "parents": []
            }
            """
        );

        AuthzEntityReactorDeployable deployable = mapper.to(event).blockingGet();

        assertThat(deployable).isNotNull();
        assertThat(deployable.entityId()).isEqualTo("alice");
        assertThat(deployable.entityType()).isEqualTo("User");
        assertThat(deployable.engineUid()).isEqualTo("User::\"alice\"");
        assertThat(deployable.kind()).isEqualTo(AuthzEntityReactorDeployable.Kind.PRINCIPAL);
        assertThat(deployable.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void to_legacy_payload_without_entityType_falls_back_to_kind_default() {
        DistributedEvent event = event(
            """
            {
              "entityId": "custom.bookings",
              "kind": "RESOURCE",
              "attributes": {},
              "parents": []
            }
            """
        );

        AuthzEntityReactorDeployable deployable = mapper.to(event).blockingGet();

        assertThat(deployable).isNotNull();
        assertThat(deployable.entityType()).isNull();
        assertThat(deployable.engineUid()).isEqualTo("Resource::\"custom.bookings\"");
        assertThat(deployable.kind()).isEqualTo(AuthzEntityReactorDeployable.Kind.RESOURCE);
    }

    @Test
    void to_roundtrip_preserves_entityType_through_distributed_event() throws Exception {
        AuthzEntityReactorDeployable source = AuthzEntityReactorDeployable
            .builder()
            .entityId("doc-1")
            .kind(AuthzEntityReactorDeployable.Kind.RESOURCE)
            .entityType("Doc")
            .engineUid("Doc::\"doc-1\"")
            .syncAction(SyncAction.DEPLOY)
            .build();

        DistributedEvent emitted = mapper.to(source).blockingFirst();
        AuthzEntityReactorDeployable decoded = mapper.to(emitted).blockingGet();

        assertThat(decoded).isNotNull();
        assertThat(decoded.entityType()).isEqualTo("Doc");
        assertThat(decoded.engineUid()).isEqualTo("Doc::\"doc-1\"");
    }

    @Test
    void to_missing_entityId_returns_empty() {
        DistributedEvent event = event("{\"kind\":\"RESOURCE\"}");
        assertThat(mapper.to(event).blockingGet()).isNull();
    }

    @Test
    void to_missing_kind_returns_empty() {
        DistributedEvent event = event("{\"entityId\":\"alice\"}");
        assertThat(mapper.to(event).blockingGet()).isNull();
    }

    @Test
    void to_unparseable_payload_returns_empty() {
        DistributedEvent event = event("not-json");
        assertThat(mapper.to(event).blockingGet()).isNull();
    }

    private static DistributedEvent event(String payload) {
        return DistributedEvent.builder()
            .id("evt")
            .type(DistributedEventType.AUTHZ_ENTITY)
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload(payload)
            .updatedAt(new Date())
            .build();
    }
}
