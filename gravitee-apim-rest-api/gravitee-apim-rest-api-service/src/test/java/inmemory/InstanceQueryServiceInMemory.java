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

import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;

public class InstanceQueryServiceInMemory implements InstanceQueryService, InMemoryAlternative<Instance> {

    private final List<Instance> storage = new ArrayList<>();

    @Override
    public List<Instance> findAllStarted(String organizationId, String environmentId) {
        return storage
            .stream()
            .filter(instance -> instance.getStartedAt() != null)
            .toList();
    }

    @Override
    public BaseInstance findById(ExecutionContext executionContext, String instanceId) {
        return storage
            .stream()
            .filter(
                instance -> instanceId.equals(instance.getId()) && instance.getEnvironments().contains(executionContext.getEnvironmentId())
            )
            .findFirst()
            .map(instance -> BaseInstance.builder().ip(instance.getIp()).id(instance.getId()).hostname(instance.getHostname()).build())
            .orElseThrow(NotFoundException::new);
    }

    @Override
    public void initWith(List<Instance> items) {
        reset();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Instance> storage() {
        return storage;
    }
}
