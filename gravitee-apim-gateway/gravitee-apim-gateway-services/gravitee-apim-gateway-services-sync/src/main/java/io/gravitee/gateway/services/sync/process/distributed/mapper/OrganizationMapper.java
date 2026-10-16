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
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class OrganizationMapper {

    private final ObjectMapper objectMapper;
    private final DistributedSyncMetrics metrics;

    public OrganizationMapper(final ObjectMapper objectMapper) {
        this(objectMapper, new DistributedSyncMetrics());
    }

    public Maybe<OrganizationDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                final ReactableOrganization reactableOrganization = objectMapper.readValue(event.getPayload(), ReactableOrganization.class);
                return OrganizationDeployable.builder().reactableOrganization(reactableOrganization).build();
            } catch (Exception e) {
                return MappingFailures.decodeFailure(metrics, event, e);
            }
        });
    }

    public Maybe<DistributedEvent> to(final OrganizationDeployable organizationDeployable) {
        return Maybe.fromCallable(() -> {
            try {
                return DistributedEvent.builder()
                    .id(organizationDeployable.id())
                    .type(DistributedEventType.ORGANIZATION)
                    .syncAction(DistributedSyncAction.DEPLOY)
                    .payload(objectMapper.writeValueAsString(organizationDeployable.reactableOrganization()))
                    .updatedAt(new Date())
                    .build();
            } catch (Exception e) {
                return MappingFailures.deadLetter(metrics, DistributedEventType.ORGANIZATION, organizationDeployable.id(), e);
            }
        });
    }
}
