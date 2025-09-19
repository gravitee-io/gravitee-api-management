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
package io.gravitee.gateway.services.sync.process.repository.service;

import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class PlanService {

    private final Map<String, Set<String>> plansPerApi = new ConcurrentHashMap<>();

    public void register(final ApiReactorDeployable apiReactorDeployable) {
        if (apiReactorDeployable != null && apiReactorDeployable.apiId() != null) {
            plansPerApi.put(apiReactorDeployable.apiId(), apiReactorDeployable.subscribablePlans());
        }
    }

    public void unregister(final ApiReactorDeployable apiReactorDeployable) {
        if (apiReactorDeployable != null && apiReactorDeployable.apiId() != null) {
            plansPerApi.remove(apiReactorDeployable.apiId());
        }
    }

    public boolean isDeployed(final String apiId, final String planId) {
        return Optional.ofNullable(plansPerApi.get(apiId))
            .map(strings -> strings.contains(planId))
            .orElse(false);
    }
}
