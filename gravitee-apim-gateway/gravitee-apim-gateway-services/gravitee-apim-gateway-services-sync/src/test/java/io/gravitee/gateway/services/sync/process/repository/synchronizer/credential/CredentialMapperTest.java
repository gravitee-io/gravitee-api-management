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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.credential;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import java.util.Date;
import org.junit.jupiter.api.Test;

class CredentialMapperTest {

    private final CredentialMapper mapper = new CredentialMapper(new ObjectMapper());

    @Test
    void should_map_a_publish_event_to_a_deployable() {
        Event event = event("evt-1", "{\"id\": \"credential-1\", \"environmentId\": \"env-1\", \"encryptedSecret\": \"ciphertext\"}");
        event.setUpdatedAt(new Date(1234L));

        CredentialDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.id()).isEqualTo("credential-1");
        assertThat(d.environmentId()).isEqualTo("env-1");
        assertThat(d.encryptedSecret()).isEqualTo("ciphertext");
        assertThat(d.updatedAt()).isEqualTo(1234L);
        assertThat(d.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void should_default_updatedAt_to_zero_when_the_event_has_none() {
        Event event = event("evt-2", "{\"id\": \"credential-1\", \"environmentId\": \"env-1\", \"encryptedSecret\": \"ciphertext\"}");

        assertThat(mapper.toDeploy(event).blockingGet().updatedAt()).isZero();
    }

    @Test
    void should_skip_a_publish_event_without_an_id() {
        Event event = event("evt-3", "{\"environmentId\": \"env-1\", \"encryptedSecret\": \"ciphertext\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void should_skip_a_publish_event_without_an_environment() {
        Event event = event("evt-4", "{\"id\": \"credential-1\", \"encryptedSecret\": \"ciphertext\"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void should_skip_a_publish_event_without_a_secret() {
        Event event = event("evt-5", "{\"id\": \"credential-1\", \"environmentId\": \"env-1\", \"encryptedSecret\": \" \"}");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void should_skip_a_publish_event_with_an_unreadable_payload() {
        Event event = event("evt-6", "not json");

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void should_map_an_unpublish_event_without_a_secret() {
        Event event = event("evt-7", "{\"id\": \"credential-1\", \"environmentId\": \"env-1\"}");

        CredentialDeployable d = mapper.toUndeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.id()).isEqualTo("credential-1");
        assertThat(d.environmentId()).isEqualTo("env-1");
        assertThat(d.encryptedSecret()).isNull();
        assertThat(d.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void should_skip_an_unpublish_event_without_an_id() {
        Event event = event("evt-8", "{\"environmentId\": \"env-1\"}");

        assertThat(mapper.toUndeploy(event).blockingGet()).isNull();
    }

    @Test
    void should_keep_the_encrypted_secret_out_of_toString() {
        Event event = event("evt-9", "{\"id\": \"credential-1\", \"environmentId\": \"env-1\", \"encryptedSecret\": \"ciphertext\"}");

        assertThat(mapper.toDeploy(event).blockingGet().toString()).contains("credential-1").doesNotContain("ciphertext");
    }

    private static Event event(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setPayload(payload);
        return event;
    }
}
