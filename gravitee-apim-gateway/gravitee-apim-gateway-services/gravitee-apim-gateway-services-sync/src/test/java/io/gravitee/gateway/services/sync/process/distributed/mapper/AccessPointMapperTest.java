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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.accesspoint.model.AccessPoint;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class AccessPointMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private AccessPointMapper cut;
    private DistributedEvent distributedEvent;
    private AccessPointDeployable accessPointDeployable;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new AccessPointMapper(objectMapper);

        ReactableAccessPoint reactableAccessPoint = ReactableAccessPoint.builder()
            .id("id")
            .environmentId("environmentId")
            .host("host")
            .build();

        distributedEvent = DistributedEvent.builder()
            .id(reactableAccessPoint.getId())
            .payload(objectMapper.writeValueAsString(reactableAccessPoint))
            .updatedAt(new Date())
            .type(DistributedEventType.ACCESS_POINT)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        accessPointDeployable = AccessPointDeployable.builder()
            .reactableAccessPoint(reactableAccessPoint)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }

    @Test
    void should_return_distributed_event() {
        cut
            .to(distributedEvent)
            .test()
            .assertValue(deployable -> {
                assertThat(deployable).isEqualTo(accessPointDeployable);
                return true;
            });
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertComplete();
    }

    @Test
    void should_map_accesspoint_deployable() {
        cut
            .to(accessPointDeployable)
            .test()
            .assertValue(distributedEvent -> {
                assertThat(distributedEvent.getId()).isEqualTo(this.distributedEvent.getId());
                assertThat(distributedEvent.getPayload()).isEqualTo(this.distributedEvent.getPayload());
                assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.ACCESS_POINT);
                assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                return true;
            });
    }
}
