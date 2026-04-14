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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.model.Cluster;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class ClusterTypeUpgrader implements Upgrader {

    private static final String DEFAULT_CLUSTER_TYPE = "KAFKA_CLUSTER_CONNECTION";

    private final ClusterRepository clusterRepository;

    @Autowired
    public ClusterTypeUpgrader(@Lazy ClusterRepository clusterRepository) {
        this.clusterRepository = clusterRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            clusterRepository
                .findAll()
                .stream()
                .filter(cluster -> cluster.getType() == null)
                .forEach(cluster -> {
                    try {
                        log.info("Setting type '{}' on cluster '{}'", DEFAULT_CLUSTER_TYPE, cluster.getId());
                        cluster.setType(DEFAULT_CLUSTER_TYPE);
                        clusterRepository.update(cluster);
                    } catch (Exception e) {
                        log.error("Failed to update type for cluster '{}'", cluster.getId(), e);
                    }
                });
            return true;
        });
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.CLUSTER_TYPE_UPGRADER;
    }
}
