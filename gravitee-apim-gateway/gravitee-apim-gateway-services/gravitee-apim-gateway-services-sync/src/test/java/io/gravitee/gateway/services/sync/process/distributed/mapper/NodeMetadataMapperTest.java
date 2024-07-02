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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class NodeMetadataMapperTest {

    public static final String INSTALLATION_ID = "installationId";
    public static final Set<String> ORGANIZATION_IDS = Set.of("orga1", "orga2");
    private final ObjectMapper objectMapper = new GraviteeMapper();
    private NodeMetadataMapper cut;
    private DistributedEvent distributedEvent;
    private NodeMetadataDeployable nodeMetadataDeployable;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new NodeMetadataMapper(objectMapper);

        NodeMetadataMapper.DistributedNodeMetadataDeployable metadataDeployable = new NodeMetadataMapper.DistributedNodeMetadataDeployable(
            ORGANIZATION_IDS,
            INSTALLATION_ID
        );

        distributedEvent =
            DistributedEvent
                .builder()
                .id(UUID.randomUUID().toString())
                .payload(objectMapper.writeValueAsString(metadataDeployable))
                .updatedAt(new Date())
                .type(DistributedEventType.NODE_METADATA)
                .syncAction(DistributedSyncAction.DEPLOY)
                .build();

        nodeMetadataDeployable = NodeMetadataDeployable.builder().organizationIds(ORGANIZATION_IDS).installationId(INSTALLATION_ID).build();
    }

    @Test
    void should_return_distributed_event() {
        cut
            .to(distributedEvent)
            .test()
            .assertValue(nodeMetadataDeployable -> {
                assertThat(nodeMetadataDeployable.organizationIds()).isEqualTo(this.nodeMetadataDeployable.organizationIds());
                assertThat(nodeMetadataDeployable.installationId()).isEqualTo(this.nodeMetadataDeployable.installationId());
                return true;
            });
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertComplete();
    }

    @Test
    void should_map_node_metadata_deployable() {
        cut
            .to(nodeMetadataDeployable)
            .test()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent.getType()).isEqualTo(this.distributedEvent.getType());
                assertThat(distributedEvent.getPayload()).isEqualTo(this.distributedEvent.getPayload());
                assertThat(distributedEvent.getSyncAction()).isEqualTo(this.distributedEvent.getSyncAction());
                return true;
            });
    }
}
