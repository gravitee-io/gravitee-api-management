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
package io.gravitee.apim.infra.query_service.gateway;

import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.infra.adapter.InstanceAdapter;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.InstanceNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstanceQueryServiceLegacyWrapper implements InstanceQueryService {

    private final InstanceService instanceService;

    @Override
    public List<Instance> findAllStarted(String organizationId, String environmentId) {
        return InstanceAdapter.INSTANCE.fromEntities(instanceService.findAllStarted(new ExecutionContext(organizationId, environmentId)));
    }

    @Override
    public BaseInstance findById(ExecutionContext executionContext, String instanceId) {
        return InstanceAdapter.INSTANCE.toBaseInstance(instanceService.findById(executionContext, instanceId));
    }

    @Override
    public List<BaseInstance> findByIds(ExecutionContext executionContext, Collection<String> instanceIds) {
        return instanceIds
            .stream()
            .flatMap(id -> {
                try {
                    return Stream.of(findById(executionContext, id));
                } catch (InstanceNotFoundException e) {
                    return Stream.empty();
                }
            })
            .toList();
    }
}
