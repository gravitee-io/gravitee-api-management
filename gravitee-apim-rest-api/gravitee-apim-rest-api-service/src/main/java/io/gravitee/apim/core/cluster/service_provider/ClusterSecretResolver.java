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
package io.gravitee.apim.core.cluster.service_provider;

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.definition.model.cluster.ClusterSecurityResolutionException;
import io.gravitee.definition.model.cluster.SecurityConfig;

/**
 * Resolves the secret references of a registered Kafka cluster's connections with the secret providers of the management
 * API, as the gateway does with its own when it deploys the cluster: same discovery, definition kind and evaluation. For
 * management API features that connect to a cluster themselves, such as the Kafka Explorer.
 */
public interface ClusterSecretResolver {
    /** Whether the secrets service runs on this management API: without it, no secret reference can be resolved. */
    boolean available();

    /**
     * @return the security of the connection {@code connectionCrossId} of {@code cluster}, a Kafka cluster with
     *     connections, with its secret references resolved
     * @throws ClusterSecurityResolutionException naming the field that could not be resolved, never its value
     * @throws IllegalArgumentException when the cluster has no such connection
     */
    SecurityConfig resolve(Cluster cluster, String connectionCrossId);

    /** Forgets what was discovered for the cluster {@code clusterId}, once it is deleted. Never fails the caller. */
    void revoke(String clusterId);
}
