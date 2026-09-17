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
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private final CredentialMapper cut = new CredentialMapper(objectMapper);

    @Test
    void should_map_a_deployed_credential_to_a_distributed_event_and_back() {
        CredentialDeployable deployable = deployed();

        DistributedEvent event = cut.to(deployable).blockingGet();

        assertThat(event.getId()).isEqualTo("credential-1");
        assertThat(event.getType()).isEqualTo(DistributedEventType.CREDENTIAL);
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
        assertThat(cut.to(event).blockingGet()).isEqualTo(deployable);
    }

    @Test
    void should_distribute_an_undeploy_without_payload() {
        DistributedEvent event = cut
            .to(CredentialDeployable.builder().credentialId("credential-1").environmentId("env-1").syncAction(SyncAction.UNDEPLOY).build())
            .blockingGet();

        assertThat(event.getPayload()).isNull();
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        assertThat(cut.to(event).blockingGet()).isEqualTo(
            CredentialDeployable.builder().credentialId("credential-1").syncAction(SyncAction.UNDEPLOY).build()
        );
    }

    @Test
    void should_allow_no_api_when_the_payload_lists_none() {
        DistributedEvent event = DistributedEvent.builder()
            .id("credential-1")
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload(
                "{\"id\": \"credential-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"encryptedSecret\": \"ciphertext\", \"updatedAt\": 1234}"
            )
            .build();

        assertThat(cut.to(event).blockingGet().allowedApiIds()).isEmpty();
    }

    @Test
    void should_skip_a_deployed_credential_without_organization() {
        DistributedEvent event = cut.to(deployed().organizationId(null)).blockingGet();

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut
            .to(DistributedEvent.builder().id("credential-1").syncAction(DistributedSyncAction.DEPLOY).payload("wrong").build())
            .test()
            .assertComplete()
            .assertNoValues();
    }

    private static CredentialDeployable deployed() {
        return CredentialDeployable.builder()
            .credentialId("credential-1")
            .environmentId("env-1")
            .organizationId("org-1")
            .allowedApiIds(Set.of("api-1", "api-2"))
            .encryptedSecret("ciphertext")
            .updatedAt(1234L)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }
}
