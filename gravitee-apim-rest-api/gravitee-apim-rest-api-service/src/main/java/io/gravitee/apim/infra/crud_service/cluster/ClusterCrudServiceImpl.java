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
package io.gravitee.apim.infra.crud_service.cluster;

import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.infra.adapter.ClusterAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ClusterCrudServiceImpl implements ClusterCrudService {

    private final ClusterRepository clusterRepository;
    private final ClusterAdapter clusterAdapter;

    public ClusterCrudServiceImpl(@Lazy ClusterRepository clusterRepository, ClusterAdapter clusterAdapter) {
        this.clusterRepository = clusterRepository;
        this.clusterAdapter = clusterAdapter;
    }

    @Override
    public Cluster create(Cluster clusterToCreate) {
        try {
            io.gravitee.repository.management.model.Cluster result = clusterRepository.create(clusterAdapter.toRepository(clusterToCreate));
            return clusterAdapter.fromRepository(result);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a Cluster with id: %s", clusterToCreate.getId()),
                e
            );
        }
    }
}
