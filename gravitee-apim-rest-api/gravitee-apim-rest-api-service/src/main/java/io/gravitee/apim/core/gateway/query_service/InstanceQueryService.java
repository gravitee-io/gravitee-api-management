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
package io.gravitee.apim.core.gateway.query_service;

import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;

public interface InstanceQueryService {
    List<Instance> findAllStarted(String organizationId, String environmentId);
    BaseInstance findById(ExecutionContext executionContext, String instanceId);
    List<BaseInstance> findByIds(ExecutionContext executionContext, Collection<String> instanceIds);
}
