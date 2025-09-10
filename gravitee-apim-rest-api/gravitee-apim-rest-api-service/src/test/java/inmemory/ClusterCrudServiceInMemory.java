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
package inmemory;

import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public class ClusterCrudServiceInMemory extends AbstractCrudServiceInMemory<Cluster> implements ClusterCrudService {

    @Override
    public Cluster findByIdAndEnvironmentId(String id, String environmentId) {
        return storage
            .stream()
            .filter(cluster -> id.equals(cluster.getId()) && environmentId.equals(cluster.getEnvironmentId()))
            .findFirst()
            .orElseThrow(() -> new DbEntityNotFoundException(Cluster.class, id));
    }

    @Override
    public Cluster update(Cluster clusterToUpdate) {
        OptionalInt index = this.findIndex(this.storage, cluster -> cluster.getId().equals(clusterToUpdate.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), clusterToUpdate);
            return clusterToUpdate;
        }
        throw new IllegalStateException("Cluster not found.");
    }

    @Override
    public void delete(String id, String environmentId) {
        storage.removeIf(cluster -> id.equals(cluster.getId()) && environmentId.equals(cluster.getEnvironmentId()));
    }

    @Override
    public void updateGroups(String id, String environmentId, Set<String> groups) {
        OptionalInt index =
            this.findIndex(this.storage, cluster -> cluster.getId().equals(id) && cluster.getEnvironmentId().equals(environmentId));
        if (index.isPresent()) {
            Cluster cluster = storage.get(index.getAsInt());
            Cluster updatedCluster = Cluster
                .builder()
                .id(cluster.getId())
                .name(cluster.getName())
                .description(cluster.getDescription())
                .environmentId(cluster.getEnvironmentId())
                .groups(groups)
                .build();
            storage.set(index.getAsInt(), updatedCluster);
        } else {
            throw new SharedPolicyGroupNotFoundException(id);
        }
    }
}
