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

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.service_provider.ClusterSecretResolver;
import io.gravitee.definition.model.cluster.SecurityConfig;
import java.util.ArrayList;
import java.util.List;

public class ClusterSecretResolverInMemory implements ClusterSecretResolver {

    private final List<String> revoked = new ArrayList<>();

    public List<String> revoked() {
        return List.copyOf(revoked);
    }

    public void reset() {
        revoked.clear();
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public SecurityConfig resolve(Cluster cluster, String connectionCrossId) {
        throw new UnsupportedOperationException("No secrets service in memory");
    }

    @Override
    public void revoke(String clusterId) {
        revoked.add(clusterId);
    }
}
