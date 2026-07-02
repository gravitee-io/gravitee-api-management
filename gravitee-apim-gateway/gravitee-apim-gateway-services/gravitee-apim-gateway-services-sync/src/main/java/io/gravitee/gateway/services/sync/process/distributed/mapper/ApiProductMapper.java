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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class ApiProductMapper {

    private final ObjectMapper objectMapper;

    public Maybe<ApiProductReactorDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                SyncAction syncAction = SyncActionMapper.to(event.getSyncAction());

                if (syncAction == SyncAction.DEPLOY && (event.getPayload() == null || event.getPayload().isBlank())) {
                    return null;
                }

                var builder = ApiProductReactorDeployable.builder().apiProductId(event.getId()).syncAction(syncAction);

                if (syncAction == SyncAction.DEPLOY) {
                    ReactableApiProduct reactableApiProduct = objectMapper.readValue(event.getPayload(), ReactableApiProduct.class);
                    builder.reactableApiProduct(reactableApiProduct);

                    List<Plan> plans = reactableApiProduct.getPlans();
                    if (plans != null && !plans.isEmpty()) {
                        builder.subscribablePlans(plans.stream().map(Plan::getId).collect(Collectors.toSet()));
                    }
                }

                return builder.build();
            } catch (Exception e) {
                log.warn("Error while determining api product into event payload", e);
                return null;
            }
        });
    }

    public Maybe<DistributedEvent> to(final ApiProductReactorDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.apiProductId())
                    .type(DistributedEventType.API_PRODUCT)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(objectMapper.writeValueAsString(deployable.reactableApiProduct()));
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from api product reactor", e);
                return null;
            }
        });
    }
}
