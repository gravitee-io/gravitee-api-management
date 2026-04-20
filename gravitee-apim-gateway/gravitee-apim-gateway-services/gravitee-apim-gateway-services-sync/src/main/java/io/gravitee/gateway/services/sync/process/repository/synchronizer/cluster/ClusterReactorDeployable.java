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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.cluster;

import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.gateway.services.sync.process.common.model.ClusterDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Builder
@Getter
@Setter
@Accessors(fluent = true)
public class ClusterReactorDeployable implements ClusterDeployable {

    private String clusterId;
    private ReactableCluster reactableCluster;
    private SyncAction syncAction;

    @Override
    public String id() {
        return clusterId;
    }

    @Override
    public String clusterId() {
        return clusterId != null ? clusterId : (reactableCluster != null ? reactableCluster.getId() : null);
    }
}
