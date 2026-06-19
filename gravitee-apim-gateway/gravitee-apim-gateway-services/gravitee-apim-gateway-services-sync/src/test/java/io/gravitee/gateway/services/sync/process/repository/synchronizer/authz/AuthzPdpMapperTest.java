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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import org.junit.jupiter.api.Test;

class AuthzPdpMapperTest {

    private final AuthzPdpMapper mapper = new AuthzPdpMapper(new ObjectMapper());

    @Test
    void publish_maps_to_DEPLOY_with_scope() {
        Event event = event("evt-1", EventType.PUBLISH_AUTHZ_PDP, "{\"targetPdpId\":\"scope-1\",\"environmentId\":\"env-1\"}");

        AuthzPdpProvisionDeployable deployable = mapper.toDeploy(event).blockingGet();

        assertThat(deployable.targetPdpId()).isEqualTo("scope-1");
        assertThat(deployable.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void unpublish_maps_to_UNDEPLOY() {
        Event event = event("evt-2", EventType.UNPUBLISH_AUTHZ_PDP, "{\"targetPdpId\":\"scope-2\",\"environmentId\":\"env-1\"}");

        AuthzPdpProvisionDeployable deployable = mapper.toUndeploy(event).blockingGet();

        assertThat(deployable.targetPdpId()).isEqualTo("scope-2");
        assertThat(deployable.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void blank_scope_is_dropped() {
        Event event = event("evt-3", EventType.PUBLISH_AUTHZ_PDP, "{\"targetPdpId\":\"\"}");

        assertThat(mapper.toDeploy(event).isEmpty().blockingGet()).isTrue();
    }

    @Test
    void missing_environment_is_dropped() {
        Event event = event("evt-5", EventType.PUBLISH_AUTHZ_PDP, "{\"targetPdpId\":\"scope-1\"}");

        assertThat(mapper.toDeploy(event).isEmpty().blockingGet()).isTrue();
    }

    @Test
    void blank_environment_is_dropped() {
        Event event = event("evt-6", EventType.PUBLISH_AUTHZ_PDP, "{\"targetPdpId\":\"scope-1\",\"environmentId\":\"  \"}");

        assertThat(mapper.toDeploy(event).isEmpty().blockingGet()).isTrue();
    }

    @Test
    void valid_scope_and_environment_maps() {
        Event event = event("evt-7", EventType.PUBLISH_AUTHZ_PDP, "{\"targetPdpId\":\"scope-1\",\"environmentId\":\"env-1\"}");

        AuthzPdpProvisionDeployable deployable = mapper.toDeploy(event).blockingGet();

        assertThat(deployable.targetPdpId()).isEqualTo("scope-1");
        assertThat(deployable.environmentId()).isEqualTo("env-1");
    }

    @Test
    void unparseable_payload_is_dropped() {
        Event event = event("evt-4", EventType.PUBLISH_AUTHZ_PDP, "not-json");

        assertThat(mapper.toDeploy(event).isEmpty().blockingGet()).isTrue();
    }

    private static Event event(String id, EventType type, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(type);
        event.setPayload(payload);
        return event;
    }
}
