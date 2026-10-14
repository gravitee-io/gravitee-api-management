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
package io.gravitee.apim.core.cluster.domain_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * The clusters deployed in an environment, as their last deployment published them: what the gateways run, which may
 * differ from the cluster being edited until it is redeployed.
 */
@DomainService
@RequiredArgsConstructor
public class DeployedClustersDomainService {

    private final EventLatestQueryService eventLatestQueryService;
    private final ObjectMapper objectMapper;

    public List<Cluster> findAll(String environmentId) {
        return eventLatestQueryService
            .findAllByTypeAndEnvironments(Set.of(EventType.DEPLOY_CLUSTER), Set.of(environmentId), Event.EventProperties.CLUSTER_ID)
            .stream()
            .map(this::toCluster)
            .toList();
    }

    public Optional<Cluster> findByCrossId(String environmentId, String crossId) {
        return findAll(environmentId)
            .stream()
            .filter(cluster -> crossId.equals(cluster.getCrossId()))
            .findFirst();
    }

    private Cluster toCluster(Event event) {
        try {
            return objectMapper.readValue(event.getPayload(), Cluster.class);
        } catch (Exception e) {
            throw new TechnicalManagementException("Failed to read the deployed cluster of event [" + event.getId() + "]", e);
        }
    }
}
