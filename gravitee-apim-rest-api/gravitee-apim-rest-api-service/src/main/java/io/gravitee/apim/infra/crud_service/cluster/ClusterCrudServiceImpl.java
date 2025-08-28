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
import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.apim.infra.adapter.ClusterAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import java.util.function.Predicate;
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
            var result = clusterRepository.create(clusterAdapter.toRepository(clusterToCreate));
            return clusterAdapter.fromRepository(result);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToCreateWithId(Cluster.class, clusterToCreate.getId(), e);
        }
    }

    @Override
    public Cluster findByIdAndEnvironmentId(String id, String environmentId) {
        try {
            return clusterRepository
                .findById(id)
                .filter(belongsToEnvironment(environmentId))
                .map(clusterAdapter::fromRepository)
                .orElseThrow(() -> new DbEntityNotFoundException(io.gravitee.repository.management.model.Cluster.class, id));
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToFindById(Cluster.class, id, e);
        }
    }

    private static Predicate<io.gravitee.repository.management.model.Cluster> belongsToEnvironment(String environmentId) {
        return cluster -> environmentId.equals(cluster.getEnvironmentId());
    }

    @Override
    public Cluster update(Cluster clusterToUpdate) {
        try {
            var result = clusterRepository.update(clusterAdapter.toRepository(clusterToUpdate));
            return clusterAdapter.fromRepository(result);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToUpdateWithId(Cluster.class, clusterToUpdate.getId(), e);
        }
    }

    @Override
    public void delete(String clusterId, String environmentId) {
        try {
            // So we throw an error if the cluster is not in the correct environment
            findByIdAndEnvironmentId(clusterId, environmentId);
            clusterRepository.delete(clusterId);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToDeleteWithId(Cluster.class, clusterId, e);
        }
    }

    @Override
    public void updateGroups(String id, String environmentId, Set<String> groups) {
        findByIdAndEnvironmentId(id, environmentId);

        clusterRepository.updateGroups(id, groups);
    }
}
