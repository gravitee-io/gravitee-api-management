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
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private ApiProductMapper cut;
    private DistributedEvent distributedEvent;
    private ApiProductReactorDeployable apiProductDeployable;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new ApiProductMapper(objectMapper);

        Plan plan = Plan.builder().id("plan-1").build();
        ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
            .id("product-id")
            .name("Test Product")
            .apiIds(Set.of("api-1", "api-2"))
            .plans(List.of(plan))
            .build();

        distributedEvent = DistributedEvent.builder()
            .id(reactableApiProduct.getId())
            .payload(objectMapper.writeValueAsString(reactableApiProduct))
            .updatedAt(new Date())
            .type(DistributedEventType.API_PRODUCT)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        apiProductDeployable = ApiProductReactorDeployable.builder()
            .apiProductId(reactableApiProduct.getId())
            .reactableApiProduct(reactableApiProduct)
            .syncAction(SyncAction.DEPLOY)
            .subscribablePlans(Set.of("plan-1"))
            .build();
    }

    @Test
    void should_return_deployable_from_distributed_event() {
        cut
            .to(distributedEvent)
            .test()
            .assertValue(deployable -> {
                assertThat(deployable.apiProductId()).isEqualTo(apiProductDeployable.apiProductId());
                assertThat(deployable.syncAction()).isEqualTo(apiProductDeployable.syncAction());
                assertThat(deployable.reactableApiProduct()).isEqualTo(apiProductDeployable.reactableApiProduct());
                assertThat(deployable.subscribablePlans()).isEqualTo(apiProductDeployable.subscribablePlans());
                return true;
            });
    }

    @Test
    void should_return_deployable_for_undeploy_event() {
        DistributedEvent undeployEvent = DistributedEvent.builder()
            .id("product-id")
            .type(DistributedEventType.API_PRODUCT)
            .syncAction(DistributedSyncAction.UNDEPLOY)
            .updatedAt(new Date())
            .build();

        cut
            .to(undeployEvent)
            .test()
            .assertValue(deployable -> {
                assertThat(deployable.apiProductId()).isEqualTo("product-id");
                assertThat(deployable.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
                assertThat(deployable.reactableApiProduct()).isNull();
                return true;
            });
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertComplete();
    }

    @Test
    void should_return_empty_for_deploy_event_with_missing_payload() {
        DistributedEvent deployEvent = DistributedEvent.builder()
            .id("product-id")
            .type(DistributedEventType.API_PRODUCT)
            .syncAction(DistributedSyncAction.DEPLOY)
            .updatedAt(new Date())
            .build();

        cut.to(deployEvent).test().assertComplete();
    }

    @Test
    void should_map_deployable_to_distributed_event() {
        cut
            .to(apiProductDeployable)
            .test()
            .assertValue(event -> {
                assertThat(event.getId()).isEqualTo(distributedEvent.getId());
                assertThat(event.getType()).isEqualTo(distributedEvent.getType());
                assertThat(event.getPayload()).isEqualTo(distributedEvent.getPayload());
                assertThat(event.getSyncAction()).isEqualTo(distributedEvent.getSyncAction());
                return true;
            });
    }

    @Test
    void should_map_undeploy_deployable_without_payload() {
        ApiProductReactorDeployable undeployDeployable = ApiProductReactorDeployable.builder()
            .apiProductId("product-id")
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        cut
            .to(undeployDeployable)
            .test()
            .assertValue(event -> {
                assertThat(event.getId()).isEqualTo("product-id");
                assertThat(event.getType()).isEqualTo(DistributedEventType.API_PRODUCT);
                assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
                assertThat(event.getPayload()).isNull();
                return true;
            });
    }
}
