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
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class OrganizationMapper {

    private final ObjectMapper objectMapper;

    public Maybe<OrganizationDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                final ReactableOrganization reactableOrganization = objectMapper.readValue(event.getPayload(), ReactableOrganization.class);
                return OrganizationDeployable.builder().reactableOrganization(reactableOrganization).build();
            } catch (Exception e) {
                log.warn("Error while determining deployed organization into event payload", e);
                return null;
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
                log.warn("Error while building distributed event from organization", e);
                return null;
            }
        });
    }
}
