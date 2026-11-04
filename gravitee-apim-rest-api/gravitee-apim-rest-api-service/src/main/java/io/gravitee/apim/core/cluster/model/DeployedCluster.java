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
package io.gravitee.apim.core.cluster.model;

import io.gravitee.definition.model.cluster.ClusterType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployedCluster {

    private String crossId;
    private String name;
    private String description;
    private ClusterType type;
    private Instant deployedAt;
    private Integer version;
    private List<DeployedClusterConnection> connections;
    /**
     * Whether the caller may bind an API to this cluster: a member of it with {@code CLUSTER_DEFINITION[READ]}, or an org
     * admin. The cluster pickers of the Console and of Gamma ESM only offer bindable clusters; an API keeps its binding to
     * another one, limited to the connections it already uses.
     */
    private boolean bindable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeployedClusterConnection {

        private String crossId;
        private String name;
        /** Why a Message API cannot use this connection; {@code null} when it can. Never carries credentials. */
        private String messageApiUnsupportedReason;
    }
}
