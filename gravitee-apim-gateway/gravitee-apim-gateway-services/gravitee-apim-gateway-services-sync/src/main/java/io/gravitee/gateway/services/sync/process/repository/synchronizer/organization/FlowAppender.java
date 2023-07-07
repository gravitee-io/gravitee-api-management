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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.organization;

import io.gravitee.definition.model.Organization;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.env.GatewayConfiguration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FlowAppender {

    private final GatewayConfiguration gatewayConfiguration;

    /**
     * Fetching flows for given deployables
     * @param organizationDeployable the deployables to update
     * @return the deployables updated with flows
     */
    public OrganizationDeployable appends(final OrganizationDeployable organizationDeployable) {
        List<String> shardingTags = gatewayConfiguration.shardingTags().orElse(null);
        if (shardingTags != null && !shardingTags.isEmpty()) {
            filterFlows(organizationDeployable.organization());
        }
        return organizationDeployable;
    }

    private void filterFlows(final Organization organization) {
        List<Flow> filteredFlows = organization
            .getFlows()
            .stream()
            .filter(flow -> {
                List<Consumer> consumers = flow.getConsumers();
                if (consumers != null && !consumers.isEmpty()) {
                    Set<String> flowTags = consumers
                        .stream()
                        .filter((consumer -> consumer.getConsumerType().equals(ConsumerType.TAG)))
                        .map(Consumer::getConsumerId)
                        .collect(Collectors.toSet());
                    return gatewayConfiguration.hasMatchingTags(flowTags);
                }
                return true;
            })
            .collect(Collectors.toList());
        organization.setFlows(filteredFlows);
    }
}
